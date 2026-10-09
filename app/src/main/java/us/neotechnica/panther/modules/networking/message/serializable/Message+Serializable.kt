//
//  Message+Serializable.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.serializable

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.networking.common.messageService
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.LocalAudioFilePath
import us.neotechnica.panther.modules.networking.message.models.LocalMediaFilePath
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.models.ReadReceipt
import us.neotechnica.panther.modules.networking.message.models.RichMessageContent
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.networking.modules.translation.models.TranslationValidator
import us.neotechnica.panther.networking.modules.translation.serializable.from
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation

// MARK: - Type Aliases

private typealias Keys = Message.SerializableKey

// MARK: - Can Decode

/**
 * Returns a Boolean value that indicates whether a message can be
 * decoded from the given data.
 *
 * @param data The serialized message data.
 *
 * @return `true` if a message can be decoded; otherwise, `false`.
 */
fun Message.Companion.canDecode(data: Map<String, Any?>): Boolean {
    if (data[Keys.ID.rawValue] !is String) return false
    if (data[Keys.FROM_ACCOUNT_ID.rawValue] !is String) return false
    val contentTypeString = data[Keys.CONTENT_TYPE.rawValue] as? String ?: return false
    val hostedContentType = HostedContentType.from(contentTypeString) ?: return false
    val encodedReadReceipts = stringList(data, Keys.READ_RECEIPTS) ?: return false
    if (!encodedReadReceipts.isBangQualifiedEmpty && !encodedReadReceipts.all { ReadReceipt.canDecode(it) }) {
        return false
    }

    val sentDateString = data[Keys.SENT_DATE.rawValue] as? String ?: return false
    if (DependencyValues.current.timestampDateFormatter.parse(sentDateString) == null) return false
    val translationReferenceStrings = stringList(data, Keys.TRANSLATION_REFERENCES) ?: return false

    if (hostedContentType == HostedContentType.Text &&
        translationReferenceStrings.isBangQualifiedEmpty
    ) {
        return false
    }

    return true
}

// MARK: - Decode

/**
 * Creates a message by decoding the given serialized data.
 *
 * Decoding resolves the message's translations and, for audio and
 * media messages, downloads its content from remote storage.
 *
 * @param data The serialized message data.
 *
 * @return The decoded message.
 *
 * @throws Exception if the data cannot be decoded.
 */
suspend fun Message.Companion.decode(data: Map<String, Any?>): Message {
    val currentUser = DependencyValues.current.clientSession.entity.user.currentUser
    val dateFormatter = DependencyValues.current.timestampDateFormatter
    val messageService = DependencyValues.current.networking.messageService

    val id = data[Keys.ID.rawValue] as? String
    val fromAccountID = data[Keys.FROM_ACCOUNT_ID.rawValue] as? String
    val contentType = (data[Keys.CONTENT_TYPE.rawValue] as? String)?.let { HostedContentType.from(it) }
    val translationReferenceStrings = stringList(data, Keys.TRANSLATION_REFERENCES)
    val encodedReadReceipts = stringList(data, Keys.READ_RECEIPTS)
    val sentDate = (data[Keys.SENT_DATE.rawValue] as? String)?.let { dateFormatter.parse(it) }

    if (id == null ||
        fromAccountID == null ||
        contentType == null ||
        translationReferenceStrings == null ||
        encodedReadReceipts == null ||
        sentDate == null
    ) {
        throw Exception.Networking.decodingFailed(data, ExceptionMetadata(Message::class))
    }

    var readReceipts: List<ReadReceipt>? = null
    if (!encodedReadReceipts.isBangQualifiedEmpty) {
        readReceipts = encodedReadReceipts.map { ReadReceipt.decode(it) }
    }

    val languageCode = currentUser?.languageCode ?: RuntimeStorage.languageCode
    val translationReferences: List<TranslationReference>? =
        if (translationReferenceStrings.isEmpty()) {
            null
        } else {
            translationReferenceStrings.mapNotNull { TranslationReference.from(it) }
        }

    return when (contentType) {
        is HostedContentType.Audio -> {
            val translations =
                getTranslations(
                    languageCode = languageCode,
                    referenceStrings = translationReferenceStrings,
                    fromAccountID = fromAccountID,
                )

            val translation =
                translations.firstOrNull()
                    ?: throw Exception.Networking.decodingFailed(data, ExceptionMetadata(Message::class))

            Message(
                id = id,
                fromAccountID = fromAccountID,
                contentType = contentType,
                richContent = audioRichContent(id, fromAccountID, translation),
                translationReferences = translationReferences,
                translations = translations,
                readReceipts = readReceipts,
                sentDate = sentDate,
            )
        }

        is HostedContentType.Media -> {
            val localMediaFilePath =
                LocalMediaFilePath.from(contentType)
                    ?: throw Exception.Networking.decodingFailed(data, ExceptionMetadata(Message::class))

            Message(
                id = id,
                fromAccountID = fromAccountID,
                contentType = contentType,
                richContent =
                    RichMessageContent.Media(
                        messageService.media.getMediaComponent(
                            messageID = id,
                            localMediaFilePath = localMediaFilePath,
                        ),
                    ),
                translationReferences = translationReferences,
                translations = null,
                readReceipts = readReceipts,
                sentDate = sentDate,
            )
        }

        HostedContentType.Text ->
            Message(
                id = id,
                fromAccountID = fromAccountID,
                contentType = contentType,
                richContent = null,
                translationReferences = translationReferences,
                translations =
                    getTranslations(
                        languageCode = languageCode,
                        referenceStrings = translationReferenceStrings,
                        fromAccountID = fromAccountID,
                    ),
                readReceipts = readReceipts,
                sentDate = sentDate,
            )
    }
}

