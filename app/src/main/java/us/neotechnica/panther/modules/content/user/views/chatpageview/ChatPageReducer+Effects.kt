//
//  ChatPageReducer+Effects.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.ReadReceiptService
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.models.ReactionSessionServiceEffectID
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType

private val deferredReactionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

internal fun reactEffect(
    message: Message,
    style: Reaction.Style,
): Effect<ChatPageReducer.Action> =
    Effect.run {
        performReaction(message, style)
    }

// While a reaction is already being applied, defer and re-fire once
// it completes rather than overlapping transactions.
private suspend fun performReaction(
    message: Message,
    style: Reaction.Style,
) {
    if (DependencyValues.current.clientSession.entity.reaction.isReactingToMessage) {
        DependencyValues.current.clientSession.entity.reaction.addEffectUponIsReactingToMessage(
            state = false,
            id = ReactionSessionServiceEffectID("deferredReaction-" + (1..DEFERRED_REACTION_ID_BOUND).random()),
        ) { deferredReactionScope.launch { performReaction(message, style) } }

        return
    }

    val reaction = Reaction.from(style) ?: return
    try {
        DependencyValues.current.clientSession.entity.reaction
            .react(reaction, message)
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

private const val DEFERRED_REACTION_ID_BOUND = 1_000_000
