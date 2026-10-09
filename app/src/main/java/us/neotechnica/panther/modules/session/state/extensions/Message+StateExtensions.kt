//
//  Message+StateExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.extensions

import us.neotechnica.panther.modules.common.models.AudioFileExtension
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.networking.message.models.AudioFile
import us.neotechnica.panther.modules.networking.message.models.AudioMessageReference
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.models.ReadReceipt
import us.neotechnica.panther.modules.networking.message.models.RichMessageContent
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

// MARK: - Archived

/**
 * The message's local archive representation, carrying its decoded
 * translations and rich content so it can be restored without
 * resolving them again.
 */
val Message.archived: Map<String, Any?>
    get() {
        val formatter = DependencyValues.current.timestampDateFormatter
        return mapOf(
            ArchiveKey.CONTENT_TYPE.rawValue to contentType.hostedValue,
            ArchiveKey.FROM_ACCOUNT_ID.rawValue to fromAccountID,
            ArchiveKey.ID.rawValue to id,
            ArchiveKey.READ_RECEIPTS.rawValue to readReceipts?.map { it.encoded },
            ArchiveKey.RICH_CONTENT.rawValue to richContent?.archived,
            ArchiveKey.SENT_DATE.rawValue to formatter.format(sentDate),
            ArchiveKey.TRANSLATION_REFERENCES.rawValue to translationReferences?.map { it.hostingKey },
            ArchiveKey.TRANSLATIONS.rawValue to translations?.map { it.archived },
        )
    }

// MARK: - From Archive

/**
 * Creates a message from its local archive representation.
 *
 * @param data The archived message data.
 *
 * @return The message, or `null` if the data is malformed.
 */
fun Message.Companion.fromArchive(data: Map<String, Any?>): Message? {
    val formatter = DependencyValues.current.timestampDateFormatter
    val id = data[ArchiveKey.ID.rawValue] as? String ?: return null
    val fromAccountID = data[ArchiveKey.FROM_ACCOUNT_ID.rawValue] as? String ?: return null
    val contentType = (data[ArchiveKey.CONTENT_TYPE.rawValue] as? String)?.let { HostedContentType.from(it) } ?: return null
    val sentDate = (data[ArchiveKey.SENT_DATE.rawValue] as? String)?.let { formatter.parse(it) } ?: return null

    val richContent = (data[ArchiveKey.RICH_CONTENT.rawValue] as? Map<*, *>)?.let { richContent(it) ?: return null }
    val readReceipts = stringList(data[ArchiveKey.READ_RECEIPTS.rawValue])?.map { ReadReceipt.decode(it) }
    val translationReferences = stringList(data[ArchiveKey.TRANSLATION_REFERENCES.rawValue])?.mapNotNull { TranslationReference.from(it) }
    val translations = (data[ArchiveKey.TRANSLATIONS.rawValue] as? List<*>)?.map { translation(it) ?: return null }

    return Message(
        id = id,
        fromAccountID = fromAccountID,
        contentType = contentType,
        richContent = richContent,
        translationReferences = translationReferences,
        translations = translations,
        readReceipts = readReceipts,
        sentDate = sentDate,
    )
}

// MARK: - Auxiliary

private enum class ArchiveKey(
    val rawValue: String,
) {
    AUDIO("audio"),
    CONTENT_DURATION("contentDuration"),
    CONTENT_TYPE("contentType"),
    FILE_EXTENSION("fileExtension"),
    FROM_ACCOUNT_ID("fromAccountID"),
    ID("id"),
    INPUT("input"),
    LANGUAGE_PAIR("languagePair"),
    MEDIA("media"),
    NAME("name"),
    ORIGINAL("original"),
    OUTPUT("output"),
    READ_RECEIPTS("readReceipts"),
    RELATIVE_PATH("relativePath"),
    RICH_CONTENT("richContent"),
    SENT_DATE("sentDate"),
    TRANSLATED("translated"),
    TRANSLATED_DIRECTORY_PATH("translatedDirectoryPath"),
    TRANSLATION("translation"),
    TRANSLATION_REFERENCES("translationReferences"),
    TRANSLATIONS("translations"),
}

private val AudioFile.archived: Map<String, Any?>
    get() =
        mapOf(
            ArchiveKey.CONTENT_DURATION.rawValue to contentDuration?.toDouble(),
            ArchiveKey.FILE_EXTENSION.rawValue to fileExtension.rawValue,
            ArchiveKey.NAME.rawValue to name,
            ArchiveKey.RELATIVE_PATH.rawValue to relativePath,
        )

