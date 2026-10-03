//
//  ReadReceiptService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 26/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.modules.common.services.NotificationService
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.calculateBadgeNumber
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.messages
import us.neotechnica.panther.modules.session.entity.extensions.updateReadDate
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService

/**
 * Manages read receipts.
 *
 * [ReadReceiptService] marks the displayed conversation's incoming
 * messages as read, then updates the application badge with the
 * resulting unread count.
 */
object ReadReceiptService {
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