// MARK: - Auxiliary

private suspend fun audioRichContent(
    messageID: String,
    fromAccountID: String,
    translation: Translation,
): RichMessageContent =
    RichMessageContent.Audio(
        listOf(
            DependencyValues.current.networking.messageService.audio.getAudioComponent(
                messageID = messageID,
                isFromCurrentUser = fromAccountID == User.currentUserID,
                localAudioFilePath = LocalAudioFilePath.from(messageID, translation),
                translation = translation,
            ),
        ),
    )

private suspend fun getTranslation(reference: TranslationReference): Translation {
    val translation = Translation.from(reference)

    TranslationValidator.validate(
        translation = translation,
        metadata = ExceptionMetadata(Message::class),
    )

    return translation
}

private suspend fun getTranslations(
    languageCode: String,
    referenceStrings: List<String>,
    fromAccountID: String,
): List<Translation> {
    val isFromCurrentUser = fromAccountID == User.currentUserID
    val translationReferences = referenceStrings.mapNotNull { TranslationReference.from(it) }
    var filteredReferences = translationReferences

    val firstMatchingSource = filteredReferences.firstOrNull { it.languagePair.from == languageCode }
    val firstNearlyMatchingSource = filteredReferences.firstOrNull { it.languagePair.from.isUserReadableLanguageCode }

    val firstMatchingTarget = filteredReferences.firstOrNull { it.languagePair.to == languageCode }
    val firstNearlyMatchingTarget = filteredReferences.firstOrNull { it.languagePair.to.isUserReadableLanguageCode }

    var reference = firstMatchingTarget ?: firstMatchingSource ?: firstNearlyMatchingTarget ?: firstNearlyMatchingSource
    if (isFromCurrentUser) {
        reference = firstMatchingSource ?: firstNearlyMatchingSource ?: firstMatchingTarget ?: firstNearlyMatchingTarget
    }

    filteredReferences = if (reference == null) filteredReferences else listOf(reference)
    val translations = getTranslations(references = filteredReferences)

    return when (isFromCurrentUser) {
        true -> {
            val matchingLanguage = translations.filter { it.languagePair.from.isUserReadableLanguageCode }
            val notMatchingLanguage = translations.filter { !it.languagePair.from.isUserReadableLanguageCode }
            matchingLanguage + notMatchingLanguage
        }

        false -> {
            if (firstMatchingTarget == null &&
                (firstMatchingSource ?: firstNearlyMatchingSource) != null
            ) {
                return translations.map {
                    Translation(
                        input = it.input,
                        output = it.input.value,
                        languagePair =
                            LanguagePair(
                                from = it.languagePair.from,
                                to = it.languagePair.from,
                            ),
                    )
                }
            }

            val matchingLanguage = translations.filter { it.languagePair.to.isUserReadableLanguageCode }
            val notMatchingLanguage = translations.filter { !it.languagePair.to.isUserReadableLanguageCode }
            matchingLanguage + notMatchingLanguage
        }
    }
}

private suspend fun getTranslations(references: List<TranslationReference>): List<Translation> {
    if (references.isEmpty()) {
        throw Exception(
            "No translation references provided.",
            metadata = ExceptionMetadata(Message::class),
        )
    }

    val translations =
        coroutineScope {
            references.map { async { getTranslation(it) } }.awaitAll()
        }

    if (!translations.all { it.isWellFormed }) {
        throw Exception(
            "Translations fail validation.",
            metadata = ExceptionMetadata(Message::class),
        )
    }

    return translations
}

@Suppress("UNCHECKED_CAST")
private fun stringList(
    data: Map<String, Any?>,
    key: Keys,
): List<String>? =
    (data[key.rawValue] as? List<*>)
        ?.takeIf { list -> list.all { it is String } }
        ?.map { it as String }

private val String.isUserReadableLanguageCode: Boolean
    get() {
        val currentUser = DependencyValues.current.clientSession.entity.user.currentUser
        val currentUserLanguageCode = currentUser?.languageCode ?: RuntimeStorage.languageCode
        return this == currentUserLanguageCode || currentUser?.previousLanguageCodes?.contains(this) == true
    }
