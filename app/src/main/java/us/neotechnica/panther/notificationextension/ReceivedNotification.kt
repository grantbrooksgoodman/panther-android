//
//  ReceivedNotification.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.notificationextension

import us.neotechnica.panther.bundle.notifications
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * The values a push payload must carry for the app to respond to
 * it.
 */
internal data class ReceivedNotification(
    /** The notification's body text. */
    val body: String,
    /** The identifier key of the conversation the notification describes. */
    val conversationIDKey: String,
    /** The identifier of the reacted-to message, or the empty sentinel. */
    val reactionMessageID: String,
    /** The identifier of the user the notification is intended for. */
    val recipientUserID: String,
    /** The notification's title text, or `null` if the payload has none. */
    val title: String?,
) {
    // MARK: - Computed Properties

    /** Whether the notification describes a reaction. */
    val isReaction: Boolean
        get() = !reactionMessageID.isBangQualifiedEmpty

    // MARK: - Companion

    companion object {
        /**
         * Reads the notification values from the push payload's data.
         *
         * The body is logged first, so a malformed payload still leaves
         * a trace.
         *
         * @param data The push payload's data map.
         *
         * @return The received notification.
         *
         * @throws Exception if a required value is missing.
         */
        fun from(data: Map<String, String>): ReceivedNotification {
            val body = data[BODY_KEY].orEmpty()
            Logger.log(
                "Received notification.\n\"$body\"",
                domain = LoggerDomain.notifications,
            )

            val conversationIDKey = data[NotificationExtensionConstants.CONVERSATION_ID_KEY_USER_INFO_KEY]
            val reactionMessageID = data[NotificationExtensionConstants.REACTION_MESSAGE_ID_USER_INFO_KEY]
            val recipientUserID = data[RECIPIENT_USER_ID_USER_INFO_KEY]
            if (conversationIDKey == null ||
                reactionMessageID == null ||
                recipientUserID == null
            ) {
                throw Exception(
                    "Failed to resolve required values.",
                    metadata = ExceptionMetadata(this),
                )
            }

            return ReceivedNotification(
                body = body,
                conversationIDKey = conversationIDKey,
                reactionMessageID = reactionMessageID,
                recipientUserID = recipientUserID,
                title = data[TITLE_KEY],
            )
        }

        private const val BODY_KEY = "body"
        private const val RECIPIENT_USER_ID_USER_INFO_KEY = "recipientUserID"
        private const val TITLE_KEY = "title"
    }
}
