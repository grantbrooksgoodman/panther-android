//
//  SettingsPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.session.services.AccountDeletionService
import us.neotechnica.panther.networking.modules.session.services.SignOutService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * The reducer for the settings page.
 *
 * Hosts sign-out and account-deletion, each of which clears the session
 * and returns to onboarding.
 */
class SettingsPageReducer : Reducer<SettingsPageReducer.State, SettingsPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object BackTapped : Action

        data object SignOutTapped : Action

        data object DeleteAccountTapped : Action

        data object Finished : Action
    }

    // MARK: - State

    data class State(
        val isBusy: Boolean = false,
    )

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.BackTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            Action.SignOutTapped ->
                ReduceResult(state.copy(isBusy = true), signOutEffect())

            Action.DeleteAccountTapped ->
                ReduceResult(state.copy(isBusy = true), deleteAccountEffect())

            Action.Finished ->
                ReduceResult(state.copy(isBusy = false))
        }

    // MARK: - Auxiliary

    private fun signOutEffect(): Effect<Action> =
        Effect.run { send ->
            runCatching { SignOutService.signOut() }.onFailure { Logger.log(it.toException()) }
            returnToOnboarding()
            send(Action.Finished)
        }

    private fun deleteAccountEffect(): Effect<Action> =
        Effect.run { send ->
            val confirmed =
                ConfirmationAlert(
                    title = "Delete Account",
                    message = "This permanently deletes your account and cannot be undone.",
                    confirmButtonTitle = "Delete",
                    confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                ).present()

            if (!confirmed) {
                send(Action.Finished)
                return@run
            }

            Overlay.show()
            runCatching { AccountDeletionService.deleteAccount() }.onFailure { Logger.log(it.toException()) }
            Overlay.hide()
            returnToOnboarding()
            send(Action.Finished)
        }

    private fun returnToOnboarding() {
        val navigation = DependencyValues.current.navigation
        navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
        navigation.navigate(Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Onboarding)))
    }

    private fun Throwable.toException(): Exception =
        this as? Exception
            ?: Exception.from(
                this,
                us.neotechnica.panther.subsystem.modules.foundation.models
                    .ExceptionMetadata(this),
            )
}
