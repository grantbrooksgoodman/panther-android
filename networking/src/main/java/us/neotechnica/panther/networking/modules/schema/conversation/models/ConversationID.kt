//
//  ConversationID.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.conversation.models

import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder

/**
 * A conversation's composite identifier.
 *
 * A conversation identifier pairs a stable [key] that locates the
 * conversation in the database with a [hash] token that
 * identifies the conversation's current content version.
 */
data class ConversationID(
    /** The stable key that locates the conversation in the database. */
    val key: String,
    /** The token that identifies the conversation's current content version. */
    val hash: String,
) : Serializable<String> {
    // MARK: - Computed Properties

    /** The serialized representation, `"<key> | <hash>"`. */
    override val encoded: String
        get() = "$key | $hash"

    // MARK: - Companion

    companion object : SerializableDecoder<ConversationID, String> {
        override fun canDecode(data: String): Boolean = data.split(" | ").size == 2

        override suspend fun decode(data: String): ConversationID {
            val components = data.split(" | ")
            if (components.size != 2) throw decodingFailure(this, data)
            return ConversationID(
                key = components[0],
                hash = components[1],
            )
        }

        /**
         * Creates a conversation identifier from its string
         * representation, or `null` if the string does not
         * contain exactly a key and hash separated by `" | "`.
         */
        fun from(string: String): ConversationID? {
            val components = string.split(" | ")
            if (components.size != 2) return null
            return ConversationID(
                key = components[0],
                hash = components[1],
            )
        }
    }
}
