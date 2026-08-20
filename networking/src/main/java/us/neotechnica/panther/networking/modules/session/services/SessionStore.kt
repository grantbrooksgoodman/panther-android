//
//  SessionStore.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.conversation.models.ConversationID
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.sessionStoreDidChange
import us.neotechnica.panther.networking.modules.session.models.SessionStoreChange
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

/**
 * The in-memory source of truth for the signed-in user's world:
 * conversations, messages, and users.
 *
 * Reads are synchronous snapshots; mutations emit a
 * [SessionStoreChange] through the `sessionStoreDidChange` shared event
 * so views recompute. The session services upsert resolved entities
 * here; the UI reads from here.
 *
 * **Note:** the iOS store also persists archives to disk for offline
 * cold starts; this Phase 6 port keeps everything in memory and
 * refetches on launch, which the acceptance criterion permits.
 */
object SessionStore {
    // MARK: - Properties

    private val conversationsByIDKey = LockIsolated(mapOf<String, Conversation>())
    private val messagesByID = LockIsolated(mapOf<String, Message>())
    private val usersByID = LockIsolated(mapOf<String, User>())

    // MARK: - Computed Properties

    /** Every stored conversation, keyed by conversation-ID key. */
    val conversations: Map<String, Conversation> get() = conversationsByIDKey.wrappedValue

    /** Every stored message, keyed by message ID. */
    val messages: Map<String, Message> get() = messagesByID.wrappedValue

    /** Every stored user, keyed by user ID. */
    val users: Map<String, User> get() = usersByID.wrappedValue

    // MARK: - Conversations

    /** The conversation matching [id] exactly (key and hash), or `null`. */
    fun getConversation(id: ConversationID): Conversation? {
        val conversation = conversationsByIDKey.wrappedValue[id.key] ?: return null
        return if (conversation.id == id) conversation else null
    }

    /** The conversation with the given key, ignoring hash, or `null`. */
    fun getConversation(idKey: String): Conversation? = conversationsByIDKey.wrappedValue[idKey]

    /** Upserts a single conversation, emitting a change when it differs. */
    fun upsertConversation(conversation: Conversation) {
        if (!conversation.isStorable) return
        var changed = false
        conversationsByIDKey.withValue {
            if (it.value[conversation.id.key] != conversation) changed = true
            it.value = it.value + (conversation.id.key to conversation)
        }
        if (changed) {
            emit(SessionStoreChange.Conversations(setOf(conversation.id.key), emptySet()))
        }
    }

    /** Upserts a set of conversations, emitting a single change. */
    fun upsertConversations(newConversations: Set<Conversation>) {
        val changedIDKeys = mutableSetOf<String>()
        conversationsByIDKey.withValue { current ->
            for (conversation in newConversations.filter { it.isStorable }) {
                if (current.value[conversation.id.key] != conversation) changedIDKeys.add(conversation.id.key)
                current.value = current.value + (conversation.id.key to conversation)
            }
        }
        if (changedIDKeys.isNotEmpty()) {
            emit(SessionStoreChange.Conversations(changedIDKeys, emptySet()))
        }
    }

    /** Removes a conversation and any messages it alone referenced. */
    fun removeConversation(idKey: String) {
        var orphanedMessageIDs = emptySet<String>()
        var removed = false
        conversationsByIDKey.withValue { current ->
            val conversation = current.value[idKey] ?: return@withValue
            removed = true
            val conversationMessageIDs = conversation.messageIDs.toSet()
            current.value = current.value - idKey
            val remainingMessageIDs =
                current.value.values
                    .flatMap { it.messageIDs }
                    .toSet()
            orphanedMessageIDs = conversationMessageIDs - remainingMessageIDs
        }
        if (!removed) return
        if (orphanedMessageIDs.isNotEmpty()) removeMessages(orphanedMessageIDs)
        emit(SessionStoreChange.Conversations(emptySet(), setOf(idKey)))
    }

    // MARK: - Messages

    /** Upserts a set of messages, emitting a single change. */
    fun upsertMessages(newMessages: Set<Message>) {
        val changedIDs = mutableSetOf<String>()
        messagesByID.withValue { current ->
            for (message in newMessages) {
                if (current.value[message.id] != message) changedIDs.add(message.id)
                current.value = current.value + (message.id to message)
            }
        }
        if (changedIDs.isNotEmpty()) emit(SessionStoreChange.Messages(changedIDs, emptySet()))
    }

    /** Removes the messages with the given IDs. */
    fun removeMessages(ids: Set<String>) {
        val removedIDs = mutableSetOf<String>()
        messagesByID.withValue { current ->
            for (id in ids) {
                if (current.value.containsKey(id)) {
                    current.value = current.value - id
                    removedIDs.add(id)
                }
            }
        }
        if (removedIDs.isNotEmpty()) emit(SessionStoreChange.Messages(emptySet(), removedIDs))
    }

    // MARK: - Users

    /** Upserts a single user, emitting a change when it differs. */
    fun upsertUser(user: User) {
        var changed = false
        usersByID.withValue {
            if (it.value[user.id] != user) changed = true
            it.value = it.value + (user.id to user)
        }
        if (changed) emit(SessionStoreChange.Users(setOf(user.id), emptySet()))
    }

    /** Upserts a set of users, emitting a single change. */
    fun upsertUsers(newUsers: Set<User>) {
        val changedIDs = mutableSetOf<String>()
        usersByID.withValue { current ->
            for (user in newUsers) {
                if (current.value[user.id] != user) changedIDs.add(user.id)
                current.value = current.value + (user.id to user)
            }
        }
        if (changedIDs.isNotEmpty()) emit(SessionStoreChange.Users(changedIDs, emptySet()))
    }

    /** Removes all stored entities, e.g. on sign-out. */
    fun clear() {
        conversationsByIDKey.wrappedValue = mapOf()
        messagesByID.wrappedValue = mapOf()
        usersByID.wrappedValue = mapOf()
    }

    // MARK: - Auxiliary

    private val Conversation.isStorable: Boolean
        get() = id.key.isNotBlank() && id.hash.isNotBlank()

    private fun emit(change: SessionStoreChange) {
        DependencyValues.current.sharedEvents.sessionStoreDidChange
            .send(change)
    }
}
