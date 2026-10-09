//
//  OutboxEntry+SessionExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.extensions

import us.neotechnica.panther.modules.common.extensions.shortened
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.models.RichMessageContent
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.networking.modules.translation.extensions.reference
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * A display message representing this outbox entry, so staged content
 * can appear in the message list before its delivery completes.
 */
val OutboxEntry.asDisplayMessage: Message
    get() {
        val languageCode = UserSessionService.currentUser?.languageCode ?: RuntimeStorage.languageCode
        val selfTranslationPair = LanguagePair(from = languageCode, to = languageCode)

        return when (val payload = payload) {
            is OutboxEntry.Payload.Audio -> {
                // Audio sending is cut; no audio entry is enqueued. This
                // defensive branch renders the transcription as text.
                val transcriptionText = transcription.orEmpty()
                val translation =
                    Translation(
                        input = TranslationInput(transcriptionText),
                        output = transcriptionText,
                        languagePair = selfTranslationPair,
                    )
                Message(
                    id = id,
                    fromAccountID = fromAccountID,
                    contentType = HostedContentType.Text,
                    richContent = null,
                    translationReferences = listOf(translation.reference),
                    translations = listOf(translation),
                    readReceipts = null,
                    sentDate = createdDate,
                )
            }

            is OutboxEntry.Payload.Media -> {
                // A media entry renders as a media message pointing at its
                // staged local file, keyed by content hash so the id matches
                // the delivered message.
                val mediaFile =
                    MediaFile(
                        relativePath = "outbox/${payload.fileName}",
                        name = payload.fileName,
                        fileExtension = payload.fileExtension,
                    )
                Message(
                    id = id,
                    fromAccountID = fromAccountID,
                    contentType = HostedContentType.Media(id = mediaFile.encodedHash.shortened, fileExtension = payload.fileExtension),
                    richContent = RichMessageContent.Media(mediaFile),
                    translationReferences = null,
                    translations = null,
                    readReceipts = null,
                    sentDate = createdDate,
                )
            }

            is OutboxEntry.Payload.Text -> {
                val translation =
                    Translation(
                        input = TranslationInput(payload.value),
                        output = payload.value,
                        languagePair = selfTranslationPair,
                    )
                Message(
                    id = id,
                    fromAccountID = fromAccountID,
                    contentType = HostedContentType.Text,
                    richContent = null,
                    translationReferences = listOf(translation.reference),
                    translations = listOf(translation),
                    readReceipts = null,
                    sentDate = createdDate,
                )
            }
        }
    }
