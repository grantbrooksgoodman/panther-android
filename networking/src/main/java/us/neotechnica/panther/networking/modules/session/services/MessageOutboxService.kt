//
//  MessageOutboxService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import org.json.JSONArray
import org.json.JSONObject
import us.neotechnica.panther.networking.modules.session.extensions.messageOutboxDidChange
import us.neotechnica.panther.networking.modules.session.models.OutboxEntry
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.models.send
import java.util.Date

/**
 * The service that queues messages for delivery and retries failed
 * sends.
 *
 * [MessageOutboxService] holds pending and failed message entries and
 * persists them to disk. It publishes a change whenever its contents
 * change.
 *
 * **Note:** this Phase 7 port queues text messages only; audio and
 * media payloads (and their payload files) arrive with the media
 * phases.
 */
object MessageOutboxService {
    // MARK: - Properties

    private val entries = LockIsolated(mapOf<String, OutboxEntry>())

    @Volatile
    private var didLoad = false

    // MARK: - Computed Properties

    /** The outbox entries, sorted by creation date. */
    val allEntries: List<OutboxEntry>
        get() {
            loadIfNeeded()
            return entries.wrappedValue.values.sortedBy { it.createdDate.time }
        }

    // MARK: - Query Methods

    /**
     * Returns the outbox entries for the conversation with the given
     * identifier key, sorted by creation date.
     */
    fun entries(conversationIDKey: String): List<OutboxEntry> = allEntries.filter { it.conversationIDKey == conversationIDKey }

    /** Returns the outbox entry with the given identifier, or `null`. */
    fun entry(id: String): OutboxEntry? {
        loadIfNeeded()
        return entries.wrappedValue[id]
    }

    // MARK: - Mutation Methods

    /** Adds the given entry to the outbox. */
    fun enqueue(entry: OutboxEntry) {
        loadIfNeeded()
        entries.withValue { it.value = it.value + (entry.id to entry) }
        persist()
        Logger.log("Enqueued outbox entry ${entry.id} for conversation ${entry.conversationIDKey}.")
        emit()
    }

    /**
     * Atomically claims the entry with the given identifier for retry,
     * transitioning it to `sending` and reserving [candidateRemoteID]
     * for its message if none is reserved.
     *
     * @return The claimed entry, or `null` if the entry is missing or
     *   already claimed.
     */
    fun claimForRetry(
        id: String,
        candidateRemoteID: String,
    ): OutboxEntry? {
        loadIfNeeded()
        val claimed =
            entries.withValue { reference ->
                val entry = reference.value[id] ?: return@withValue null
                if (entry.state == OutboxEntry.State.SENDING) return@withValue null

                val updated =
                    entry.copy(
                        reservedRemoteID = entry.reservedRemoteID ?: candidateRemoteID,
                        state = OutboxEntry.State.SENDING,
                        attemptCount = entry.attemptCount + 1,
                        lastAttemptDate = Date(),
                    )
                reference.value = reference.value + (id to updated)
                updated
            }

        if (claimed != null) {
            persist()
            Logger.log("Claimed outbox entry $id for retry (attempt ${claimed.attemptCount}).")
            emit()
        }
        return claimed
    }

    /** Marks the outbox entry with the given identifier as failed. */
    fun markFailed(id: String) {
        loadIfNeeded()
        val failed =
            entries.withValue { reference ->
                val entry = reference.value[id] ?: return@withValue null
                val updated = entry.copy(state = OutboxEntry.State.FAILED)
                reference.value = reference.value + (id to updated)
                updated
            } ?: return

        persist()
        Logger.log("Marked outbox entry $id as failed (attempt ${failed.attemptCount}).")
        emit()
    }

    /** Removes the outbox entry with the given identifier. */
    fun remove(id: String) {
        loadIfNeeded()
        val removed =
            entries.withValue { reference ->
                val entry = reference.value[id] ?: return@withValue null
                reference.value = reference.value - id
                entry
            } ?: return

        persist()
        Logger.log("Removed outbox entry ${removed.id}.")
        emit()
    }

    /** Removes every outbox entry. */
    fun removeAll() {
        loadIfNeeded()
        val removed =
            entries.withValue { reference ->
                val current = reference.value
                reference.value = emptyMap()
                current
            }

        if (removed.isEmpty()) return
        persist()
        Logger.log("Removed all outbox entries (${removed.size}).")
        emit()
    }

    // MARK: - Auxiliary

