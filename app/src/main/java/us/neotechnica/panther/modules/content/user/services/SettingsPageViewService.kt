//
//  SettingsPageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.Application.ResetCompletionProcedure
import us.neotechnica.panther.bundle.application
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.modules.common.extensions.ApplicationStorageKey
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.services.AccountDeletionService
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.AnalyticsService.AnalyticsEvent
import us.neotechnica.panther.modules.common.services.DevModeService
import us.neotechnica.panther.modules.common.services.HapticsService
import us.neotechnica.panther.modules.common.services.InviteService
import us.neotechnica.panther.modules.common.services.MetadataService
import us.neotechnica.panther.modules.common.services.NotificationService
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewColors
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.SquareIconViewFloats
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.removeCurrentPushToken
import us.neotechnica.panther.modules.content.user.models.SquareIconViewConfiguration
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.modules.session.entity.services.ModerationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.SettingsNavigatorState
import us.neotechnica.panther.navigation.SettingsRoute
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.overriddenLanguageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
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
 * controls – feature switches, account actions, and support options –
 * most of which confirm through alerts before taking effect.
 *
 * [initialize] must be called once with the application context before
 * the intent-launching handlers.
 */
@Suppress("LargeClass")
object SettingsPageViewService {
    // MARK: - Properties

    /** A Boolean value that indicates whether the main settings page is presented. */
    var isMainPagePresented = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var appContext: Context? = null
    private var cachedCNContactForCurrentUser: ContactPair? = null

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Reducer Action Handlers

    /** Begins the unblock users flow, surfacing any error as a toast. */
    fun blockedUsersButtonTapped() {
        scope.launch {
            runCatching { ModerationSessionService.unblockUsers() }.onFailure { Logger.log(it.toException(), with = AlertType.toast) }
        }
    }

