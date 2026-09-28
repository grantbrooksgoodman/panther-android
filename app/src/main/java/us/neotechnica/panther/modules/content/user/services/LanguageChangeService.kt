//
//  LanguageChangeService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.updateValues
import us.neotechnica.panther.modules.session.entity.extensions.conversations
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.messages
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.entity.extensions.visibleForCurrentUser
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.models.Translation

/**
 * Changes the current user's language.
 *
 * The outgoing language is recorded in the user's previous-language list
 * so past messages read in it can still be resolved, and both fields are
 * written to the user node in a single atomic update.
 */
object LanguageChangeService {
    /**
     * Changes the current user's language to [languageCode] and updates
     * the runtime language.
     *
     * The outgoing language is recorded in the user's previous-language
     * list only when the user has actually sent or received messages in
     * it, scanning every visible conversation's translations.
     *
     * @throws Exception if the current user is unset or the write fails.
     */
    suspend fun changeLanguage(languageCode: String) {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        UserSessionService.resolveCurrentUser(UserSessionService.DataType.entries.toSet())

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
        RuntimeStorage.languageCode = languageCode
    }

    // MARK: - Auxiliary

    private fun List<Conversation>.messageTranslations(fromCurrentUser: Boolean): List<Translation> =
        flatMap { it.messages ?: emptyList() }
            .filter { it.isFromCurrentUser == fromCurrentUser }
            .flatMap { it.translations ?: emptyList() }
            .distinct()
}
