//
//  InviteQRCodePageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.inviteqrcodepageview

import us.neotechnica.panther.bundle.navigators.SettingsRoute
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * The reducer that drives the invite QR code page.
 *
 * This page displays a QR code that others can scan to be invited to
 * the app.
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings,
 *   remaining in the loading state until resolution completes. If
 *   resolution fails, the page loads anyway.
 * - Tapping done dismisses the page.
 */
class InviteQRCodePageReducer : Reducer<InviteQRCodePageReducer.State, InviteQRCodePageReducer.Action> {
    // MARK: - Dependencies

    private val translator: HostedTranslationDelegate by Dependency { it.networking.hostedTranslation }

    // MARK: - Actions

    /** The actions the invite QR code page can process. */
    sealed interface Action {
        /** An action that indicates the view appeared. Begins display string resolution. */
        data object ViewAppeared : Action

        /** An action that indicates the user tapped the done button. Dismisses the page. */
        data object DoneButtonTapped : Action

        /** An action that indicates display string resolution succeeded, carrying the resolved strings. */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        /** An action that indicates display string resolution failed, carrying the resulting [Exception]. */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action
    }

    // MARK: - State

    /** The state of the invite QR code page. */
    data class State(
        /** The page's translated display strings. Contains the default, untranslated strings until resolution completes. */
        val strings: List<TranslationOutputMap> = InviteQRCodePageViewStrings.defaultOutputMap,
        /** The page's loading state. Remains loading until display string resolution completes. */
        val viewState: ViewState = ViewState.Loading,
    )

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared ->
                ReduceResult(state.copy(viewState = ViewState.Loading), resolveEffect())

            Action.DoneButtonTapped -> {
                DependencyValues.current.navigation.navigate(Route.Settings(SettingsRoute.Sheet(null)))
                ReduceResult(state)
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }
        }

    // MARK: - Auxiliary

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(translator.resolve(InviteQRCodePageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }
}
