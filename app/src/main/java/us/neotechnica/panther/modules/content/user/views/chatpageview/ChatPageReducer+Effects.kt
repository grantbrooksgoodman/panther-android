//
//  ChatPageReducer+Effects.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.ReadReceiptService
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.services.ReactionSessionService
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

internal fun reactEffect(
    message: Message,
    style: Reaction.Style,
): Effect<ChatPageReducer.Action> =
    Effect.run {
        val currentUserID = User.currentUserID ?: return@run
        try {
            ReactionSessionService.react(Reaction(style, currentUserID), message)
        } catch (exception: Exception) {
            Logger.log(exception, with = AlertType.toast)
        }
    }

internal fun sendMediaEffect(mediaFile: MediaFile): Effect<ChatPageReducer.Action> =
    Effect.run {
        MessageDeliveryService.sendMediaMessage(mediaFile)
    }

internal fun sendEffect(text: String): Effect<ChatPageReducer.Action> =
    Effect.run {
        MessageDeliveryService.sendTextMessage(text)
    }

internal fun markReadEffect(): Effect<ChatPageReducer.Action> = Effect.run { markCurrentConversationAsRead() }

internal suspend fun markCurrentConversationAsRead() {
    try {
        ReadReceiptService.updateReadDateForUnreadMessages()
    } catch (exception: Exception) {
        Logger.log(exception)
    }
}
