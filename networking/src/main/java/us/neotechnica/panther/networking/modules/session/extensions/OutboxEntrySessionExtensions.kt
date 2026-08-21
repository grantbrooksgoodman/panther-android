//
//  OutboxEntrySessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.modules.schema.message.models.HostedContentType
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.message.models.TranslationReference
import us.neotechnica.panther.networking.modules.session.models.OutboxEntry
import us.neotechnica.panther.networking.modules.session.services.UserSessionService
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference as HostedTranslationReference

/**
 * A display message representing this outbox entry, so staged content
 * can appear in the message list before its delivery completes.
 */
val OutboxEntry.asDisplayMessage: Message
    get() {
        val languageCode = UserSessionService.currentUser?.languageCode ?: RuntimeStorage.languageCode
        val selfPair = LanguagePair(from = languageCode, to = languageCode)
        val translation =
            Translation(
                input = TranslationInput(text),
                output = text,
                languagePair = selfPair,
            )

        return Message(
            id = id,
            fromAccountID = fromAccountID,
            contentType = HostedContentType.Text,
            translationReferences = listOf(TranslationReference(HostedTranslationReference.from(translation).hostingKey)),
            readReceipts = null,
            sentDate = createdDate,
            translations = listOf(translation),
        )
    }
