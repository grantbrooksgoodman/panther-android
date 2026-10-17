//
//  SettingsPageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

@file:Suppress("FileLength")

package us.neotechnica.panther.modules.content.user.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.Application
import us.neotechnica.panther.Application.ResetCompletionProcedure
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.bundle.application
import us.neotechnica.panther.bundle.navigators.OnboardingRoute
import us.neotechnica.panther.bundle.navigators.SettingsNavigatorState
import us.neotechnica.panther.bundle.navigators.SettingsRoute
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.designsystem.modules.developermode.services.DevModeService
import us.neotechnica.panther.designsystem.modules.foundation.components.ListRowView
import us.neotechnica.panther.designsystem.modules.foundation.dependencies.reportDelegate
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.modules.common.extensions.ApplicationStorageKey
import us.neotechnica.panther.modules.common.extensions.toException
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.AccountDeletionService
import us.neotechnica.panther.modules.common.services.sources.AnalyticsService.AnalyticsEvent
import us.neotechnica.panther.modules.common.services.sources.HapticsService
import us.neotechnica.panther.modules.common.services.sources.InviteService
import us.neotechnica.panther.modules.common.services.sources.MetadataService
import us.neotechnica.panther.modules.content.user.components.SquareIconImage
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewColors
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewFloats
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.removeCurrentPushToken
import us.neotechnica.panther.modules.content.user.models.SquareIconViewConfiguration
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.ModerationSessionService
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.extensions.languageExonym
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.overriddenLanguageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Constants Accessors

private typealias SettingsColors = SettingsPageViewColors
private typealias SettingsFloats = SettingsPageViewFloats
private typealias SettingsStrings = SettingsPageViewConstants

// The counterpart service carries `// swiftlint:disable file_length type_body_length`.

/**
 * The service that handles the settings page's user interactions.
 *
 * Use [SettingsPageViewService] to respond to the settings page's
 * controls – account actions and support options – most of which
 * confirm through alerts before taking effect.
 *
 * Resolve the service through
 * [DependencyValues.settingsPageViewService][us.neotechnica.panther.modules.content.user.dependencies.settingsPageViewService]
 * and call [initialize] once with the application context before the
 * intent-launching handlers.
 *
 * @param dependencies The collaborators the service resolves its work
 *   through. The default resolves the live services.
 */
