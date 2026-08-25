//
//  RichMessageContent.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.message.models

/**
 * The rich content of a message.
 *
 * **Note:** This viewing-phase port models media content only; audio
 * content lands with the audio-message phase.
 */
sealed interface RichMessageContent {
    // MARK: - Properties

    /** The media file, or `null` if the content is not media. */
    val mediaComponent: MediaFile?
        get() = (this as? Media)?.file

    /** The document, or `null` if the content is not a document. */
    val documentComponent: MediaFile?
        get() = mediaComponent?.takeIf { it.fileExtension.isDocument }

    /** The image, or `null` if the content is not an image. */
    val imageComponent: MediaFile?
        get() = mediaComponent?.takeIf { it.fileExtension.isImage }

    /** The video, or `null` if the content is not a video. */
    val videoComponent: MediaFile?
        get() = mediaComponent?.takeIf { it.fileExtension.isVideo }

    // MARK: - Cases

    /** Media content – an image, video, or document. */
    data class Media(
        val file: MediaFile,
    ) : RichMessageContent
}