    private fun emit() {
        DependencyValues.current.sharedEvents.messageOutboxDidChange
            .send()
    }

    private fun loadIfNeeded() {
        if (didLoad) return
        synchronized(this) {
            if (didLoad) return
            didLoad = true

            val archive = Persistent.string(PersistentStorageKey.messageOutbox) ?: return
            val decoded = runCatching { decode(archive) }.getOrNull() ?: return

            // Reconcile: any entry still marked SENDING at launch means
            // the app died mid-attempt.
            val reconciled =
                decoded.associateBy { it.id }.mapValues { (_, entry) ->
                    if (entry.state == OutboxEntry.State.SENDING) {
                        Logger.log("Reconciled stale SENDING entry ${entry.id} to FAILED.")
                        entry.copy(state = OutboxEntry.State.FAILED)
                    } else {
                        entry
                    }
                }

            entries.wrappedValue = reconciled
            Logger.log("Loaded ${reconciled.size} outbox entries into memory.")
        }
    }

    private fun persist() {
        Persistent.setString(PersistentStorageKey.messageOutbox, encode(entries.wrappedValue.values.toList()))
    }

    // MARK: - Serialization

    private fun encode(entries: List<OutboxEntry>): String {
        val array = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put(KEY_ID, entry.id)
            obj.put(KEY_CONVERSATION_ID_KEY, entry.conversationIDKey)
            obj.put(KEY_FROM_ACCOUNT_ID, entry.fromAccountID)
            obj.put(KEY_RECIPIENT_USER_IDS, JSONArray(entry.recipientUserIDs))
            obj.put(KEY_TEXT, entry.text)
            obj.put(KEY_IS_PEN_PALS, entry.isPenPalsConversation)
            obj.put(KEY_CREATED_DATE, entry.createdDate.time)
            obj.put(KEY_ATTEMPT_COUNT, entry.attemptCount)
            obj.put(KEY_LAST_ATTEMPT_DATE, entry.lastAttemptDate?.time ?: JSONObject.NULL)
            obj.put(KEY_RESERVED_REMOTE_ID, entry.reservedRemoteID ?: JSONObject.NULL)
            obj.put(KEY_STATE, entry.state.rawValue)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decode(archive: String): List<OutboxEntry> {
        val array = JSONArray(archive)
        val result = mutableListOf<OutboxEntry>()
        for (index in 0 until array.length()) {
            val obj = array.getJSONObject(index)
            val recipientsArray = obj.getJSONArray(KEY_RECIPIENT_USER_IDS)
            val recipients = (0 until recipientsArray.length()).map { recipientsArray.getString(it) }
            val state = OutboxEntry.State.from(obj.getString(KEY_STATE)) ?: continue

            result.add(
                OutboxEntry(
                    id = obj.getString(KEY_ID),
                    conversationIDKey = obj.getString(KEY_CONVERSATION_ID_KEY),
                    fromAccountID = obj.getString(KEY_FROM_ACCOUNT_ID),
                    recipientUserIDs = recipients,
                    text = obj.getString(KEY_TEXT),
                    isPenPalsConversation = obj.getBoolean(KEY_IS_PEN_PALS),
                    createdDate = Date(obj.getLong(KEY_CREATED_DATE)),
                    attemptCount = obj.getInt(KEY_ATTEMPT_COUNT),
                    lastAttemptDate = if (obj.isNull(KEY_LAST_ATTEMPT_DATE)) null else Date(obj.getLong(KEY_LAST_ATTEMPT_DATE)),
                    reservedRemoteID = if (obj.isNull(KEY_RESERVED_REMOTE_ID)) null else obj.getString(KEY_RESERVED_REMOTE_ID),
                    state = state,
                ),
            )
        }
        return result
    }

    // MARK: - Companion

    private const val KEY_ID = "id"
    private const val KEY_CONVERSATION_ID_KEY = "conversationIDKey"
    private const val KEY_FROM_ACCOUNT_ID = "fromAccountID"
    private const val KEY_RECIPIENT_USER_IDS = "recipientUserIDs"
    private const val KEY_TEXT = "text"
    private const val KEY_IS_PEN_PALS = "isPenPalsConversation"
    private const val KEY_CREATED_DATE = "createdDate"
    private const val KEY_ATTEMPT_COUNT = "attemptCount"
    private const val KEY_LAST_ATTEMPT_DATE = "lastAttemptDate"
    private const val KEY_RESERVED_REMOTE_ID = "reservedRemoteID"
    private const val KEY_STATE = "state"
}
