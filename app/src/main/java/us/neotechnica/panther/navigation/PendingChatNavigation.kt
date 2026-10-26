//
//  PendingChatNavigation.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import java.util.concurrent.atomic.AtomicReference

/**
 * Holds a conversation the app should open once it reaches the signed-in
 * content, set when a push notification is tapped.
 *
 * The splash flow opens the pending conversation after resolving the
 * current user, through
 * [us.neotechnica.panther.modules.common.services.sources.NotificationService.openPendingNotificationConversation].
 */
object PendingChatNavigation {
    // MARK: - Types

    /**
     * A conversation to open, with the message to reveal in it and the
     * user the tapped notification was intended for.
     */
    data class PendingChat(
        /** The identifier key of the conversation to open. */
        val conversationIDKey: String,
        /**
         * The identifier of the message to reveal, or `null` to open the
         * conversation at its latest message.
         */
        val focusedMessageID: String? = null,
        /**
         * The identifier of the user the tapped notification was
         * intended for, or `null` if the conversation is not opened from
         * a notification.
         */
        val recipientUserID: String? = null,
    )

    // MARK: - Properties

    /** The intent extra holding the key of the conversation to open. */
    const val CONVERSATION_ID_KEY_EXTRA = "conversationIDKey"

    /** The intent extra holding the identifier of the reacted-to message. */
    const val REACTION_MESSAGE_ID_EXTRA = "reactionMessageID"

    /**
     * The intent extra holding the identifier of the user the
     * notification was intended for.
     */
    const val RECIPIENT_USER_ID_EXTRA = "recipientUserID"

    private val pending = AtomicReference<PendingChat?>(null)

    // MARK: - Methods

    /** Returns and clears the pending conversation, if any. */
    fun consume(): PendingChat? = pending.getAndSet(null)

    /**
     * Records [pendingChat] as the conversation to open next.
     *
     * A `null` value, or one whose conversation key is blank, has no
     * effect.
     */
    fun set(pendingChat: PendingChat?) {
        if (pendingChat != null && pendingChat.conversationIDKey.isNotBlank()) pending.set(pendingChat)
    }
}
