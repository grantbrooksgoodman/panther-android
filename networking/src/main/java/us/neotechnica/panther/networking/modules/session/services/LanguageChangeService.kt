//
//  LanguageChangeService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

/**
 * Changes the current user's language.
 *
 * The outgoing language is recorded in the user's previous-language list
 * so past messages read in it can still be resolved, and both fields are
 * written to the user node in a single atomic update.
 *
 * **Note:** iOS records the outgoing language only when messages exist in
 * it; this Phase 8 port always records it (a safe superset), avoiding a
 * full message scan.
 */
object LanguageChangeService {
    /**
     * Changes the current user's language to [languageCode] and updates
     * the runtime language.
     *
     * @throws Exception if the current user is unset or the write fails.
     */
    suspend fun changeLanguage(languageCode: String) {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        val outgoingLanguageCode = RuntimeStorage.languageCode
        var previousLanguageCodes = (currentUser.previousLanguageCodes ?: emptyList()).filter { it != languageCode }
        if (outgoingLanguageCode != languageCode) previousLanguageCodes = previousLanguageCodes + outgoingLanguageCode
        previousLanguageCodes = previousLanguageCodes.distinct().reversed()

        UserMutationService.updateLanguageForCurrentUser(languageCode, previousLanguageCodes)
        RuntimeStorage.languageCode = languageCode
    }
}
