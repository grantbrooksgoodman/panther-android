//
//  OutboxEntry+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.common.extensions.shortened
import us.neotechnica.panther.modules.common.models.AudioFileExtension
import us.neotechnica.panther.modules.networking.message.models.AudioFile
import us.neotechnica.panther.modules.networking.message.models.AudioMessageReference
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.models.RichMessageContent
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.networking.modules.translation.extensions.reference
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * A display message representing this outbox entry, so staged content
 * can appear in the message list before its delivery completes.
 */
val OutboxEntry.asDisplayMessage: Message
    get() {
        val clientSession = DependencyValues.current.clientSession
        val currentUser = clientSession.entity.user.currentUser
        val languageCode = currentUser?.languageCode ?: "en"
        val selfTranslationPair =
            LanguagePair(
                from = languageCode,
                to = languageCode,
            )

        return when (val payload = payload) {
            is OutboxEntry.Payload.Audio -> {
                val audioFile =
                    AudioFile(
                        relativePath = "outbox/${payload.inputFileName}",
                        name = payload.inputFileName,
                        fileExtension = AudioFileExtension.M4A,
                        contentDuration = 0f,
                    )

                val mockTranslation =
                    Translation(
                        input = TranslationInput(""),
                        output = "",
                        languagePair = selfTranslationPair,
                    )

                val audioReference =
                    AudioMessageReference(
                        translation = mockTranslation,
                        original = audioFile,
                        translated = audioFile,
                        translatedDirectoryPath = "",
                    )

                Message(
                    id = id,
                    fromAccountID = fromAccountID,
                    contentType = HostedContentType.Audio(AudioFileExtension.M4A),
                    richContent = RichMessageContent.Audio(listOf(audioReference)),
                    translationReferences = listOf(mockTranslation.reference),
                    translations = listOf(mockTranslation),
                    readReceipts = null,
                    sentDate = createdDate,
                )
            }

            is OutboxEntry.Payload.Media -> {
                val mediaFile =
                    MediaFile(
                        "outbox/${payload.fileName}",
                        name = payload.fileName,
                        fileExtension = payload.fileExtension,
                    )

                Message(
                    id = id,
                    fromAccountID = fromAccountID,
                    contentType =
                        HostedContentType.Media(
                            id = mediaFile.encodedHash.shortened,
                            fileExtension = payload.fileExtension,
                        ),
                    richContent = RichMessageContent.Media(mediaFile),
                    translationReferences = null,
                    translations = null,
                    readReceipts = null,
                    sentDate = createdDate,
                )
            }

            is OutboxEntry.Payload.Text -> {
                val mockTranslation =
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
                    translationReferences = listOf(mockTranslation.reference),
                    translations = listOf(mockTranslation),
                    readReceipts = null,
                    sentDate = createdDate,
                )
            }
        }
    }
