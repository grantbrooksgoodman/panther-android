//
//  SettingsPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.Application.ResetCompletionProcedure
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewConstants
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.AnalyticsService.AnalyticsEvent
import us.neotechnica.panther.modules.common.services.AccountDeletionService
import us.neotechnica.panther.modules.content.user.services.CacheClearingService
import us.neotechnica.panther.modules.session.entity.services.ModerationSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.modules.content.user.services.SignOutService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.networking.user.services.UserService
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
        data object ViewAppeared : Action

        data object BackTapped : Action

        data object SignOutTapped : Action

        data object DeleteAccountTapped : Action

        data object ClearCachesTapped : Action

        data object BlockedUsersTapped : Action

        data object ChangeLanguageTapped : Action

        data object Finished : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action
    }

    // MARK: - State

    data class State(
        val isBusy: Boolean = false,
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

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(SettingsPageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    private fun signOutEffect(): Effect<Action> =
        Effect.run { send ->
            val confirmed =
                ActionSheetAlert(
                    confirmButtonTitle = "Sign Out",
                    cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
                    isDestructive = true,
                ).present(translating = listOf(ActionSheetAlert.TranslationOptionKey.Actions()))

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
                            "If you wish to continue using ⌘Hello⌘, you will need to create a new account.\n\n" +
                            "An app restart is required for this process to complete.",
                    cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
                    confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                ).present(
                    translating =
                        listOf(
                            ConfirmationAlert.TranslationOptionKey.ConfirmButtonTitle,
                            ConfirmationAlert.TranslationOptionKey.Message,
                            ConfirmationAlert.TranslationOptionKey.Title,
                        ),
                )

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
                    title = SettingsPageViewConstants.CLEAR_CACHES,
                    message = SettingsPageViewConstants.CLEAR_CACHES_CONFIRM_MESSAGE,
                    cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
                    confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                ).present(
                    translating =
                        listOf(
                            ConfirmationAlert.TranslationOptionKey.ConfirmButtonTitle,
                            ConfirmationAlert.TranslationOptionKey.Message,
                            ConfirmationAlert.TranslationOptionKey.Title,
                        ),
                )

            if (!confirmed) {
                send(Action.Finished)
                return@run
            }

            AnalyticsService.logEvent(AnalyticsEvent.CLEAR_CACHES)
            CacheClearingService.clearCaches()
            // iOS presents this with a bare `.present()`, which translates everything
            // (including the default "OK" action); opt in explicitly on Android.
            Alert(message = SettingsPageViewConstants.CLEAR_CACHES_DONE_MESSAGE).present(
                translating =
                    listOf(
                        Alert.TranslationOptionKey.Actions(),
                        Alert.TranslationOptionKey.Message,
                        Alert.TranslationOptionKey.Title,
                    ),
            )
            send(Action.Finished)
            Application.reset(
                preserveCurrentUserID = true,
                onCompletion = ResetCompletionProcedure.EXIT_GRACEFULLY,
            )
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
                Alert(message = SettingsPageViewConstants.BLOCKED_USERS_EMPTY).present(translating = listOf(Alert.TranslationOptionKey.Message))
                send(Action.Finished)
                return@run
            }

            val pairs = resolveBlockedUserPairs(blockedIDs)
            val selection = presentUnblockSheet(pairs, blockedIDs)
            if (selection == null) {
                send(Action.Finished)
                return@run
            }

            // Protect a single user's name from translation with ⌘…⌘; the
            // "All Users" label is a translatable phrase, so it is left bare.
            val name =
                if (selection.size > 1) {
                    ALL_USERS
                } else {
                    pairs.firstOrNull { it.userIDs == selection }?.displayName?.let { "⌘$it⌘" }.orEmpty()
                }
            val confirmed =
                ConfirmationAlert(
                    message = "${SettingsPageViewConstants.UNBLOCK} $name",
                    cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
                    confirmButtonTitle = SettingsPageViewConstants.UNBLOCK,
                    confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                ).present(
                    translating =
                        listOf(
                            ConfirmationAlert.TranslationOptionKey.ConfirmButtonTitle,
                            ConfirmationAlert.TranslationOptionKey.Message,
                        ),
                )

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
        // Per-user actions are display names (data); only the "Unblock All
        // Users" action and the title are translatable, mirroring iOS.
        val allUsersAction =
            AlertAction("${SettingsPageViewConstants.UNBLOCK} $ALL_USERS", style = ActionStyle.DESTRUCTIVE) {
                selection = blockedIDs
            }
        val actions =
            pairs.map { pair -> AlertAction(pair.displayName) { selection = pair.userIDs } } + allUsersAction

        val selected =
            ActionSheetAlert(
                title = "${SettingsPageViewConstants.UNBLOCK} Users",
                actions = actions,
                cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            ).present(
                translating =
                    listOf(
                        ActionSheetAlert.TranslationOptionKey.Actions(listOf(allUsersAction)),
                        ActionSheetAlert.TranslationOptionKey.Title,
                    ),
            )

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
