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
import us.neotechnica.panther.modules.content.user.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.networking.common.visibleForCurrentUser
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.updateValues
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.translator.models.Translation

/**
 * The service that applies the user's language selection from the
 * language change page.
 *
 * Use [ChangeLanguagePageViewService] to confirm and apply a new app
 * language. Applying a language persists it to the current user's
 * remote record and resets the app, which must restart for the change
 * to take effect.
 */
object ChangeLanguagePageViewService {
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
            val languageName =
                LocalizedStringResolver.languageDisplayNames()[selectedLanguageCode] ?: selectedLanguageCode.uppercase()

            val applyAndExitAction =
                Action(
                    "Apply & Exit",
                    style = ActionStyle.DESTRUCTIVE_PREFERRED,
                ) {
                    scope.launch {
                        runCatching {
                            changeLanguage(selectedLanguageCode)
                        }.onFailure { Logger.log(it.toException(), with = AlertType.toast) }
                    }
                }

            ActionSheet(
                title = "Change Language to ⌘$languageName⌘",
                message = "You must restart the app for this to take effect.",
                actions =
                    listOf(
                        applyAndExitAction,
                        Action.cancelAction,
                    ),
            ).present(
                translating =
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
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        UserSessionService.resolveCurrentUser(User.DataType.entries.toSet())

        val conversations = (UserSessionService.currentUser?.conversations ?: emptyList()).visibleForCurrentUser
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

        currentUser.updateValues(
            mapOf(
                UserUpdatableKey.LANGUAGE_CODE to languageCode,
                UserUpdatableKey.PREVIOUS_LANGUAGE_CODES to newPreviousLanguageCodes.ifEmpty { bangQualifiedEmptyList },
            ),
        )
        CoreUtilities.setLanguageCode(languageCode)

        Application.reset(
            preserveCurrentUserID = true,
            onCompletion = ResetCompletionProcedure.EXIT_GRACEFULLY,
        )
    }

    private fun List<Conversation>.messageTranslations(fromCurrentUser: Boolean): List<Translation> =
        flatMap { it.messages ?: emptyList() }
            .filter { it.isFromCurrentUser == fromCurrentUser }
            .flatMap { it.translations ?: emptyList() }
            .distinct()

    private fun Throwable.toException(): Exception = this as? Exception ?: Exception.from(this, ExceptionMetadata(this))
}
