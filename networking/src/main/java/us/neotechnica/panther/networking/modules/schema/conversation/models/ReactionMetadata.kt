//
//  ReactionMetadata.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.conversation.models

import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable

/**
 * The reactions applied to a single message.
 */
data class ReactionMetadata(
    /** The identifier of the message the reactions apply to. */
    val messageID: String,
    /** The reactions applied to the message. */
    val reactions: List<Reaction>,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Type Aliases

    private enum class Keys(
        val rawValue: String,
    ) {
        MESSAGE_ID("messageID"),
        REACTIONS("reactions"),
    }

    // MARK: - Computed Properties

    /** The serialized representation of the reaction metadata. */
    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.MESSAGE_ID.rawValue to messageID,
                Keys.REACTIONS.rawValue to reactions.map { it.encoded },
            )

    override val hashFactors: List<String>
        get() =
            buildList {
                add(messageID)
                addAll(reactions.map { it.userID })
                addAll(reactions.map { it.style.encodedValue })
            }.sorted()

    // MARK: - Companion

    companion object : SerializableDecoder<ReactionMetadata, Map<String, Any?>> {
        /** An empty reaction metadata placeholder. */
        val empty =
            ReactionMetadata(
                messageID = BANG_QUALIFIED_EMPTY,
                reactions =
                    listOf(
                        Reaction(
                            style = Reaction.Style.orderedCases.first(),
                            userID = BANG_QUALIFIED_EMPTY,
                        ),
                    ),
            )

        override fun canDecode(data: Map<String, Any?>): Boolean {
            if (data[Keys.MESSAGE_ID.rawValue] !is String) return false
            val encodedReactions = reactionMaps(data) ?: return false
            return encodedReactions.all { Reaction.canDecode(it) }
        }

        override suspend fun decode(data: Map<String, Any?>): ReactionMetadata {
            val messageID = data[Keys.MESSAGE_ID.rawValue] as? String
            val encodedReactions = reactionMaps(data)

            if (messageID == null || encodedReactions == null) {
                throw decodingFailure(this, data)
            }

            return ReactionMetadata(
                messageID = messageID,
                reactions = encodedReactions.map { Reaction.decode(it) },
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun reactionMaps(data: Map<String, Any?>): List<Map<String, Any?>>? =
            (data[Keys.REACTIONS.rawValue] as? List<*>)
                ?.takeIf { list -> list.all { it is Map<*, *> } }
                ?.map { it as Map<String, Any?> }
    }
}
