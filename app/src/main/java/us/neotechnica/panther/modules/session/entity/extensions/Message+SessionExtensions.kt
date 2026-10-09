//
//  Message+SessionExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.extensions

import us.neotechnica.panther.modules.common.constants.CommonConstants
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.models.ReadReceipt
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.models.OutboxEntry

/** Whether the message was sent by the current user. */
val Message.isFromCurrentUser: Boolean
    get() = fromAccountID == User.currentUserID

/** Whether the current user has read the message. */
val Message.isReadByCurrentUser: Boolean
    get() = readReceipts?.any { it.userID == User.currentUserID } == true

/** Whether the message is a mock, representing a message not yet sent. */
val Message.isMock: Boolean
    get() = id == CommonConstants.NEW_MESSAGE_ID

/** Whether the message is staged in the outbox awaiting delivery. */
val Message.isOutboxMessage: Boolean
    get() = id.startsWith(OutboxEntry.ID_PREFIX)

/** Whether the message is a system message, such as an activity notice. */
val Message.isSystemMessage: Boolean
    get() = fromAccountID == CommonConstants.SYSTEM_MESSAGE_ID

/** The other participant's read receipt, if any (for delivery status). */
val Message.otherParticipantReadReceipt: ReadReceipt?
    get() = readReceipts?.firstOrNull { it.userID != User.currentUserID }
