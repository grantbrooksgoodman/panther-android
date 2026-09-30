//
//  SettingsPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewConstants
import us.neotechnica.panther.modules.content.user.services.SettingsPageViewService
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * The reducer that drives the settings page.
 *
 * This page presents the app's settings. It shows the user's contact
 * header and the buttons for the app's account actions – blocked users,
 * language, and account actions. Most of these actions are performed
 * through [SettingsPageViewService].
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings,
 *   remaining in the loading state until resolution completes. If
 *   resolution fails, the page falls back to its default strings and
 *   loads anyway.
 * - Tapping a button performs its action, and tapping done dismisses
 *   the page.
 */
class SettingsPageReducer : Reducer<SettingsPageReducer.State, SettingsPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object DoneToolbarButtonTapped : Action

        data object BlockedUsersButtonTapped : Action

        data object ChangeLanguageTapped : Action

        data object ClearCachesButtonTapped : Action

        data object DeleteAccountButtonTapped : Action

        data object SignOutButtonTapped : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action
    }

    // MARK: - State

    data class State(
        val strings: List<TranslationOutputMap> = SettingsPageViewStrings.defaultOutputMap,
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

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }

            Action.DoneToolbarButtonTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            Action.BlockedUsersButtonTapped -> {
                SettingsPageViewService.blockedUsersButtonTapped()
                ReduceResult(state)
            }

            Action.ChangeLanguageTapped -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePath.ChangeLanguage)),
                )
                ReduceResult(state)
            }

            Action.ClearCachesButtonTapped -> {
                SettingsPageViewService.clearCachesButtonTapped()
                ReduceResult(state)
            }

            Action.DeleteAccountButtonTapped -> {
                SettingsPageViewService.deleteAccountButtonTapped()
                ReduceResult(state)
            }

            Action.SignOutButtonTapped -> {
                SettingsPageViewService.signOutButtonTapped()
                ReduceResult(state)
            }
        }

    // MARK: - Auxiliary

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(SettingsPageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }
}

// MARK: - Strings

/** The translated label strings for the settings page. */
object SettingsPageViewStrings : TranslatedLabelStrings {
    val blockedUsers = TranslatedLabelStringCollection("settingsPageView.blockedUsers")
    val changeLanguage = TranslatedLabelStringCollection("settingsPageView.changeLanguage")
    val clearCaches = TranslatedLabelStringCollection("settingsPageView.clearCaches")
    val deleteAccount = TranslatedLabelStringCollection("settingsPageView.deleteAccount")
    val inviteFriends = TranslatedLabelStringCollection("settingsPageView.inviteFriends")
    val leaveReview = TranslatedLabelStringCollection("settingsPageView.leaveReview")
    val signOut = TranslatedLabelStringCollection("settingsPageView.signOut")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(blockedUsers, TranslationInput(SettingsPageViewConstants.BLOCKED_USERS)),
            TranslationInputMap(changeLanguage, TranslationInput(SettingsPageViewConstants.CHANGE_LANGUAGE)),
            TranslationInputMap(clearCaches, TranslationInput(SettingsPageViewConstants.CLEAR_CACHES)),
            TranslationInputMap(deleteAccount, TranslationInput(SettingsPageViewConstants.DELETE_ACCOUNT)),
            TranslationInputMap(inviteFriends, TranslationInput(SettingsPageViewConstants.INVITE_FRIENDS)),
            TranslationInputMap(leaveReview, TranslationInput(SettingsPageViewConstants.LEAVE_REVIEW)),
            TranslationInputMap(signOut, TranslationInput(SettingsPageViewConstants.SIGN_OUT)),
        )
}