private val AudioMessageReference.archived: Map<String, Any?>
    get() =
        mapOf(
            ArchiveKey.ORIGINAL.rawValue to original.archived,
            ArchiveKey.TRANSLATED.rawValue to translated.archived,
            ArchiveKey.TRANSLATED_DIRECTORY_PATH.rawValue to translatedDirectoryPath,
            ArchiveKey.TRANSLATION.rawValue to translation.archived,
        )

private val MediaFile.archived: Map<String, Any?>
    get() =
        mapOf(
            ArchiveKey.FILE_EXTENSION.rawValue to fileExtension.rawValue,
            ArchiveKey.NAME.rawValue to name,
            ArchiveKey.RELATIVE_PATH.rawValue to relativePath,
        )

private val RichMessageContent.archived: Map<String, Any?>
    get() =
        when (this) {
            is RichMessageContent.Audio -> mapOf(ArchiveKey.AUDIO.rawValue to components.map { it.archived })
            is RichMessageContent.Media -> mapOf(ArchiveKey.MEDIA.rawValue to file.archived)
        }

private val Translation.archived: Map<String, Any?>
    get() =
        mapOf(
            ArchiveKey.INPUT.rawValue to input.value,
            ArchiveKey.LANGUAGE_PAIR.rawValue to languagePair.string,
            ArchiveKey.OUTPUT.rawValue to output,
        )

private fun audioFile(data: Any?): AudioFile? {
    val map = data as? Map<*, *> ?: return null
    val fileExtensionString = map[ArchiveKey.FILE_EXTENSION.rawValue] as? String ?: return null
    val fileExtension = AudioFileExtension.entries.firstOrNull { it.rawValue == fileExtensionString } ?: return null
    return AudioFile(
        relativePath = map[ArchiveKey.RELATIVE_PATH.rawValue] as? String ?: return null,
        name = map[ArchiveKey.NAME.rawValue] as? String ?: return null,
        fileExtension = fileExtension,
        contentDuration = (map[ArchiveKey.CONTENT_DURATION.rawValue] as? Number)?.toFloat(),
    )
}

private fun audioMessageReference(data: Any?): AudioMessageReference? {
    val map = data as? Map<*, *> ?: return null
    return AudioMessageReference(
        translation = translation(map[ArchiveKey.TRANSLATION.rawValue]) ?: return null,
        original = audioFile(map[ArchiveKey.ORIGINAL.rawValue]) ?: return null,
        translated = audioFile(map[ArchiveKey.TRANSLATED.rawValue]) ?: return null,
        translatedDirectoryPath = map[ArchiveKey.TRANSLATED_DIRECTORY_PATH.rawValue] as? String ?: return null,
    )
}

private fun mediaFile(data: Any?): MediaFile? {
    val map = data as? Map<*, *> ?: return null
    val fileExtension = (map[ArchiveKey.FILE_EXTENSION.rawValue] as? String)?.let { MediaFileExtension.from(it) } ?: return null
    return MediaFile(
        relativePath = map[ArchiveKey.RELATIVE_PATH.rawValue] as? String ?: return null,
        name = map[ArchiveKey.NAME.rawValue] as? String ?: return null,
        fileExtension = fileExtension,
    )
}

private fun richContent(map: Map<*, *>): RichMessageContent? {
    (map[ArchiveKey.MEDIA.rawValue] as? Map<*, *>)?.let { return RichMessageContent.Media(mediaFile(it) ?: return null) }
    val audio = map[ArchiveKey.AUDIO.rawValue] as? List<*> ?: return null
    return RichMessageContent.Audio(audio.map { audioMessageReference(it) ?: return null })
}

private fun stringList(data: Any?): List<String>? =
    (data as? List<*>)
        ?.takeIf { list -> list.all { it is String } }
        ?.map { it as String }

private fun translation(data: Any?): Translation? {
    val map = data as? Map<*, *> ?: return null
    val languagePair = (map[ArchiveKey.LANGUAGE_PAIR.rawValue] as? String)?.let { LanguagePair.fromString(it) } ?: return null
    return Translation(
        input = TranslationInput(map[ArchiveKey.INPUT.rawValue] as? String ?: return null),
        output = map[ArchiveKey.OUTPUT.rawValue] as? String ?: return null,
        languagePair = languagePair,
    )
}
