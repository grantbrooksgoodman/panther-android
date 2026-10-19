//
//  Participant.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.conversation.models

import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

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
    /**
     * A Boolean value that indicates whether the user has deleted
     * the conversation.
     */
    val hasDeletedConversation: Boolean = false,
    /**
     * A Boolean value that indicates whether the user is
     * currently typing.
     */
    val isTyping: Boolean = false,
) : Serializable<Map<String, Any?>> {
    // MARK: - Types

    /** The serializable keys for encoding and decoding the model. */
    enum class SerializableKey(
        val rawValue: String,
    ) {
        HAS_DELETED_CONVERSATION("hasDeletedConversation"),
        IS_TYPING("isTyping"),
        USER_ID("userID"),
    }

    // MARK: - Computed Properties

    /**
     * Whether the participant is well-formed (has a usable user
     * ID).
     */
    val isWellFormed: Boolean
        get() = userID.isNotBlank() && !userID.isBangQualifiedEmpty

    /** The serialized representation of the participant. */
    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                SerializableKey.HAS_DELETED_CONVERSATION.rawValue to hasDeletedConversation,
                SerializableKey.IS_TYPING.rawValue to isTyping,
                SerializableKey.USER_ID.rawValue to userID,
            )

    // MARK: - Companion

    companion object : SerializableDecoder<Participant, Map<String, Any?>> {
        override fun canDecode(data: Map<String, Any?>): Boolean =
            data[SerializableKey.HAS_DELETED_CONVERSATION.rawValue] is Boolean &&
                data[SerializableKey.IS_TYPING.rawValue] is Boolean &&
                data[SerializableKey.USER_ID.rawValue] is String

        // NIT: Unused on Android.
        override fun decode(data: Map<String, Any?>): Participant {
            val hasDeletedConversation = data[SerializableKey.HAS_DELETED_CONVERSATION.rawValue] as? Boolean
            val isTyping = data[SerializableKey.IS_TYPING.rawValue] as? Boolean
            val userID = data[SerializableKey.USER_ID.rawValue] as? String

            if (hasDeletedConversation == null || isTyping == null || userID == null) {
                throw Exception.Networking.decodingFailed(
                    data,
                    ExceptionMetadata(this),
                )
            }

            return Participant(
                userID = userID,
                hasDeletedConversation = hasDeletedConversation,
                isTyping = isTyping,
            )
        }
    }
}
