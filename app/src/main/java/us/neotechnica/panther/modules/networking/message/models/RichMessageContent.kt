//
//  RichMessageContent.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.models

/** The rich content of a message – audio or media. */
sealed interface RichMessageContent {
    // MARK: - Properties

    /** The audio components, or `null` if the content is not audio. */
    val audioComponents: List<AudioMessageReference>?
        get() = (this as? Audio)?.components

    /** The document, or `null` if the content is not a document. */
    val documentComponent: MediaFile?
        get() = mediaComponent?.takeIf { it.fileExtension.isDocument }

    /** The image, or `null` if the content is not an image. */
    val imageComponent: MediaFile?
        get() = mediaComponent?.takeIf { it.fileExtension.isImage }

    /** The media file, or `null` if the content is not media. */
    val mediaComponent: MediaFile?
        get() = (this as? Media)?.file

    /** The video, or `null` if the content is not a video. */
    val videoComponent: MediaFile?
        get() = mediaComponent?.takeIf { it.fileExtension.isVideo }

    // MARK: - Cases

    /** Audio content, as one or more audio references. */
    data class Audio(
        val components: List<AudioMessageReference>,
    ) : RichMessageContent

    /** Media content – an image, video, or document. */
    data class Media(
        val file: MediaFile,
    ) : RichMessageContent
}
