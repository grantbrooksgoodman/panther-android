//
//  Conversation+RemotelyUpdatable.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.conversation.remotelyupdatable

import us.neotechnica.panther.bundle.conversations
import us.neotechnica.panther.bundle.messages
import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.modules.networking.common.encodeForWrite
import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.conversation.models.ReactionMetadata
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.entity.extensions.currentUserParticipant
import us.neotechnica.panther.modules.session.entity.extensions.filteringSystemMessages
import us.neotechnica.panther.modules.session.entity.extensions.uniquedByID
import us.neotechnica.panther.modules.session.state.services.PendingTranslationArchive
import us.neotechnica.panther.modules.session.state.services.SelfWriteRegistry
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.common.models.WriteAction
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import java.util.Date

// MARK: - Types

/** The remotely updatable keys of a conversation. */
enum class ConversationUpdatableKey(
    val rawValue: String,
) {
    ACTIVITIES("activities"),
    ENCODED_HASH("hash"),
    ID("id"),
    MESSAGES("messages"),
    METADATA("metadata"),
    PARTICIPANTS("participants"),
    REACTION_METADATA("reactionMetadata"),
}

// MARK: - Modify Key

/**
 * Returns a copy of the conversation with the given key set to the
 * given value, or `null` if the value's type does not match the key.
 */
fun Conversation.modifyKey(
    key: ConversationUpdatableKey,
    value: Any,
): Conversation? =
    when (key) {
        ConversationUpdatableKey.ENCODED_HASH, ConversationUpdatableKey.ID -> null

        ConversationUpdatableKey.ACTIVITIES ->
            (value as? List<*>)?.filterIsInstance<Activity>()?.let { updateIDHash(copy(activities = it)) }

        ConversationUpdatableKey.MESSAGES -> {
            val messages = (value as? List<*>)?.filterIsInstance<Message>() ?: return null
            // The session store never stores system messages; filter them
            // here too so hydrated display arrays can't leak their IDs.
            val filtered = messages.uniquedByID.filteringSystemMessages
            // Messages set via updateValues bypass Message.didWrite.
            SessionStore.upsertMessages(filtered.toSet())
            updateIDHash(copy(messageIDs = filtered.map { it.id }.distinct()))
        }

        ConversationUpdatableKey.METADATA ->
            (value as? ConversationMetadata)?.let { updateIDHash(copy(metadata = it)) }

        ConversationUpdatableKey.PARTICIPANTS ->
            (value as? List<*>)?.filterIsInstance<Participant>()?.let { updateIDHash(copy(participants = it)) }

        ConversationUpdatableKey.REACTION_METADATA -> {
            val reactionMetadata = (value as? List<*>)?.filterIsInstance<ReactionMetadata>() ?: return null
            updateIDHash(
                copy(
                    reactionMetadata =
                        if (reactionMetadata.isEmpty() || reactionMetadata.all { it == ReactionMetadata.empty }) {
                            null
                        } else {
                            reactionMetadata
                        },
                ),
            )
        }
    }

// MARK: - Updates Values

/**
 * Applies the given key updates to the conversation and writes the
 * changed fields, the conversation's hash token, and the
 * participants' hash tokens in a single atomic fan-out. Upserts the
 * updated conversation into the session store.
 */
suspend fun Conversation.updateValues(data: Map<ConversationUpdatableKey, Any>): Conversation {
    var updated = filteringSystemMessages
    val changedKeys = mutableSetOf<String>()

    for ((key, value) in data) {
        val modified =
            updated.modifyKey(key, value)
                ?: throw Exception.Networking.typeMismatch(
                    key.rawValue,
                    value,
                    ExceptionMetadata(this),
                )

        updated = modified
        changedKeys.add(key.rawValue)
    }

    val conversationPath = "${NetworkPath.conversations.rawValue}/${updated.id.key}"
    val updates = mutableMapOf<String, Any?>()

    for ((key, value) in updated.encoded) {
        if (changedKeys.contains(key)) updates["$conversationPath/$key"] = value
    }
    updates["$conversationPath/${ConversationUpdatableKey.ENCODED_HASH.rawValue}"] = updated.id.hash
    updates.putAll(buildParticipantUpdates(updated))

    SelfWriteRegistry.record(updated.id)
    Networking.config.databaseDelegate.commit(updates)

    // updateValues bypasses didWrite; this is its only upsert.
    SessionStore.upsertConversation(updated)
    return updated
}

