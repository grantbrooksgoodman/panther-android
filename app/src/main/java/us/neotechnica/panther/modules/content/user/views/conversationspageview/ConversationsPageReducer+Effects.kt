//
//  ConversationsPageReducer+Effects.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewFloats
import us.neotechnica.panther.modules.content.user.dependencies.conversationsPageViewService
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// MARK: - Effects

internal fun animationAmountEffect(currentAnimationAmount: Float): Effect<ConversationsPageReducer.Action> =
    Effect.task(delay = ANIMATION_AMOUNT_DELAY) {
        ConversationsPageReducer.Action.ComposeToolbarButtonAnimationAmountSet(
            if (currentAnimationAmount == ConversationsPageViewFloats.COMPOSE_TOOLBAR_BUTTON_ANIMATION_AMOUNT) {
                1f
            } else {
                ConversationsPageViewFloats.COMPOSE_TOOLBAR_BUTTON_ANIMATION_AMOUNT
            },
        )
    }

internal fun reloadDataEffect(): Effect<ConversationsPageReducer.Action> =
    Effect.run { send ->
        try {
            DependencyValues.current.conversationsPageViewService.reloadData()
            send(ConversationsPageReducer.Action.ReloadDataReturned)
        } catch (exception: Exception) {
            send(ConversationsPageReducer.Action.ReloadDataFailed(exception))
        }
    }

internal fun handleChatPageStoreChangeTask(): Effect<ConversationsPageReducer.Action> =
    Effect
        .task<ConversationsPageReducer.Action>(delay = HANDLE_CHAT_PAGE_STORE_CHANGE_DELAY) {
            ConversationsPageReducer.Action.HandleChatPageStoreChange
        }.cancellable(TASK_ID_HANDLE_CHAT_PAGE_STORE_CHANGE, cancelInFlight = true)

internal fun resolveEffect(): Effect<ConversationsPageReducer.Action> =
    Effect.run { send ->
        try {
            send(
                ConversationsPageReducer.Action.ResolveReturned(
                    DependencyValues.current.networking.hostedTranslation
                        .resolve(ConversationsPageViewStrings),
                ),
            )
        } catch (exception: Exception) {
            send(ConversationsPageReducer.Action.ResolveFailed(exception))
        }
    }

// MARK: - Constants

private val ANIMATION_AMOUNT_DELAY = 1.seconds
private val HANDLE_CHAT_PAGE_STORE_CHANGE_DELAY = 250.milliseconds
private const val TASK_ID_HANDLE_CHAT_PAGE_STORE_CHANGE = "ConversationsPageReducer/handleChatPageStoreChange"
