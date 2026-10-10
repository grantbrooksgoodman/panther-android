//
//  InviteService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.inviteLanguagePicker
import us.neotechnica.panther.bundle.reviewService
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.HUDConfig
import us.neotechnica.panther.designsystem.modules.foundation.extensions.cancelAction
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheet
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.ReviewServiceStorageKey
import us.neotechnica.panther.modules.common.services.AnalyticsService.AnalyticsEvent
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.modules.content.user.extensions.hasContactsBesidesCurrentUser
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.TranslationInput
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Invites the user's contacts to the app.
 *
 * Use [InviteService] to compose invitation messages – translated into a
 * language of the user's choice – and present the system share sheet for
 * sending them.
 *
 * [initialize] must be called once with the application context before
 * the share-sheet handlers.
 */
object InviteService {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var appContext: Context? = null

    // MARK: - Computed Properties

    private val appOpenCount: Int?
        get() = Persistent.int(PersistentStorageKey.reviewService(ReviewServiceStorageKey.APP_OPEN_COUNT))

    private val canSuggestInvitation: Boolean
        get() {
            val sufficientAppOpenCount = (appOpenCount ?: 0) == 0 || appOpenCount == 1 || (appOpenCount ?: 0) % 2 == 0
            val currentUser = UserSessionService.currentUser

            if (PermissionService.contactPermissionStatus != PermissionService.PermissionStatus.GRANTED) return false
            if (hasContactsBesidesCurrentUser()) return false
            if (!(currentUser?.conversations).isNullOrEmpty()) return false
            if (!(currentUser?.conversationIDs).isNullOrEmpty()) return false
            if (!(DependencyValues.current.onboardingService.createdUserInCurrentAppSession || sufficientAppOpenCount)) return false

            return true
        }

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Compose Invitation

    /**
     * Composes an app invitation and presents the system share sheet for
     * sending it.
     *
     * The invitation contains the app's share link and a prompt message
     * translated into the given language. If the app share link has not
     * yet been resolved, remote metadata is resolved first.
     *
     * @param languageCode The language code into which to translate the
     *   invitation message. Pass `null` to target the system language.
     *   If the given language code is `en`, the message is not translated.
     *
     * @throws Exception if metadata resolution or translation fails.
     */
    suspend fun composeInvitation(languageCode: String?) {
        composeInvitation(languageCode, retried = false)
    }

    // MARK: - Present Invitation Prompt

    /**
     * Asks the user whether to translate the invitation before composing
     * it.
     *
     * If the user declines translation, the invitation is composed
     * targeting the system language. If the user accepts, all presented
     * sheets are dismissed and the invite language picker is presented.
     * Canceling the alert does nothing.
     *
     * @throws Exception if composing the invitation fails.
     */
    suspend fun presentInvitationPrompt() {
        val shouldPresentInviteLanguagePicker = presentTranslationAlert() ?: return

        if (!shouldPresentInviteLanguagePicker) {
            return composeInvitation(null)
        }

        Application.dismissSheets()
        Task.delayed(by = PRESENT_PICKER_DELAY_SECONDS.seconds) { RootSheets.present(RootSheet.inviteLanguagePicker) }
    }

    // MARK: - Suggest Invitation If Needed

    /**
     * Presents the invitation suggestion prompt if the user has no
     * conversations and no registered contacts.
     *
     * The suggestion requires contact permission and is limited to
     * qualifying app launches. Before presenting, this method syncs the
     * contacts to confirm that no contact besides the current user has an
     * account.
     *
     * @return `true` if the invitation suggestion was presented;
     *   otherwise, `false`.
     */
    suspend fun suggestInvitationIfNeeded(): Boolean {
        if (!canSuggestInvitation) return false

        val didSync = runCatching { ContactService.syncContactPairArchive() }.isSuccess
        if (!didSync) return false
        if (hasContactsBesidesCurrentUser()) return false

        presentInvitationSuggestionPrompt()
        return true
    }

    // MARK: - Auxiliary

    private suspend fun composeInvitation(
        languageCode: String?,
        retried: Boolean,
    ) {
        val appShareLink = MetadataService.appShareLink
        if (appShareLink == null) {
            if (retried) throw Exception("Failed to resolve app share link.", metadata = ExceptionMetadata(this))
            MetadataService.resolveValues()
            return composeInvitation(languageCode, retried = true)
        }

        val promptMessage =
            "Hey, let's chat on ⌘${Build.finalName}⌘! It's a simple messaging app that allows us to " +
                "easily talk to each other in our native languages!"

        AnalyticsService.logEvent(AnalyticsEvent.INVITE)
        if (languageCode == "en") {
            return presentShareSheet(appShareLink, promptMessage.sanitized)
        }

        val translation =
            Networking.config.hostedTranslationDelegate.translate(
                TranslationInput(promptMessage),
                LanguagePair(from = "en", to = languageCode ?: RuntimeStorage.languageCode),
                hudConfig = HUDConfig(appearsAfter = Duration.ZERO, isModal = true),
            )

        presentShareSheet(appShareLink, translation.output.sanitized)
    }

    /**
     * Presents an alert suggesting that the user invite their contacts
     * to the app. If the user accepts, the invitation prompt is
     * presented.
     */
    suspend fun presentInvitationSuggestionPrompt() {
        val inviteAction =
            Action("Send Invite", style = ActionStyle.PREFERRED) {
                scope.launch {
                    runCatching { presentInvitationPrompt() }.onFailure { Logger.log(it.toException(), with = AlertType.toast) }
                }
            }
        val message =
            "It doesn't appear that any of your contacts have an account on ⌘${Build.finalName}⌘ yet.\n\n" +
                "Would you like to send them an invite to sign up?"

        Alert(
            message = message,
            actions = listOf(inviteAction, Action.cancelAction),
        ).present(
            translating =
                listOf(
                    Alert.TranslationOptionKey.Actions(listOf(inviteAction)),
                    Alert.TranslationOptionKey.Message,
                ),
        )
    }

    private suspend fun presentTranslationAlert(): Boolean? {
        var shouldTranslate: Boolean? = null
        val acceptTranslationAction =
            Action(
                "Yes, translate",
                style = ActionStyle.PREFERRED,
            ) { shouldTranslate = true }
        val rejectTranslationAction = Action("No, don't translate") { shouldTranslate = false }

        Alert(
            title = "Translate Invitation",
            message = "Would you like ⌘${Build.finalName}⌘ to translate the invitation message into another language?",
            actions =
                listOf(
                    acceptTranslationAction,
                    rejectTranslationAction,
                    Action.cancelAction,
                ),
        ).present(
            translating =
                listOf(
                    Alert.TranslationOptionKey.Actions(listOf(acceptTranslationAction, rejectTranslationAction)),
                    Alert.TranslationOptionKey.Message,
                    Alert.TranslationOptionKey.Title,
                ),
        )

        return shouldTranslate
    }

    private fun presentShareSheet(
        appShareLink: String,
        text: String,
    ) {
        val context = appContext ?: return
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$text\n$appShareLink")
            }
        val chooser = Intent.createChooser(shareIntent, INVITE_FRIENDS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { context.startActivity(chooser) }
    }

    private fun hasContactsBesidesCurrentUser(): Boolean = ContactService.hasContactsBesidesCurrentUser

    private fun Throwable.toException(): Exception = this as? Exception ?: Exception.from(this, ExceptionMetadata(this))

    // MARK: - Companion

    private const val INVITE_FRIENDS = "Invite friends"
    private const val PRESENT_PICKER_DELAY_SECONDS = 2
}