// MARK: - Update

/**
 * Writes the value for the given key to the server and returns the
 * updated conversation.
 */
suspend fun Conversation.update(
    key: ConversationUpdatableKey,
    to: Any,
): Conversation {
    val newValue =
        modifyKey(key, to) ?: throw Exception.Networking.typeMismatch(
            key.rawValue,
            to,
            ExceptionMetadata(this),
        )

    val valueKeyPath = "${NetworkPath.conversations.rawValue}/${id.key}/${key.rawValue}"

    return when (val action = willWrite(to, key, newValue)) {
        is WriteAction.Encoded -> {
            Networking.config.databaseDelegate.setValue(action.value, valueKeyPath)
            didWrite(newValue, key)
        }
        is WriteAction.Handled -> didWrite(action.updated, key)
        WriteAction.Proceed -> {
            Networking.config.databaseDelegate.setValue(encodeForWrite(this, key.rawValue, to), valueKeyPath)
            didWrite(newValue, key)
        }
    }
}

/**
 * Atomically transforms the raw database value for the given key and
 * returns the updated conversation.
 */
suspend fun Conversation.update(
    key: ConversationUpdatableKey,
    applyingRaw: (Any?) -> Any?,
): Conversation {
    val valueKeyPath = "${NetworkPath.conversations.rawValue}/${id.key}/${key.rawValue}"
    val committed =
        Networking.config.databaseDelegate.runTransaction(valueKeyPath, block = applyingRaw)
            ?: throw Exception.Networking.typeMismatch(
                key.rawValue,
                null,
                ExceptionMetadata(this),
            )

    val decoded: Any =
        when (key) {
            ConversationUpdatableKey.REACTION_METADATA ->
                @Suppress("UNCHECKED_CAST")
                (committed as? List<Map<String, Any?>>)?.map { ReactionMetadata.decode(it) }
                    ?: throw Exception.Networking.typeMismatch(
                        key.rawValue,
                        committed,
                        ExceptionMetadata(this),
                    )
            else -> committed
        }

    val updated =
        modifyKey(key, decoded) ?: throw Exception.Networking.typeMismatch(
            key.rawValue,
            decoded,
            ExceptionMetadata(this),
        )

    return didWrite(updated, key)
}

// MARK: - Will Write

/**
 * Prepares a single-field remote update before it is written.
 *
 * When new messages are written, commits the message nodes, their
 * conversation index entries, an un-delete of every participant, a
 * reset of the current user's typing status, the conversation's hash
 * token and last-modified date, the participants' hash tokens, and
 * any pending hosted translation archive entries, in a single atomic
 * fan-out. Other fields proceed with the default write.
 */
