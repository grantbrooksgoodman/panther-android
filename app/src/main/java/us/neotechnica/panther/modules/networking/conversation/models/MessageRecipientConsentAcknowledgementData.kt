//
//  MessageRecipientConsentAcknowledgementData.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.conversation.models

import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * A record of whether a participant has acknowledged the
 * message-recipient consent requirement.
 *
 * Serializes as `"<userID>: <flag>"`, where the flag is `"!"`
 * when consent is acknowledged and `"false"` otherwise.
 */
data class MessageRecipientConsentAcknowledgementData(
    /** The identifier of the acknowledging user. */
    val userID: String,
    /** A Boolean value that indicates whether consent was acknowledged. */
    val consentAcknowledged: Boolean,
) : Serializable<String> {
    // MARK: - Computed Properties

    /** The serialized representation of the acknowledgement record. */
    override val encoded: String
        get() = "$userID: ${if (consentAcknowledged) "!" else "false"}"

    // MARK: - Companion

    companion object : SerializableDecoder<MessageRecipientConsentAcknowledgementData, String> {
        /**
         * Returns an acknowledgement record for each of the given users,
         * all seeded to [consentAcknowledged].
         */
        fun prepopulated(
            userIDs: List<String>,
            consentAcknowledged: Boolean,
        ): List<MessageRecipientConsentAcknowledgementData> =
            userIDs.map { MessageRecipientConsentAcknowledgementData(userID = it, consentAcknowledged = consentAcknowledged) }

        override fun canDecode(data: String): Boolean {
            val components = data.split(": ")
            if (components.size != 2) return false
            val booleanString = components[1]
            return booleanString == "false" ||
                booleanString == "true" ||
                booleanString.isBangQualifiedEmpty
        }

        override fun decode(data: String): MessageRecipientConsentAcknowledgementData {
            val components = data.split(": ")
            if (components.size != 2) {
                throw Exception.Networking.decodingFailed(
                    data,
                    ExceptionMetadata(this),
                )
            }

            return MessageRecipientConsentAcknowledgementData(
                userID = components[0],
                consentAcknowledged = components[1] != "false",
            )
        }
    }
}
