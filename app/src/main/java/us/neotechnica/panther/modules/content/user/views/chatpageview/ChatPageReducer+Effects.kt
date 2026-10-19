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
import us.neotechnica.panther.modules.content.user.dependencies.inputBarService
import us.neotechnica.panther.modules.content.user.extensions.chatPageHeaderLabelText
import us.neotechnica.panther.modules.content.user.extensions.isEmpty
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.chatpageview.ChatPageViewService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.ReadReceiptService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.models.ReactionSessionServiceEffectID
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import kotlin.time.Duration.Companion.milliseconds

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

internal fun sendEffect(text: String): Effect<ChatPageReducer.Action> =
    Effect.run {
        try {
            DependencyValues.current.inputBarService.actionHandler
                .didPressSendButton(text)
        } catch (exception: Exception) {
            Logger.log(exception, with = AlertType.toast)
        }
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

// MARK: - Reloads

// One identifier for both change sources: a burst of session store and
// outbox changes reloads once.
internal enum class ReloadEffectID {
    CHAT_PAGE_RELOAD,
}

internal fun reloadEffect(
    debounceMilliseconds: Long,
    id: ReloadEffectID,
): Effect<ChatPageReducer.Action> =
    Effect
        .task<ChatPageReducer.Action>(delay = debounceMilliseconds.milliseconds) { ChatPageReducer.Action.ReloadRequested }
        .cancellable(id, cancelInFlight = true)

// MARK: - Appearance

internal fun viewAppearedEffect(
    conversationIDKey: String,
    configuration: ChatPageView.Configuration,
): Effect<ChatPageReducer.Action> =
    Effect.run { send ->
        val clientSession = DependencyValues.current.clientSession
        val conversationSession = clientSession.entity.conversation
        val conversation = clientSession.store.getConversation(conversationIDKey)
        if (conversation == null) {
            send(ChatPageReducer.Action.ConversationUnavailable)
            return@run
        }

        ChatPageViewService.setConfiguration(configuration)
        conversationSession.resetMessageOffset()
        conversationSession.setCurrentConversation(conversation)
        // Stored conversations are observed on every appearance.
        if (!conversation.isEmpty && !conversation.isMock) clientSession.sync.conversationObserver.startObserving(conversation.id.key)
        configuration.focusedMessageID?.let { conversationSession.incrementMessageOffset(it) }

        val inputBarService = DependencyValues.current.inputBarService
        inputBarService.configureInputBar(forceUpdate = true)
        inputBarService.toggleSendingUI(on = false)

        val navigationTitle = conversation.chatPageHeaderLabelText ?: ConversationCellViewData(conversation)?.titleLabelText
        navigationTitle?.let { ChatPageViewService.setNavigationTitle(it) }
    }