suspend fun Conversation.willWrite(
    value: Any,
    key: ConversationUpdatableKey,
    updated: Conversation,
): WriteAction<Conversation> {
    val messages = value as? List<*>
    if (key != ConversationUpdatableKey.MESSAGES ||
        messages == null ||
        messages.filterIsInstance<Message>().filteringSystemMessages.isEmpty()
    ) {
        return WriteAction.Proceed
    }

    val newMessages =
        messages
            .filterIsInstance<Message>()
            .filteringSystemMessages
            .filter { it.id !in messageIDs.toSet() }

    val conversationPath = "${NetworkPath.conversations.rawValue}/${updated.id.key}"
    val currentUserParticipant =
        updated.currentUserParticipant
            ?: throw Exception(
                "Failed to resolve current user participant.",
                metadata = ExceptionMetadata(SessionStore),
            )

    // Reset typing for current user + un-delete all participants.
    val conversation =
        updateIDHash(
            updated.copy(
                participants =
                    participants.map {
                        Participant(
                            userID = it.userID,
                            hasDeletedConversation = false,
                            isTyping = if (it.userID == currentUserParticipant.userID) false else it.isTyping,
                        )
                    },
            ),
        )

    val updates = mutableMapOf<String, Any?>()

    // Message node data + conversation index entries.
    for (message in newMessages) {
        updates["${NetworkPath.messages.rawValue}/${message.id}"] = message.encoded.filterKeys { it != KEY_ID }
        updates["$conversationPath/${ConversationUpdatableKey.MESSAGES.rawValue}/${message.id}"] = true
    }

    // Un-delete participants who had deleted the conversation.
    for (participant in updated.participants.filter { it.hasDeletedConversation }) {
        updates["$conversationPath/${ConversationUpdatableKey.PARTICIPANTS.rawValue}/${participant.userID}/$KEY_HAS_DELETED"] =
            false
    }

    // Reset typing status for the current user.
    updates["$conversationPath/${ConversationUpdatableKey.PARTICIPANTS.rawValue}/${currentUserParticipant.userID}/$KEY_IS_TYPING"] =
        false

    // Conversation hash + last-modified date.
    updates["$conversationPath/${ConversationUpdatableKey.ENCODED_HASH.rawValue}"] = conversation.id.hash
    updates["$conversationPath/${ConversationUpdatableKey.METADATA.rawValue}/$KEY_LAST_MODIFIED"] =
        DependencyValues.current.timestampDateFormatter.format(Date())

    // Participant hash tokens.
    updates.putAll(buildParticipantUpdates(conversation))

    // Drain pending hosted-archive entries so a message node never
    // commits without its translations being resolvable.
    for (message in newMessages) {
        for (reference in message.translationReferences ?: emptyList()) {
            val entry = PendingTranslationArchive.drain(reference.hostingKey) ?: continue
            updates[entry.first] = entry.second
        }
    }

    SelfWriteRegistry.record(conversation.id)
    Networking.config.databaseDelegate.commit(updates)
    return WriteAction.Handled(conversation)
}

// MARK: - Did Write

/**
 * Applies a completed single-field remote update, upserting the
 * updated conversation into the session store. Unless the field was
 * `messages`, also writes the conversation's new hash token and the
 * participants' hash tokens in a single atomic fan-out.
 */
suspend fun Conversation.didWrite(
    updated: Conversation,
    key: ConversationUpdatableKey,
): Conversation {
    // willWrite(.messages) already commits hash and user tokens.
    if (key == ConversationUpdatableKey.MESSAGES || updated.id.hash == id.hash) {
        SessionStore.upsertConversation(updated)
        return updated
    }

    val updates = mutableMapOf<String, Any?>()
    updates["${NetworkPath.conversations.rawValue}/$identifier/${ConversationUpdatableKey.ENCODED_HASH.rawValue}"] =
        updated.id.hash
    updates.putAll(buildParticipantUpdates(updated))

    SelfWriteRegistry.record(updated.id)
    Networking.config.databaseDelegate.commit(updates)
    SessionStore.upsertConversation(updated)
    return updated
}

// MARK: - Auxiliary

private val Conversation.identifier: String
    get() = id.key

/** Builds fan-out entries updating each participant's conversation token to the conversation's hash. */
private fun buildParticipantUpdates(conversation: Conversation): Map<String, Any?> {
    val updates = mutableMapOf<String, Any?>()
    for (participant in conversation.participants) {
        updates["${NetworkPath.users.rawValue}/${participant.userID}/$KEY_OPEN_CONVERSATIONS/${conversation.id.key}"] =
            conversation.id.hash
    }
    return updates
}

private fun Conversation.updateIDHash(conversation: Conversation): Conversation =
    conversation.copy(id = ConversationID(key = conversation.id.key, hash = conversation.encodedHash))

private const val KEY_ID = "id"
private const val KEY_HAS_DELETED = "hasDeletedConversation"
private const val KEY_IS_TYPING = "isTyping"
private const val KEY_LAST_MODIFIED = "lastModified"
private const val KEY_OPEN_CONVERSATIONS = "openConversations"
