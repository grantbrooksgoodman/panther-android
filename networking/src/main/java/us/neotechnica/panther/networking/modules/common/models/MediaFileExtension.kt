//
//  MediaFileExtension.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.models

// MARK: - Media File Extension

/**
 * A media file's type and extension.
 *
 * [MediaFileExtension] categorizes a file extension by media kind –
 * audio, document, image, or video – carrying the specific extension
 * as an associated value.
 */
sealed interface MediaFileExtension {
    // MARK: - Properties

    /** The MIME content type for this extension. */
    val contentTypeString: String

    /** The extension's string value, without a leading period. */
    val rawValue: String

    /** Whether this is an audio extension. */
    val isAudio: Boolean get() = this is Audio

    /** Whether this is a document extension. */
    val isDocument: Boolean get() = this is Document

    /** Whether this is an image extension. */
    val isImage: Boolean get() = this is Image

    /** Whether this is a video extension. */
    val isVideo: Boolean get() = this is Video

    // MARK: - Cases

    /** An audio file with the given extension. */
    data class Audio(
        val fileExtension: AudioFileExtension,
    ) : MediaFileExtension {
        override val contentTypeString: String get() = fileExtension.contentTypeString
        override val rawValue: String get() = fileExtension.rawValue
    }

    /** A document file with the given extension. */
    data class Document(
        val fileExtension: DocumentFileExtension,
    ) : MediaFileExtension {
        override val contentTypeString: String get() = fileExtension.contentTypeString
        override val rawValue: String get() = fileExtension.rawValue
    }

    /** An image file with the given extension. */
    data class Image(
        val fileExtension: ImageFileExtension,
    ) : MediaFileExtension {
        override val contentTypeString: String get() = fileExtension.contentTypeString
        override val rawValue: String get() = fileExtension.rawValue
    }

    /** A video file with the given extension. */
    data class Video(
        val fileExtension: VideoFileExtension,
    ) : MediaFileExtension {
        override val contentTypeString: String get() = fileExtension.contentTypeString
        override val rawValue: String get() = fileExtension.rawValue
    }

    // MARK: - Companion

    companion object {
        /**
         * Creates a media file extension from the given string, ignoring
         * case and surrounding whitespace.
         *
         * A string that does not match a known audio, image, video, or PDF
         * extension but is non-empty and strictly alphanumeric is treated
         * as a plain-text document extension.
         *
         * @param string The extension's string value, without a leading
         *   period.
         *
         * @return A media file extension, or `null` if the string is empty
         *   or not strictly alphanumeric.
         */
        fun from(string: String): MediaFileExtension? {
            val rawValue = string.trim().lowercase()
            return when {
                rawValue == AudioFileExtension.CAF.rawValue -> Audio(AudioFileExtension.CAF)
                rawValue == AudioFileExtension.M4A.rawValue -> Audio(AudioFileExtension.M4A)
                rawValue == DocumentFileExtension.Pdf.rawValue -> Document(DocumentFileExtension.Pdf)
                rawValue == ImageFileExtension.JPEG.rawValue -> Image(ImageFileExtension.JPEG)
                rawValue == ImageFileExtension.JPG.rawValue -> Image(ImageFileExtension.JPG)
                rawValue == ImageFileExtension.PNG.rawValue -> Image(ImageFileExtension.PNG)
                rawValue == VideoFileExtension.MP4.rawValue -> Video(VideoFileExtension.MP4)
                rawValue.isNotEmpty() && rawValue.all { it.isLetterOrDigit() } ->
                    Document(DocumentFileExtension.PlainText(rawValue))
                else -> null
            }
        }
    }
}

// MARK: - Audio File Extension

/** The supported audio file extensions. */
enum class AudioFileExtension(
    val rawValue: String,
) {
    CAF("caf"),
    M4A("m4a"),
    ;

    /** The MIME content type for this extension. */
    val contentTypeString: String
        get() =
            when (this) {
                CAF -> "audio/x-caf"
                M4A -> "audio/m4a"
            }
}

// MARK: - Document File Extension

/**
 * A document file's extension.
 *
 * [DocumentFileExtension] distinguishes PDF documents from plain-text
 * documents, carrying an arbitrary plain-text extension as an
 * associated value.
 */
sealed interface DocumentFileExtension {
    /** The MIME content type for this extension. */
    val contentTypeString: String

    /** The extension's string value, without a leading period. */
    val rawValue: String

    /** A PDF file. */
    data object Pdf : DocumentFileExtension {
        override val contentTypeString: String get() = "application/pdf"
        override val rawValue: String get() = "pdf"
    }

    /**
     * A plain-text file with the given extension, lowercased and without
     * a leading period.
     */
    data class PlainText(
        val fileExtension: String,
    ) : DocumentFileExtension {
        override val contentTypeString: String get() = "text/plain"
        override val rawValue: String get() = fileExtension
    }
}

// MARK: - Image File Extension

/** The supported image file extensions. */
enum class ImageFileExtension(
    val rawValue: String,
) {
    JPEG("jpeg"),
    JPG("jpg"),
    PNG("png"),
    ;

    /** The MIME content type for this extension. */
    val contentTypeString: String
        get() =
            when (this) {
                JPEG, JPG -> "image/jpeg"
                PNG -> "image/png"
            }
}

// MARK: - Video File Extension

/** The supported video file extensions. */
enum class VideoFileExtension(
    val rawValue: String,
) {
    MP4("mp4"),
    ;

    /** The MIME content type for this extension. */
    val contentTypeString: String
        get() =
            when (this) {
                MP4 -> "video/mp4"
            }
}
