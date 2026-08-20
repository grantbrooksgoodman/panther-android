//
//  Conversation.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.conversation.models

import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash

/**
 * A conversation between two or more participants.
 *
 * The conversation's [encodedHash] is its content-version token:
 * every content mutation recomputes it, and it is stored in
 * [id]'s hash.
 */
data class Conversation(
    /** The conversation's composite identifier. */
    val id: ConversationID,
    /** The conversation's activity log, or `null` if none. */
    val activities: List<Activity>?,
    /** The identifiers of the conversation's messages. */
    val messageIDs: List<String>,
    /** The conversation's descriptive metadata. */
    val metadata: ConversationMetadata,
    /** The conversation's participants. */
    val participants: List<Participant>,
    /** The reactions applied to the conversation's messages, or `null`. */
    val reactionMetadata: List<ReactionMetadata>?,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Type Aliases

    private enum class Keys(
        val rawValue: String,
    ) {
        ID("id"),
        ACTIVITIES("activities"),
        ENCODED_HASH("hash"),
        MESSAGES("messages"),
        METADATA("metadata"),
        PARTICIPANTS("participants"),
        REACTION_METADATA("reactionMetadata"),
    }

    // MARK: - Computed Properties

    /** The serialized representation of the conversation. */
    override val encoded: Map<String, Any?>
        get() {
            val messagesMap =
                messageIDs
                    .filter { it.startsWith("-") }
                    .associateWith { true }

            val participantsMap =
                participants.associate { participant ->
                    participant.userID to
                        mapOf(
                            Participant.Keys.HAS_DELETED_CONVERSATION.rawValue to
                                participant.hasDeletedConversation,
                            Participant.Keys.IS_TYPING.rawValue to participant.isTyping,
                        )
                }

            return mapOf(
                Keys.ID.rawValue to id.encoded,
                Keys.ACTIVITIES.rawValue to
                    (activities?.map { it.encoded } ?: listOf(Activity.empty.encoded)),
                Keys.ENCODED_HASH.rawValue to encodedHash,
                Keys.MESSAGES.rawValue to messagesMap,
                Keys.METADATA.rawValue to metadata.encoded,
                Keys.PARTICIPANTS.rawValue to participantsMap,
                Keys.REACTION_METADATA.rawValue to
                    (reactionMetadata?.map { it.encoded } ?: listOf(ReactionMetadata.empty.encoded)),
            )
        }

    override val hashFactors: List<String>
        get() {
            val formatter = DependencyValues.current.timestampDateFormatter
            return buildList {
                add(id.key)
                addAll(activities?.map { it.encodedHash } ?: emptyList())
                addAll(messageIDs.filter { it.startsWith("-") })
                add(metadata.name)
                add(metadata.imageHash ?: BANG_QUALIFIED_EMPTY)
                add(metadata.isPenPalsConversation.toString())
                add(formatter.format(metadata.lastModifiedDate))
                addAll(metadata.messageRecipientConsentAcknowledgementData.map { it.encoded })
                addAll(metadata.penPalsSharingData.map { it.encoded })
                add(metadata.requiresConsentFromInitiator ?: BANG_QUALIFIED_EMPTY)
                // Content-version only: userID + hasDeletedConversation. isTyping
                // is excluded so typing writes do not mint version tokens.
                addAll(participants.map { "${it.userID} | ${it.hasDeletedConversation}" })
                addAll(reactionMetadata?.map { it.encodedHash } ?: emptyList())
            }.sorted()
        }

    // MARK: - Companion

    companion object : SerializableDecoder<Conversation, Map<String, Any?>> {
        override fun canDecode(data: Map<String, Any?>): Boolean {
            if (data[Keys.ID.rawValue] !is String) return false
            val activities = mapList(data, Keys.ACTIVITIES) ?: return false
            if (!activities.all { Activity.canDecode(it) }) return false
            val metadata = data[Keys.METADATA.rawValue] as? Map<*, *> ?: return false

            @Suppress("UNCHECKED_CAST")
            if (!ConversationMetadata.canDecode(metadata as Map<String, Any?>)) return false

            val participantMap = data[Keys.PARTICIPANTS.rawValue] as? Map<*, *> ?: return false
            if (participantMap.size <= 1) return false
            val reactionMetadata = mapList(data, Keys.REACTION_METADATA) ?: return false
            return reactionMetadata.all { ReactionMetadata.canDecode(it) }
        }

        override suspend fun decode(data: Map<String, Any?>): Conversation {
            val idString = data[Keys.ID.rawValue] as? String
            val encodedActivities = mapList(data, Keys.ACTIVITIES)
            val encodedMetadata =
                (data[Keys.METADATA.rawValue] as? Map<*, *>)?.let {
                    @Suppress("UNCHECKED_CAST")
                    it as Map<String, Any?>
                }
            val encodedReactionMetadata = mapList(data, Keys.REACTION_METADATA)
            val participantMap =
                (data[Keys.PARTICIPANTS.rawValue] as? Map<*, *>)?.let {
                    @Suppress("UNCHECKED_CAST")
                    it as Map<String, Map<String, Any?>>
                }

            if (idString == null ||
                encodedActivities == null ||
                encodedMetadata == null ||
                encodedReactionMetadata == null ||
                participantMap == null
            ) {
                throw decodingFailure(this, data)
            }

            val messageIDs = messageKeys(data)
            val participants =
                participantMap
                    .map { (userID, values) ->
                        val hasDeleted = values[Participant.Keys.HAS_DELETED_CONVERSATION.rawValue] as? Boolean
                        val isTyping = values[Participant.Keys.IS_TYPING.rawValue] as? Boolean
                        if (hasDeleted == null || isTyping == null) throw decodingFailure(this, values)
                        Participant(
                            userID = userID,
                            hasDeletedConversation = hasDeleted,
                            isTyping = isTyping,
                        )
                    }.sortedBy { it.userID }

            val reactionMetadata = encodedReactionMetadata.map { ReactionMetadata.decode(it) }

            return Conversation(
                id = ConversationID.decode(idString),
                activities = encodedActivities.map { Activity.decode(it) },
                messageIDs = if (messageIDs.isBangQualifiedEmpty) bangQualifiedEmptyList else messageIDs,
                metadata = ConversationMetadata.decode(encodedMetadata),
                participants = participants,
                reactionMetadata =
                    if (reactionMetadata.all { it == ReactionMetadata.empty }) {
                        null
                    } else {
                        reactionMetadata
                    },
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun mapList(
            data: Map<String, Any?>,
            key: Keys,
        ): List<Map<String, Any?>>? =
            (data[key.rawValue] as? List<*>)
                ?.takeIf { list -> list.all { it is Map<*, *> } }
                ?.map { it as Map<String, Any?> }

        private fun messageKeys(data: Map<String, Any?>): List<String> =
            (data[Keys.MESSAGES.rawValue] as? Map<*, *>)
                ?.keys
                ?.filterIsInstance<String>()
                ?.sorted()
                ?: emptyList()
    }
}