@Suppress("LargeClass")
class SettingsPageViewService(
    private val dependencies: Dependencies = Dependencies(),
) {
    // MARK: - Dependencies

    private val accountDeletionService: AccountDeletionService by Dependency { it.commonServices.accountDeletion }
    private val hapticsService: HapticsService by Dependency { it.commonServices.haptics }
    private val inviteService: InviteService by Dependency { it.commonServices.invite }
    private val metadataService: MetadataService by Dependency { it.commonServices.metadata }
    private val moderationSession: ModerationSessionService by Dependency { it.clientSession.entity.moderation }

    // MARK: - Types

    /**
     * The collaborators the service resolves its work through.
     *
     * Each property defaults to the live service; a test replaces the
     * ones it exercises.
     *
     * @property currentUser Returns the current user.
     * @property delayed Runs the given operation after the given
     *   delay.
     * @property dismissSheets Dismisses every presented sheet.
     * @property isDeveloperModeEnabled Returns whether developer mode
     *   is enabled.
     * @property logEvent Logs the given analytics event.
     * @property milestone Returns the running build's milestone.
     * @property navigate Navigates to the given route.
     * @property overriddenLanguageCode Returns the overriding language
     *   code, or `null` if none is set.
     * @property promptToToggleDeveloperMode Prompts to toggle
     *   developer mode.
     * @property removeCurrentPushToken Removes the device's push token
     *   from the given user.
     * @property reset Resets the app.
     * @property setBadgeNumber Sets the app's badge number.
     * @property stopObservingCurrentUserChanges Stops observing
     *   changes to the current user.
     */
    data class Dependencies(
        val currentUser: () -> User? = { DependencyValues.current.clientSession.entity.user.currentUser },
        val delayed: (Duration, suspend () -> Unit) -> Unit = { by, operation -> Task.delayed(by, operation) },
        val dismissSheets: () -> Unit = { Application.dismissSheets() },
        val isDeveloperModeEnabled: () -> Boolean = { Build.isDeveloperModeEnabled },
        val logEvent: (AnalyticsEvent) -> Unit = {
            DependencyValues.current.commonServices.analytics
                .logEvent(it)
        },
        val milestone: () -> Build.Milestone = { Build.milestone },
        val navigate: (Route) -> Unit = { DependencyValues.current.navigation.navigate(it) },
        val overriddenLanguageCode: () -> String? = { RuntimeStorage.retrieve(StoredItemKey.overriddenLanguageCode) as? String },
        val promptToToggleDeveloperMode: () -> Unit = { DevModeService.promptToToggle() },
        val removeCurrentPushToken: suspend (User) -> Unit = { it.removeCurrentPushToken() },
        val reset: () -> Unit = { Application.reset() },
        val setBadgeNumber: suspend (
            Int,
        ) -> Unit = {
            DependencyValues.current.commonServices.notification
                .setBadgeNumber(it, updateHostedValue = false)
        },
        val stopObservingCurrentUserChanges: () -> Unit = {
            DependencyValues.current.clientSession.entity.user
                .stopObservingCurrentUserChanges()
        },
    )

    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var appContext: Context? = null
    private var cachedCNContactForCurrentUser: ContactPair? = null

    /** A Boolean value that indicates whether the main settings page is presented. */
    var isMainPagePresented = true

    // MARK: - Init

    /**
     * Prepares the service with the application context.
     *
     * @param context The context whose application context the service
     *   keeps.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Reducer Action Handlers

    /** Begins the unblock users flow, surfacing any error as a toast. */
    fun blockedUsersButtonTapped() {
        scope.launch {
            runCatching { moderationSession.unblockUsers() }.onFailure { Logger.log(it.toException(), with = AlertType.toast) }
        }
    }

    /**
     * Asks the user to confirm clearing all caches, applying it if they
     * accept.
     *
     * Clearing resets the app – preserving the current user's identifier
     * – and logs an analytics event. The app must then restart; in
     * developer mode, an in-place reload is offered instead.
     */
    fun clearCachesButtonTapped() {
        scope.launch {
            val confirmMessage =
                buildString {
                    append("Are you sure you'd like to clear all caches?\n\n")
                    append("This may fix some issues, but can also temporarily slow down the app while indexes rebuild.")
                    if (!dependencies.isDeveloperModeEnabled()) append("\n\nYou will need to restart the app for this to take effect.")
                }

            val confirmed =
                ConfirmationAlert(
                    title = SettingsStrings.CLEAR_CACHES,
                    message = confirmMessage,
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

            if (!confirmed) return@launch
            clearCaches()
        }
    }

    /**
     * Asks the user to confirm account deletion – twice – before
     * deleting their account.
     *
     * After deletion, the badge number clears, an analytics event logs,
     * and the app resets and exits.
     */
    fun deleteAccountButtonTapped() {
        scope.launch {
            val message =
                "Are you sure you'd like to delete your account? All user data will be deleted.\n\n" +
                    "If you wish to continue using ⌘${Build.finalName}⌘, you will need to create a new account.\n\n" +
                    "An app restart is required for this process to complete."

            val confirmed =
                ConfirmationAlert(
                    title = SettingsStrings.DELETE_ACCOUNT,
                    message = message,
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

            if (!confirmed) return@launch
            presentDeleteAccountActionSheet()
        }
    }

    /**
     * Presents an action sheet for inviting friends, offering to share
     * the invitation to another app or to show the invite QR code.
     */
    fun inviteFriendsButtonTapped() {
        scope.launch {
            val shareToOtherAppAction =
                Action(SettingsStrings.SHARE_TO_ANOTHER_APP) {
                    scope.launch {
                        runCatching { inviteService.presentInvitationPrompt() }
                            .onFailure { Logger.log(it.toException(), with = AlertType.toast) }
                    }
                }
            val showQRCodeAction =
                Action(SettingsStrings.SHOW_QR_CODE) {
                    dependencies.navigate(Route.Settings(SettingsRoute.Sheet(SettingsNavigatorState.SheetPaths.InviteQRCode)))
                }

            ActionSheet(
                title = SettingsStrings.INVITE_FRIENDS,
                actions = listOf(shareToOtherAppAction, showQRCodeAction),
                cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            ).present(
                translating =
                    listOf(
                        ActionSheet.TranslationOptionKey.Actions(),
                        ActionSheet.TranslationOptionKey.Title,
                    ),
            )
        }
    }

    /**
     * Opens the app's Play Store listing.
     *
     * If the Play Store share link has not been resolved, this method
     * does nothing.
     */
    fun leaveReviewButtonTapped() {
        val shareLink = metadataService.playStoreShareLink ?: return
        openURL(shareLink)
    }

    /**
     * Toggles prerelease mode after verification.
     *
     * On general-release builds, entering the correct passphrase
     * switches the build milestone to beta; on prerelease builds,
     * confirmation clears the override and disables developer mode.
     * Either change exits the app, which must restart for the change to
     * take effect.
     */
    fun promptToEnterPrereleaseMode() {
        scope.launch {
            val buildMilestoneKey = PersistentStorageKey.application(ApplicationStorageKey.BUILD_MILESTONE_STRING)
            if (dependencies.milestone() != Build.Milestone.GENERAL_RELEASE) {
                val confirmed =
                    ConfirmationAlert(
                        title = ENTER_PRERELEASE_MODE_EXIT_TITLE,
                        message = "Are you sure you'd like to exit Prerelease Mode? An app restart is required for this to take effect.",
                        confirmButtonTitle = "Apply & Exit",
                        confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                    ).present(translating = emptyList())

                if (!confirmed) return@launch
                Persistent.setString(buildMilestoneKey, null)
                Build.setIsDeveloperModeEnabled(false)
                exitProcess(0)
            }

            val input =
                TextInputAlert(
                    title = ENTER_PRERELEASE_MODE_TITLE,
                    message = "Enter the correct passphrase to continue.",
                    attributes =
                        TextFieldAttributes(
                            isSecureTextEntry = true,
                            keyboardType = KeyboardType.NumberPassword,
                            placeholderText = "••••••",
                        ),
                    confirmButtonTitle = "Done",
                ).present(translating = emptyList())

            if (input == null) return@launch
            if (input != Build.expirationOverrideCode.reversed()) {
                Alert(
                    title = ENTER_PRERELEASE_MODE_TITLE,
                    message = "The passphrase entered was incorrect. Please try again.",
                    actions =
                        listOf(
                            Action("Try Again", style = ActionStyle.PREFERRED) { promptToEnterPrereleaseMode() },
                            Action("Cancel", style = ActionStyle.CANCEL) {},
                        ),
                ).present(translating = emptyList())
                return@launch
            }

            Persistent.setString(buildMilestoneKey, Build.Milestone.BETA.rawValue)
            val exitAction = Action(SettingsStrings.EXIT, style = ActionStyle.DESTRUCTIVE_PREFERRED) { exitProcess(0) }
            Alert(
                message = "Successfully entered Prerelease Mode. You must now restart the app.",
                actions = listOf(exitAction),
            ).present(translating = emptyList())
        }
    }

    /**
     * Presents an action sheet for filing a report, offering to send
     * feedback or report a bug.
     */
    fun sendFeedbackButtonTapped() {
        scope.launch {
            val reportBugAction = Action(SettingsStrings.REPORT_BUG) { DependencyValues.current.reportDelegate.reportBug() }
            ActionSheet(
                title = SettingsStrings.FILE_A_REPORT,
                actions =
                    listOf(
                        Action(LocalizedStringKey.SendFeedback.localized()) { DependencyValues.current.reportDelegate.sendFeedback() },
                        reportBugAction,
                    ),
                cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            ).present(
                translating =
                    listOf(
                        ActionSheet.TranslationOptionKey.Actions(listOf(reportBugAction)),
                        ActionSheet.TranslationOptionKey.Title,
                    ),
            )
        }
    }

    /**
     * Copies the given string to the clipboard, playing heavy haptic
     * feedback.
     *
     * @param string The string to copy.
     */
    fun setClipboardWithHapticFeedback(string: String) {
        val context = appContext ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(CLIPBOARD_LABEL, string))
        hapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.HEAVY)
    }

    /**
     * Asks the user to confirm signing out, applying it if they accept.
     *
     * Signing out clears the badge number, removes the device's push
     * token from the user's record, resets the app, logs an analytics
     * event, and returns to onboarding.
     */
    fun signOutButtonTapped() {
        scope.launch {
            val signOutAction =
                Action(
                    "Sign Out",
                    style = ActionStyle.DESTRUCTIVE_PREFERRED,
                ) { scope.launch { signOut() } }

            ActionSheet(
                actions = listOf(signOutAction),
                cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            ).present(translating = listOf(ActionSheet.TranslationOptionKey.Actions()))
        }
    }

    // MARK: - Developer Mode List Items

    /**
     * Returns the developer mode list rows for the settings page.
     *
     * The rows offer toggling developer mode and, in developer mode –
     * for users whose language is not English – overriding the app's
     * language code to English.
     *
     * @return The list row configurations; otherwise, `null` on
     *   general-release builds.
     */
    fun developerModeListItems(): List<ListRowView.Configuration>? {
        if (dependencies.milestone() == Build.Milestone.GENERAL_RELEASE) return null

        val items = mutableListOf<ListRowView.Configuration>()
        val currentUser = dependencies.currentUser()
        if (dependencies.isDeveloperModeEnabled() && currentUser != null && currentUser.languageCode != "en") {
            val languageName = currentUser.languageCode.languageExonym ?: currentUser.languageCode.uppercase()
            val restoreLanguageCodeString = "${SettingsStrings.RESTORE_LANGUAGE_CODE_BUTTON_TEXT_PREFIX} $languageName"
            val overrideOrRestore =
                if (dependencies.overriddenLanguageCode() == null) {
                    SettingsStrings.OVERRIDE_LANGUAGE_CODE_BUTTON_TEXT
                } else {
                    restoreLanguageCodeString
                }

            items.add(
                ListRowView.Configuration(
                    ListRowView.Interaction.Button { overrideLanguageCodeButtonTapped() },
                    innerText = overrideOrRestore,
                    imageView = {
                        SquareIconImage(
                            SquareIconViewConfiguration.symbol(
                                SettingsColors.overrideLanguageCodeButtonImageBackground,
                                SettingsStrings.OVERRIDE_LANGUAGE_CODE_BUTTON_IMAGE_SYSTEM_NAME,
                            ),
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                ),
            )
        }

        if (!dependencies.isDeveloperModeEnabled()) {
            items.add(
                ListRowView.Configuration(
                    ListRowView.Interaction.Button { dependencies.promptToToggleDeveloperMode() },
                    innerText = SettingsStrings.TOGGLE_DEVELOPER_MODE_BUTTON_TEXT,
                    imageView = {
                        SquareIconImage(
                            SquareIconViewConfiguration.symbol(
                                SettingsColors.toggleDeveloperModeButtonImageBackground,
                                SettingsStrings.TOGGLE_DEVELOPER_MODE_BUTTON_IMAGE_SYSTEM_NAME,
                                SettingsFloats.TOGGLE_DEVELOPER_MODE_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE,
                                FontWeight.Bold,
                            ),
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                ),
            )
        }

        return items
    }

    // MARK: - Fetch CNContact for Current User

    /**
     * Returns the device contact matching the current user's phone
     * number.
     *
     * Results are cached in memory.
     *
     * @return The matching device contact.
     *
     * @throws Exception if the current user has not been set, or if no
     *   matching contact can be resolved.
     */
    fun fetchCNContactForCurrentUser(): ContactPair {
        cachedCNContactForCurrentUser?.let { return it }

        val currentUser =
            dependencies.currentUser()
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))
        val contactPair =
            currentUser.contactPair
                ?: throw Exception("No matching contact could be resolved.", metadata = ExceptionMetadata(this))

        cachedCNContactForCurrentUser = contactPair
        return contactPair
    }

    // MARK: - Clear Cache

    /** Removes the cached device contact for the current user. */
    fun clearCache() {
        cachedCNContactForCurrentUser = null
    }

    // MARK: - Auxiliary

    private fun overrideLanguageCodeButtonTapped() {
        if (dependencies.overriddenLanguageCode() != null) {
            val currentUser = dependencies.currentUser() ?: return
            val languageName = currentUser.languageCode.languageExonym ?: currentUser.languageCode.uppercase()

            RuntimeStorage.remove(StoredItemKey.overriddenLanguageCode)
            HUD.showSuccess(text = "Set to $languageName")
            dependencies.dismissSheets()
            return
        }

        RuntimeStorage.store("en", StoredItemKey.overriddenLanguageCode)
        HUD.showSuccess(text = "Set to English")
        dependencies.dismissSheets()
    }

    private suspend fun clearCaches() {
        dependencies.stopObservingCurrentUserChanges()
        dependencies.logEvent(AnalyticsEvent.CLEAR_CACHES)
        Application.reset(preserveCurrentUserID = true)

        val isDeveloperModeEnabled = dependencies.isDeveloperModeEnabled()
        val doneMessage = "Caches have been cleared. ${if (isDeveloperModeEnabled) "" else "You must now restart the app."}"
        val exitAction = Action(SettingsStrings.EXIT, style = ActionStyle.DESTRUCTIVE_PREFERRED) { exitGracefully() }
        val actions =
            if (isDeveloperModeEnabled) {
                listOf(Action(SettingsStrings.RELOAD) { reload() }, exitAction)
            } else {
                listOf(exitAction)
            }

        Alert(message = doneMessage, actions = actions).present(
            translating =
                listOf(
                    Alert.TranslationOptionKey.Actions(),
                    Alert.TranslationOptionKey.Message,
                ),
        )
    }

    private suspend fun presentDeleteAccountActionSheet() {
        val deleteAccountAction =
            Action(SettingsStrings.DELETE_ACCOUNT, style = ActionStyle.DESTRUCTIVE_PREFERRED) {
                scope.launch {
                    runCatching { accountDeletionService.deleteAccount() }.onFailure { Logger.log(it.toException()) }
                    val exitAction = Action(SettingsStrings.EXIT, style = ActionStyle.DESTRUCTIVE_PREFERRED) { clearCachesAndExit() }
                    Alert(message = SettingsStrings.ACCOUNT_DELETED_MESSAGE, actions = listOf(exitAction)).present(
                        translating =
                            listOf(
                                Alert.TranslationOptionKey.Actions(),
                                Alert.TranslationOptionKey.Message,
                            ),
                    )
                }
            }

        ActionSheet(
            actions = listOf(deleteAccountAction),
            cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
        ).present(translating = listOf(ActionSheet.TranslationOptionKey.Actions()))
    }

    private fun clearCachesAndExit() {
        scope.launch {
            runCatching { dependencies.setBadgeNumber(0) }.onFailure { Logger.log(it.toException()) }

            dependencies.logEvent(AnalyticsEvent.DELETE_ACCOUNT)
            Application.reset(
                preserveCurrentUserID = false,
                onCompletion = ResetCompletionProcedure.EXIT_GRACEFULLY,
            )
        }
    }

    // Signs the user out: the badge clears, the push token leaves the
    // user record, and – whether or not a user was set – the app
    // resets, the event logs, and onboarding returns after a delay.
    internal suspend fun signOut() {
        runCatching { dependencies.setBadgeNumber(0) }.onFailure { Logger.log(it.toException()) }

        try {
            val currentUser = dependencies.currentUser() ?: return
            dependencies.stopObservingCurrentUserChanges()
            runCatching { dependencies.removeCurrentPushToken(currentUser) }.onFailure { Logger.log(it.toException()) }
        } finally {
            dependencies.dismissSheets()
            dependencies.reset()
            dependencies.logEvent(AnalyticsEvent.LOG_OUT)
            dependencies.delayed(SettingsFloats.SIGN_OUT_NAVIGATION_DELAY_MILLISECONDS.milliseconds) { returnToOnboarding() }
        }
    }

    private fun exitGracefully() {
        dependencies.dismissSheets()
        Application.beginGracefulExit()
    }

    private fun reload() {
        dependencies.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
        dependencies.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Splash)))
    }

    private fun returnToOnboarding() {
        dependencies.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
        dependencies.navigate(Route.Onboarding(OnboardingRoute.Stack(emptyList())))
        dependencies.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Onboarding)))
    }

    private fun openURL(url: String) {
        val context = appContext ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { context.startActivity(intent) }
    }

    // MARK: - Companion

    private companion object {
        const val CLIPBOARD_LABEL = "Build Info"
        const val ENTER_PRERELEASE_MODE_EXIT_TITLE = "Exit Prerelease Mode"
        const val ENTER_PRERELEASE_MODE_TITLE = "Enter Prerelease Mode"
    }
}
