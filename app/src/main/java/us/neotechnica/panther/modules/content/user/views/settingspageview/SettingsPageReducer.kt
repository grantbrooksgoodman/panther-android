//
//  SettingsPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewStrings
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.services.AnalyticsService
import us.neotechnica.panther.networking.modules.common.services.AnalyticsService.AnalyticsEvent
import us.neotechnica.panther.networking.modules.session.services.AccountDeletionService
import us.neotechnica.panther.networking.modules.session.services.CacheClearingService
import us.neotechnica.panther.networking.modules.session.services.ModerationSessionService
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.networking.modules.session.services.SignOutService
import us.neotechnica.panther.networking.modules.session.services.UserSessionService
import us.neotechnica.panther.networking.modules.user.services.UserService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action as AlertAction

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

        data object ClearCachesTapped : Action

        data object BlockedUsersTapped : Action

        data object ChangeLanguageTapped : Action

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

            Action.ClearCachesTapped ->
                ReduceResult(state.copy(isBusy = true), clearCachesEffect())

            Action.BlockedUsersTapped ->
                ReduceResult(state.copy(isBusy = true), unblockUsersEffect())

            Action.ChangeLanguageTapped -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePath.ChangeLanguage)),
                )
                ReduceResult(state)
            }

            Action.Finished ->
                ReduceResult(state.copy(isBusy = false))
        }

    // MARK: - Auxiliary

    private fun signOutEffect(): Effect<Action> =
        Effect.run { send ->
            val confirmed =
                ActionSheetAlert(
                    confirmButtonTitle = "Sign Out",
                    isDestructive = true,
                ).present()

            if (!confirmed) {
                send(Action.Finished)
                return@run
            }

            AnalyticsService.logEvent(AnalyticsEvent.LOG_OUT)
            runCatching { SignOutService.signOut() }.onFailure { Logger.log(it.toException()) }
            returnToOnboarding()
            send(Action.Finished)
        }

    private fun deleteAccountEffect(): Effect<Action> =
        Effect.run { send ->
            val confirmed =
                ConfirmationAlert(
                    title = "Delete Account",
                    message =
                        "Are you sure you'd like to delete your account? All user data will be deleted.\n\n" +
                            "If you wish to continue using Hello, you will need to create a new account.\n\n" +
                            "An app restart is required for this process to complete.",
                    confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                ).present()

            if (!confirmed) {
                send(Action.Finished)
                return@run
            }

            AnalyticsService.logEvent(AnalyticsEvent.DELETE_ACCOUNT)
            Overlay.show()
            runCatching { AccountDeletionService.deleteAccount() }.onFailure { Logger.log(it.toException()) }
            Overlay.hide()
            returnToOnboarding()
            send(Action.Finished)
        }

    private fun clearCachesEffect(): Effect<Action> =
        Effect.run { send ->
            val confirmed =
                ConfirmationAlert(
                    title = SettingsPageViewStrings.CLEAR_CACHES,
                    message = SettingsPageViewStrings.CLEAR_CACHES_CONFIRM_MESSAGE,
                    confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                ).present()

            if (!confirmed) {
                send(Action.Finished)
                return@run
            }

            AnalyticsService.logEvent(AnalyticsEvent.CLEAR_CACHES)
            CacheClearingService.clearCaches()
            Alert(message = SettingsPageViewStrings.CLEAR_CACHES_DONE_MESSAGE).present()
            send(Action.Finished)
        }

    private fun unblockUsersEffect(): Effect<Action> =
        Effect.run { send ->
            val blockedIDs =
                UserSessionService.currentUser
                    ?.blockedUserIDs
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
                    .distinct()

            if (blockedIDs.isEmpty()) {
                Alert(message = SettingsPageViewStrings.BLOCKED_USERS_EMPTY).present()
                send(Action.Finished)
                return@run
            }

            val pairs = resolveBlockedUserPairs(blockedIDs)
            val selection = presentUnblockSheet(pairs, blockedIDs)
            if (selection == null) {
                send(Action.Finished)
                return@run
            }

            val name =
                if (selection.size > 1) {
                    ALL_USERS
                } else {
                    pairs.firstOrNull { it.userIDs == selection }?.displayName.orEmpty()
                }
            val confirmed =
                ConfirmationAlert(
                    message = "${SettingsPageViewStrings.UNBLOCK} $name",
                    confirmButtonTitle = SettingsPageViewStrings.UNBLOCK,
                    confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                ).present()

            if (!confirmed) {
                send(Action.Finished)
                return@run
            }

            runCatching { ModerationSessionService.unblockUsers(selection) }.onFailure { Logger.log(it.toException()) }
            send(Action.Finished)
        }

    private suspend fun resolveBlockedUserPairs(blockedIDs: List<String>): List<BlockedUserPair> {
        runCatching { ContactService.syncIfNeeded() }
        val users = runCatching { UserService.getUsers(blockedIDs) }.getOrDefault(emptyList())
        return blockedIDs
            .map { id ->
                val user = users.firstOrNull { it.id == id } ?: SessionStore.users[id]
                val displayName =
                    ContactService.match(id)?.fullName
                        ?: user?.phoneNumber?.formattedString()
                        ?: id
                BlockedUserPair(displayName, listOf(id))
            }.sortedBy { it.displayName }
    }

    private suspend fun presentUnblockSheet(
        pairs: List<BlockedUserPair>,
        blockedIDs: List<String>,
    ): List<String>? {
        var selection: List<String>? = null
        val actions =
            pairs.map { pair -> AlertAction(pair.displayName) { selection = pair.userIDs } } +
                AlertAction("${SettingsPageViewStrings.UNBLOCK} $ALL_USERS", style = ActionStyle.DESTRUCTIVE) {
                    selection = blockedIDs
                }

        val selected =
            ActionSheetAlert(
                title = "${SettingsPageViewStrings.UNBLOCK} Users",
                actions = actions,
            ).present()

        return if (selected) selection else null
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

    // MARK: - Companion

    private companion object {
        const val ALL_USERS = "All Users"
    }
}

// MARK: - Blocked User Pair

/** A blocked user's display name paired with its backing identifiers. */
private data class BlockedUserPair(
    val displayName: String,
    val userIDs: List<String>,
)
