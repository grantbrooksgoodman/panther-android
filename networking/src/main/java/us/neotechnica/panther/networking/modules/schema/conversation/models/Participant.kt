//
//  Participant.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.conversation.models

import us.neotechnica.panther.networking.modules.common.interfaces.Serializable

/**
 * A member of a conversation.
 *
 * Inside a serialized conversation the [userID] becomes a map key
 * and only the two Boolean fields are nested; the standalone
 * [encoded] representation includes all three fields.
 */
data class Participant(
    /** The identifier of the participating user. */
    val userID: String,
    /** A Boolean value that indicates whether the user has deleted the conversation. */
    val hasDeletedConversation: Boolean,
    /** A Boolean value that indicates whether the user is currently typing. */
    val isTyping: Boolean,
) : Serializable<Map<String, Any?>> {
    // MARK: - Type Aliases

    internal enum class Keys(
        val rawValue: String,
    ) {
        HAS_DELETED_CONVERSATION("hasDeletedConversation"),
        IS_TYPING("isTyping"),
        USER_ID("userID"),
    }

    // MARK: - Computed Properties

    /** The serialized representation of the participant. */
    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.HAS_DELETED_CONVERSATION.rawValue to hasDeletedConversation,
                Keys.IS_TYPING.rawValue to isTyping,
                Keys.USER_ID.rawValue to userID,
            )

    // MARK: - Companion

    companion object {
        /**
         * Returns a Boolean value that indicates whether a
         * participant can be decoded from the given data.
         */
        fun canDecode(data: Map<String, Any?>): Boolean =
            data[Keys.HAS_DELETED_CONVERSATION.rawValue] is Boolean &&
                data[Keys.IS_TYPING.rawValue] is Boolean &&
                data[Keys.USER_ID.rawValue] is String
    }
}