    /**
     * Asks the user to confirm clearing all caches, applying it if they
     * accept.
     *
     * Clearing resets the app – preserving the current user's identifier.
     * The app must then restart; in developer mode, an in-place reload is
     * offered instead.
     */
    fun clearCachesButtonTapped() {
        scope.launch {
            val confirmMessage =
                buildString {
                    append("Are you sure you'd like to clear all caches?\n\n")
                    append("This may fix some issues, but can also temporarily slow down the app while indexes rebuild.")
                    if (!Build.isDeveloperModeEnabled) append("\n\nYou will need to restart the app for this to take effect.")
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
                        runCatching { InviteService.presentInvitationPrompt() }
                            .onFailure { Logger.log(it.toException(), with = AlertType.toast) }
                    }
                }
            val showQRCodeAction =
                Action(SettingsStrings.SHOW_QR_CODE) {
                    DependencyValues.current.navigation.navigate(
                        Route.Settings(SettingsRoute.Sheet(SettingsNavigatorState.SheetPath.InviteQRCode)),
                    )
                }

            ActionSheetAlert(
                title = SettingsStrings.INVITE_FRIENDS,
                actions = listOf(shareToOtherAppAction, showQRCodeAction),
                cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            ).present(
                translating =
                    listOf(
                        ActionSheetAlert.TranslationOptionKey.Actions(),
                        ActionSheetAlert.TranslationOptionKey.Title,
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
        val shareLink = MetadataService.playStoreShareLink ?: return
        openURL(shareLink)
    }

    /**
     * Toggles prerelease mode after verification.
     *
     * On general-release builds, entering the correct passphrase
     * switches the build milestone to beta; on prerelease builds,
     * confirmation clears the override. Either change exits the app,
     * which must restart for the change to take effect.
     */
    fun promptToEnterPrereleaseMode() {
        scope.launch {
            val buildMilestoneKey = PersistentStorageKey.application(ApplicationStorageKey.BUILD_MILESTONE_STRING)
            if (Build.milestone != Milestone.GENERAL_RELEASE) {
                val confirmed =
                    ConfirmationAlert(
                        title = ENTER_PRERELEASE_MODE_EXIT_TITLE,
                        message = "Are you sure you'd like to exit Prerelease Mode? An app restart is required for this to take effect.",
                        confirmButtonTitle = "Apply & Exit",
                        confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                    ).present(translating = emptyList())

                if (!confirmed) return@launch
                Persistent.setString(buildMilestoneKey, null)
                Application.beginGracefulExit()
                return@launch
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
                    confirmButtonTitle = LocalizedStringKey.Done.localized(),
                ).present(translating = emptyList())

            if (input == null) return@launch
            if (input != Build.expirationOverrideCode.reversed()) {
                Alert(
                    title = ENTER_PRERELEASE_MODE_TITLE,
                    message = "The passphrase entered was incorrect. Please try again.",
                    actions =
                        listOf(
                            Action("Try Again", style = ActionStyle.PREFERRED) { promptToEnterPrereleaseMode() },
                            Action(LocalizedStringKey.Cancel.localized(), style = ActionStyle.CANCEL) {},
                        ),
                ).present(translating = emptyList())
                return@launch
            }

            Persistent.setString(buildMilestoneKey, Milestone.BETA.rawValue)
            val exitAction =
                Action(SettingsStrings.EXIT, style = ActionStyle.DESTRUCTIVE_PREFERRED) { Application.beginGracefulExit() }
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
            val reportBugAction = Action(SettingsStrings.REPORT_BUG) { AlertKitConfig.reportDelegate?.reportBug() }
            ActionSheetAlert(
                title = SettingsStrings.FILE_A_REPORT,
                actions =
                    listOf(
                        Action(LocalizedStringKey.SendFeedback.localized()) { AlertKitConfig.reportDelegate?.sendFeedback() },
                        reportBugAction,
                    ),
                cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            ).present(
                translating =
                    listOf(
                        ActionSheetAlert.TranslationOptionKey.Actions(listOf(reportBugAction)),
                        ActionSheetAlert.TranslationOptionKey.Title,
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
        HapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.HEAVY)
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
            val confirmed =
                ActionSheetAlert(
                    confirmButtonTitle = SettingsStrings.SIGN_OUT,
                    cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
                    isDestructive = true,
                ).present(translating = listOf(ActionSheetAlert.TranslationOptionKey.Actions()))

            if (!confirmed) return@launch
            performSignOut()
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
    fun developerModeListItems(): List<DeveloperModeListItem>? {
        if (Build.milestone == Milestone.GENERAL_RELEASE) return null

        val items = mutableListOf<DeveloperModeListItem>()
        val currentUser = UserSessionService.currentUser
        if (Build.isDeveloperModeEnabled && currentUser != null && currentUser.languageCode != "en") {
            val languageName =
                LocalizedStringResolver.languageDisplayNames()[currentUser.languageCode] ?: currentUser.languageCode.uppercase()
            val restoreLanguageCodeString = "${SettingsStrings.RESTORE_LANGUAGE_CODE_BUTTON_TEXT_PREFIX} $languageName"
            val overrideOrRestore =
                if (RuntimeStorage.retrieve(StoredItemKey.overriddenLanguageCode) == null) {
                    SettingsStrings.OVERRIDE_LANGUAGE_CODE_BUTTON_TEXT
                } else {
                    restoreLanguageCodeString
                }

            items.add(
                DeveloperModeListItem(
                    title = overrideOrRestore,
                    configuration =
                        squareIconConfiguration(
                            SettingsColors.overrideLanguageCodeButtonImageBackground,
                            SettingsStrings.OVERRIDE_LANGUAGE_CODE_BUTTON_IMAGE_SYSTEM_NAME,
                        ),
                    action = ::overrideLanguageCodeButtonTapped,
                ),
            )
        }

        if (!Build.isDeveloperModeEnabled) {
            items.add(
                DeveloperModeListItem(
                    title = SettingsStrings.TOGGLE_DEVELOPER_MODE,
                    configuration =
                        squareIconConfiguration(
                            SettingsColors.toggleDeveloperModeButtonImageBackground,
                            SettingsStrings.TOGGLE_DEVELOPER_MODE_BUTTON_IMAGE_SYSTEM_NAME,
                            SettingsFloats.TOGGLE_DEVELOPER_MODE_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE,
                        ),
                    action = { DevModeService.promptToToggle() },
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
            UserSessionService.currentUser
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
        if (RuntimeStorage.retrieve(StoredItemKey.overriddenLanguageCode) != null) {
            val currentUser = UserSessionService.currentUser ?: return
            val languageName =
                LocalizedStringResolver.languageDisplayNames()[currentUser.languageCode] ?: currentUser.languageCode.uppercase()

            AlertKitConfig.overrideTargetLanguageCode(currentUser.languageCode)
            RuntimeStorage.remove(StoredItemKey.overriddenLanguageCode)
            HUD.showSuccess(text = "Set to $languageName")
            Application.dismissSheets()
            return
        }

        AlertKitConfig.overrideTargetLanguageCode("en")
        RuntimeStorage.store("en", StoredItemKey.overriddenLanguageCode)
        HUD.showSuccess(text = "Set to English")
        Application.dismissSheets()
    }

    private fun squareIconConfiguration(
        backgroundColor: Color,
        systemName: String,
        framePercentOfTotalSize: Float = SquareIconViewFloats.OVERLAY_FRAME_HEIGHT_MULTIPLIER,
    ): SquareIconViewConfiguration =
        SquareIconViewConfiguration(
            size = DpSize(SettingsFloats.iconSize, SettingsFloats.iconSize),
            backgroundColor = backgroundColor,
            overlay =
                SquareIconViewConfiguration.OverlayConfiguration.Symbol(
                    name = systemName,
                    framePercentOfTotalSize = framePercentOfTotalSize,
                ),
        )

    private suspend fun clearCaches() {
        UserSessionService.stopObservingCurrentUserChanges()
        AnalyticsService.logEvent(AnalyticsEvent.CLEAR_CACHES)
        Application.reset(preserveCurrentUserID = true)

        val doneMessage =
            "Caches have been cleared. ${if (Build.isDeveloperModeEnabled) "" else "You must now restart the app."}"
        val exitAction = Action(SettingsStrings.EXIT, style = ActionStyle.DESTRUCTIVE_PREFERRED) { exitGracefully() }
        val actions =
            if (Build.isDeveloperModeEnabled) {
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
                    runCatching { AccountDeletionService.deleteAccount() }.onFailure { Logger.log(it.toException()) }
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

        ActionSheetAlert(
            actions = listOf(deleteAccountAction),
            cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
        ).present(translating = listOf(ActionSheetAlert.TranslationOptionKey.Actions()))
    }

    private fun clearCachesAndExit() {
        scope.launch {
            runCatching { NotificationService.setBadgeNumber(0, updateHostedValue = false) }
                .onFailure { Logger.log(it.toException()) }

            AnalyticsService.logEvent(AnalyticsEvent.DELETE_ACCOUNT)
            Application.reset(
                preserveCurrentUserID = false,
                onCompletion = ResetCompletionProcedure.EXIT_GRACEFULLY,
            )
        }
    }

    private suspend fun performSignOut() {
        runCatching { NotificationService.setBadgeNumber(0, updateHostedValue = false) }
            .onFailure { Logger.log(it.toException()) }

        UserSessionService.currentUser?.let { currentUser ->
            UserSessionService.stopObservingCurrentUserChanges()
            runCatching { currentUser.removeCurrentPushToken() }.onFailure { Logger.log(it.toException()) }
        }

        Application.dismissSheets()
        Application.reset()
        AnalyticsService.logEvent(AnalyticsEvent.LOG_OUT)
        Task.delayed(by = SIGN_OUT_NAVIGATION_DELAY.milliseconds) { returnToOnboarding() }
    }

    private fun exitGracefully() {
        Application.dismissSheets()
        Application.beginGracefulExit()
    }

    private fun reload() {
        val navigation = DependencyValues.current.navigation
        navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
        navigation.navigate(Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash)))
    }

    private fun returnToOnboarding() {
        val navigation = DependencyValues.current.navigation
        navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
        navigation.navigate(Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Onboarding)))
    }

    private fun openURL(url: String) {
        val context = appContext ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { context.startActivity(intent) }
    }

    private fun Throwable.toException(): Exception = this as? Exception ?: Exception.from(this, ExceptionMetadata(this))

    // MARK: - Companion

    private const val CLIPBOARD_LABEL = "Build Info"
    private const val ENTER_PRERELEASE_MODE_EXIT_TITLE = "Exit Prerelease Mode"
    private const val ENTER_PRERELEASE_MODE_TITLE = "Enter Prerelease Mode"
    private const val SIGN_OUT_NAVIGATION_DELAY = 500
}

// MARK: - Developer Mode List Item

/** A developer mode row shown on the settings page. */
data class DeveloperModeListItem(
    val title: String,
    val configuration: SquareIconViewConfiguration,
    val action: () -> Unit,
)
