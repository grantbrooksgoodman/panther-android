//
//  ChangeLanguagePageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.Application.ResetCompletionProcedure
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.foundation.extensions.cancelAction
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.modules.content.user.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.common.extensions.visibleForCurrentUser
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.updateValues
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.extensions.languageExonym
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.Timeout
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.translator.models.Translation
import kotlin.time.Duration.Companion.seconds

/**
 * The service that applies the user's language selection from the
 * language change page.
 *
 * Use [ChangeLanguagePageViewService] to confirm and apply a new app
 * language. Applying a language persists it to the current user's
 * remote record and resets the app, which must restart for the change
 * to take effect.
 *
 * Resolve the service through
 * [DependencyValues.changeLanguagePageViewService][us.neotechnica.panther.modules.content.user.dependencies.changeLanguagePageViewService].
 *
 * @param dependencies The collaborators the service resolves its work
 *   through. The default resolves the live services.
 */
class ChangeLanguagePageViewService(
    private val dependencies: Dependencies = Dependencies(),
) {
    // MARK: - Dependencies

    // MARK: - Types

    /**
     * The collaborators the service resolves its work through.
     *
     * Each property defaults to the live service; a test replaces the
     * ones it exercises.
     *
     * @property addOverlay Adds the non-modal dimming overlay.
     * @property currentUser Returns the current user.
     * @property hideHUD Hides the HUD.
     * @property presentActionSheet Presents the given action sheet,
     *   translating the given parts.
     * @property reset Resets the app, preserving the current user's
     *   identifier, and exits.
     * @property resolveCurrentUser Resolves every data type of the
     *   current user.
     * @property showProgressHUD Shows the modal progress HUD with the
     *   given text.
     * @property updateValues Updates the given user's hosted values
     *   with the given data, returning the updated user.
     */
    data class Dependencies(
        val addOverlay: () -> Unit = {
            Overlay.addOverlay(
                alpha = OVERLAY_ALPHA,
                activityIndicator = null,
                isModal = false,
            )
        },
        val currentUser: () -> User? = { DependencyValues.current.clientSession.entity.user.currentUser },
        val hideHUD: () -> Unit = { HUD.hide() },
        val presentActionSheet: suspend (ActionSheet, List<ActionSheet.TranslationOptionKey>) -> Unit = { sheet, translating ->
            sheet.present(translating = translating)
        },
        val reset: () -> Unit = {
            Application.reset(
                preserveCurrentUserID = true,
                onCompletion = ResetCompletionProcedure.EXIT_GRACEFULLY,
            )
        },
        val resolveCurrentUser: suspend () -> Unit = {
            DependencyValues.current.clientSession.entity.user
                .resolveCurrentUser(User.DataType.entries.toSet())
        },
        val showProgressHUD: (String) -> Unit = { text -> HUD.showProgress(text = text, isModal = true) },
        val updateValues: suspend (User, Map<UserUpdatableKey, Any>) -> User = { user, data -> user.updateValues(data) },
    )

    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Reducer Action Handlers

    /**
     * Asks the user to confirm the language change, applying it if they
     * accept.
     *
     * Confirmation warns that the app must restart. If the user accepts,
     * the new language is written to the current user's remote record in
     * a single atomic update, together with a language history that
     * records the outgoing language only when messages were sent or
     * received in it. The app then resets – preserving the current
     * user's identifier – and exits. Failures surface as a toast.
     *
     * @param selectedLanguageCode The language code of the selected
     *   language.
     */
    fun confirmButtonTapped(selectedLanguageCode: String) {
        scope.launch {
            val applyAndExitAction =
                Action(
                    "Apply & Exit",
                    style = ActionStyle.DESTRUCTIVE_PREFERRED,
                ) {
                    scope.launch {
                        try {
                            changeLanguage(selectedLanguageCode)
                        } catch (exception: Exception) {
                            Logger.log(exception, with = AlertType.toast)
                        }
                    }
                }

            dependencies.presentActionSheet(
                ActionSheet(
                    title = "Change Language to ⌘${selectedLanguageCode.languageExonym ?: selectedLanguageCode.uppercase()}⌘",
                    message = "You must restart the app for this to take effect.",
                    actions =
                        listOf(
                            applyAndExitAction,
                            Action.cancelAction,
                        ),
                ),
                listOf(
                    ActionSheet.TranslationOptionKey.Actions(listOf(applyAndExitAction)),
                    ActionSheet.TranslationOptionKey.Message,
                    ActionSheet.TranslationOptionKey.Title,
                ),
            )
        }
    }

    // MARK: - Auxiliary

    private suspend fun changeLanguage(languageCode: String) {
        val currentUser =
            dependencies.currentUser()
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        try {
            val loadedData = LockIsolated<Boolean?>(null)
            val timeout =
                Timeout(after = PROGRESS_HUD_DELAY) {
                    if (loadedData.wrappedValue == true) return@Timeout
                    dependencies.addOverlay()
                    dependencies.showProgressHUD(LocalizedStringKey.SettingLanguage.localized(languageCode = languageCode))
                }

            dependencies.resolveCurrentUser()

            val conversations = (dependencies.currentUser()?.conversations ?: emptyList()).visibleForCurrentUser
            val outgoingLanguageCode = RuntimeStorage.languageCode

            val hasIncomingMessagesInCurrentLanguage =
                conversations
                    .filter { conversation ->
                        !(conversation.users ?: emptyList()).mapNotNull { it.languageCode }.contains(outgoingLanguageCode)
                    }.messageTranslations(fromCurrentUser = false)
                    .map { it.languagePair.to }
                    .contains(outgoingLanguageCode)

            val hasOutgoingMessagesInCurrentLanguage =
                conversations
                    .messageTranslations(fromCurrentUser = true)
                    .map { it.languagePair.from }
                    .contains(outgoingLanguageCode)

            var newPreviousLanguageCodes = (currentUser.previousLanguageCodes ?: emptyList()).filter { it != languageCode }
            if (hasIncomingMessagesInCurrentLanguage || hasOutgoingMessagesInCurrentLanguage) {
                newPreviousLanguageCodes = newPreviousLanguageCodes + outgoingLanguageCode
            }
            newPreviousLanguageCodes = newPreviousLanguageCodes.distinct().reversed()

            loadedData.wrappedValue = true
            timeout.cancel()

            // Both fields land in a single atomic write on the user node.
            dependencies.updateValues(
                currentUser,
                mapOf(
                    UserUpdatableKey.LANGUAGE_CODE to languageCode,
                    UserUpdatableKey.PREVIOUS_LANGUAGE_CODES to newPreviousLanguageCodes.ifEmpty { bangQualifiedEmptyList },
                ),
            )

            dependencies.reset()
        } finally {
            dependencies.hideHUD()
        }
    }

    private fun List<Conversation>.messageTranslations(fromCurrentUser: Boolean): List<Translation> =
        flatMap { it.messages ?: emptyList() }
            .filter { it.isFromCurrentUser == fromCurrentUser }
            .flatMap { it.translations ?: emptyList() }
            .distinct()

    // MARK: - Companion

    private companion object {
        const val OVERLAY_ALPHA = 0.5f
        val PROGRESS_HUD_DELAY = 1.seconds
    }
}
