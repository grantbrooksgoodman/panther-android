//
//  MessageOutboxService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.services

import us.neotechnica.panther.modules.session.entity.extensions.messageOutboxDidChange
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.models.send
import java.io.File
import java.util.Date
import java.util.UUID

/**
 * The service that queues messages for delivery and retries failed
 * sends.
 *
 * [MessageOutboxService] holds pending and failed message entries,
 * persists them to disk, and stores their payload files. It publishes
 * a change whenever its contents change.
 */
object MessageOutboxService {
    // MARK: - Properties

    /** The outbox entries, keyed by identifier. */
    val entries = LockIsolated(mapOf<String, OutboxEntry>())

    @Volatile
    private var didLoad = false

    // MARK: - Test Support

    /** Clears state and reloads the persisted outbox; for tests only. */
    internal fun reloadForTesting() {
        entries.wrappedValue = emptyMap()
        didLoad = false
        loadIfNeeded()
    }

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

    /**
     * Atomically claims the entry with the given identifier for
     * retry, transitioning it to `sending`.
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
            persistArchive()
            Logger.log("Claimed outbox entry $id for retry (attempt ${claimed.attemptCount}).", domain = LoggerDomain.outbox)
            emit()
        }
        return claimed
    }

    /** Adds the given entry to the outbox. */
    fun enqueue(entry: OutboxEntry) {
        loadIfNeeded()
        entries.withValue { it.value = it.value + (entry.id to entry) }
        persistArchive()
        Logger.log("Enqueued outbox entry ${entry.id} for conversation ${entry.conversationIDKey}.", domain = LoggerDomain.outbox)
        emit()
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

        persistArchive()
        Logger.log("Marked outbox entry $id as failed (attempt ${failed.attemptCount}).", domain = LoggerDomain.outbox)
        emit()
    }

    /** Removes the outbox entry with the given identifier, deleting its payload files. */
    fun remove(id: String) {
        loadIfNeeded()
        val removed =
            entries.withValue { reference ->
                val entry = reference.value[id] ?: return@withValue null
                reference.value = reference.value - id
                entry
            } ?: return

        removePayloadFile(removed)
        persistArchive()
        Logger.log("Removed outbox entry $id.", domain = LoggerDomain.outbox)
        emit()
    }

    /** Removes every outbox entry, deleting their payload files. */
    fun removeAll() {
        loadIfNeeded()
        val removed =
            entries.withValue { reference ->
                val current = reference.value
                reference.value = emptyMap()
                current
            }

        if (removed.isEmpty()) return
        removed.values.forEach { removePayloadFile(it) }
        persistArchive()
        Logger.log("Removed all outbox entries (${removed.size}).", domain = LoggerDomain.outbox)
        emit()
    }

    // MARK: - Payload Directory Methods

    /**
     * Copies the file at the given path into the outbox payload
     * directory and returns the destination file name.
     *
     * When a thumbnail image exists alongside the source file, it is
     * copied into the payload directory as well, so retried sends
     * upload it with the primary file.
     */
    fun storePayloadFile(from: File): String {
        val directory = FileStore.resolve("outbox") ?: error("File store is not initialized.")
        directory.mkdirs()

        val fileName = "${UUID.randomUUID()}_${from.name}"
        val destination = File(directory, fileName)
        from.copyTo(destination, overwrite = true)

        val sourceThumbnail = from.thumbnailPath
        val destinationThumbnail = destination.thumbnailPath
        if (sourceThumbnail.exists()) {
            sourceThumbnail.copyTo(destinationThumbnail, overwrite = true)
        }

        Logger.log("Stored payload file $fileName.", domain = LoggerDomain.outbox)
        return fileName
    }

    /** Returns the file for the payload with the given name. */
    fun payloadFileURL(fileName: String): File? = FileStore.resolve("outbox/$fileName")

    // MARK: - Auxiliary

    private fun emit() {
        DependencyValues.current.sharedEvents.messageOutboxDidChange.send()
    }

    private fun garbageCollectPayloadFiles() {
        val directory = FileStore.resolve("outbox") ?: return
        val fileNames = directory.listFiles()?.map { it.name } ?: return

        val referencedFileNames =
            entries.wrappedValue.values
                .flatMap { entry ->
                    when (val payload = entry.payload) {
                        is OutboxEntry.Payload.Audio -> listOf(payload.inputFileName)
                        is OutboxEntry.Payload.Media -> {
                            // Media payloads may carry a thumbnail sibling;
                            // reference it so collection preserves both.
                            val thumbnailName = payloadFileURL(payload.fileName)?.thumbnailPath?.name
                            if (thumbnailName == null) listOf(payload.fileName) else listOf(payload.fileName, thumbnailName)
                        }
                        is OutboxEntry.Payload.Text -> emptyList()
                    }
                }.toSet()

        var removedCount = 0
        for (fileName in fileNames) {
            if (fileName !in referencedFileNames) {
                File(directory, fileName).delete()
                removedCount += 1
            }
        }

        if (removedCount > 0) {
            Logger.log("Garbage-collected $removedCount orphaned payload files.", domain = LoggerDomain.outbox)
        }
    }

    private fun loadIfNeeded() {
        if (didLoad) return
        synchronized(this) {
            if (didLoad) return
            didLoad = true

            Persistent.string(PersistentStorageKey.messageOutbox)?.let { archive ->
                val decoded = runCatching { decodeOutboxArchive(archive) }.getOrNull()
                if (decoded != null) {
                    // Reconcile: any entry still marked SENDING at launch
                    // means the app died mid-attempt.
                    val reconciled =
                        decoded.associateBy { it.id }.mapValues { (_, entry) ->
                            if (entry.state == OutboxEntry.State.SENDING) {
                                Logger.log(
                                    "Reconciled stale SENDING entry ${entry.id} to FAILED.",
                                    domain = LoggerDomain.outbox,
                                )
                                entry.copy(state = OutboxEntry.State.FAILED)
                            } else {
                                entry
                            }
                        }

                    entries.wrappedValue = reconciled
                    Logger.log("Loaded ${reconciled.size} outbox entries into memory.", domain = LoggerDomain.outbox)
                }
            }

            garbageCollectPayloadFiles()
        }
    }

    private fun persistArchive() {
        Persistent.setString(PersistentStorageKey.messageOutbox, encodeOutboxArchive(entries.wrappedValue.values.toList()))
    }

    private fun removePayloadFile(entry: OutboxEntry) {
        val fileName =
            when (val payload = entry.payload) {
                is OutboxEntry.Payload.Audio -> payload.inputFileName
                is OutboxEntry.Payload.Media -> payload.fileName
                is OutboxEntry.Payload.Text -> null
            } ?: return

        val file = payloadFileURL(fileName) ?: return
        file.delete()
        file.thumbnailPath.delete()

        Logger.log("Removed payload file $fileName for entry ${entry.id}.", domain = LoggerDomain.outbox)
    }

}

// MARK: - File

/** The file's thumbnail sibling, named with the thumbnail suffix. */
private val File.thumbnailPath: File
    get() = File(parentFile, "${name.substringBeforeLast(".")}-thumbnail.jpeg")
