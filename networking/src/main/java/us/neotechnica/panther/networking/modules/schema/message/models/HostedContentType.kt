//
//  HostedContentType.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.message.models

import us.neotechnica.panther.networking.modules.common.models.AudioFileExtension
import us.neotechnica.panther.networking.modules.common.models.MediaFileExtension

/**
 * The kind of content a message carries.
 *
 * Text is a first-class case; audio and media carry their file
 * identifiers and extensions, mirroring the iOS `HostedContentType`.
 */
sealed interface HostedContentType {
    // MARK: - Properties

    /**
     * The content type's complete wire-format string.
     *
     * For text and audio content this equals [rawValue]. For media
     * content it is the MIME type, file identifier, and file extension
     * joined by `" – "` (a space, an en dash, and a space):
     * `"<mime> – <id> – <ext>"`.
     */
    val hostedValue: String

    /**
     * The content type's raw string. For text this is `"text"`; for
     * audio and media content it is the MIME type.
     */
    val rawValue: String

    /** Whether the content is audio. */
    val isAudio: Boolean get() = this is Audio

    /** Whether the content is media. */
    val isMedia: Boolean get() = this is Media

    /** The media content's identifier, or `null` if the content is not media. */
    val mediaFileID: String? get() = (this as? Media)?.id

    /** The media content's file path, or `null` if the content is not media. */
    val mediaFilePath: String? get() = (this as? Media)?.let { "${it.id}.${it.fileExtension.rawValue}" }

    // MARK: - Cases

    /** Audio content with the given file extension. */
    data class Audio(
        val fileExtension: AudioFileExtension,
    ) : HostedContentType {
        override val hostedValue: String get() = rawValue
        override val rawValue: String get() = fileExtension.contentTypeString
    }

    /**
     * Media content – an image, video, or document – with the given
     * identifier and file extension.
     */
    data class Media(
        val id: String,
        val fileExtension: MediaFileExtension,
    ) : HostedContentType {
        override val hostedValue: String
            get() = "${fileExtension.contentTypeString} $SEPARATOR $id $SEPARATOR ${fileExtension.rawValue}"
        override val rawValue: String get() = fileExtension.contentTypeString
    }

    /** Text content. */
    data object Text : HostedContentType {
        override val hostedValue: String get() = "text"
        override val rawValue: String get() = "text"
    }

    // MARK: - Companion

    companion object {
        /** The separator between a media content type's wire-format components. */
        private const val SEPARATOR = "–"

        /** The `" – "` delimiter joining a media content type's wire-format components. */
        private const val DELIMITER = " – "

        /** The number of `" – "`-joined components in a media content type. */
        private const val MEDIA_COMPONENT_COUNT = 3

        /**
         * Creates a content type from its wire-format string.
         *
         * @param hostedValue The wire-format content type string.
         *
         * @return The content type, or `null` if the string is not a valid
         *   content type.
         */
        fun from(hostedValue: String): HostedContentType? {
            if (hostedValue == Text.rawValue) return Text

            val components = hostedValue.split(DELIMITER)
            return when (components.size) {
                1 -> {
                    val audioFileExtension =
                        AudioFileExtension.entries.firstOrNull { it.contentTypeString == hostedValue }
                    audioFileExtension?.let { Audio(it) }
                }

                MEDIA_COMPONENT_COUNT -> {
                    val id = components[1]
                    val fileExtensionString = components[2]
                    val fileExtension = MediaFileExtension.from(fileExtensionString)
                    if (id.isBlank() ||
                        fileExtensionString.isBlank() ||
                        fileExtension == null ||
                        fileExtension.isAudio ||
                        components[0] != fileExtension.contentTypeString
                    ) {
                        null
                    } else {
                        Media(id = id, fileExtension = fileExtension)
                    }
                }

                else -> null
            }
        }
    }
}
