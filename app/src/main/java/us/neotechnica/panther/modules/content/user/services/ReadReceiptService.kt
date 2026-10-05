//
//  ReadReceiptService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.modules.common.services.NotificationService
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.calculateBadgeNumber
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.messages
import us.neotechnica.panther.modules.session.entity.extensions.updateReadDate
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Manages read receipts.
 *
 * [ReadReceiptService] marks the displayed conversation's incoming
 * messages as read, then updates the application badge with the
 * resulting unread count.
 */
object ReadReceiptService {
    // MARK: - Properties

    private val flushScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // MARK: - Update Read Date for Unread Messages

    /**
     * Marks the displayed conversation's unread incoming messages as
     * read.
     *
     * Has no effect when the most recent incoming message has already
     * been read, or when there are no unread incoming messages.
     *
     * @throws Exception if updating the read dates fails.
     */
    suspend fun updateReadDateForUnreadMessages() {
        val conversation = ConversationSessionService.currentConversation ?: return
        updateReadDateForUnreadMessages(conversation)
    }

    /**
     * Marks the given conversation's unread incoming messages as read
     * on a detached scope that outlives the caller.
     *
     * Use this method to flush any read receipts still pending when a
     * chat page is left, which the page's own mark-read effect would
     * otherwise drop as its view model is closed. The conversation is
     * resolved fresh from the store, so a message that arrived just
     * before the page was left is still marked read.
     *
     * @param conversationIDKey The key of the conversation to flush.
     */
    fun flushUnreadMessages(conversationIDKey: String) {
        flushScope.launch {
            try {
                val conversation = SessionStore.getConversation(conversationIDKey) ?: return@launch
                updateReadDateForUnreadMessages(conversation)
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }
    }

    // MARK: - Auxiliary

    private suspend fun updateReadDateForUnreadMessages(conversation: Conversation) {
        val currentUserID = User.currentUserID ?: return
        val messages = conversation.messages?.filter { !it.isFromCurrentUser } ?: return

        val last = messages.lastOrNull() ?: return
        if (last.readReceipts?.any { it.userID == currentUserID } == true) return

        val unreadMessages = messages.filter { message -> message.readReceipts?.any { it.userID == currentUserID } != true }
        if (unreadMessages.isEmpty()) return

        conversation.updateReadDate(unreadMessages)

        val currentUser = UserSessionService.currentUser ?: return
        NotificationService.setBadgeNumber(currentUser.calculateBadgeNumber())
    }
}
