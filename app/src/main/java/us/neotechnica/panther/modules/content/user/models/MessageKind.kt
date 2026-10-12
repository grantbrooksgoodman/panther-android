//
//  MessageKind.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import us.neotechnica.panther.modules.networking.message.models.AudioFile
import us.neotechnica.panther.modules.networking.message.models.MediaFile

/**
 * A message's content, as represented for display in the message list.
 */
sealed interface MessageKind {
    /**
     * An audio message, playing the given file.
     *
     * @property item The audio file to play.
     */
    data class Audio(
        val item: AudioFile,
    ) : MessageKind

    /**
     * A text message rendered from an attributed string.
     *
     * @property text The text to render.
     * @property isItalicized Whether the text renders italicized, as
     *   alternate content does.
     */
    data class AttributedText(
        val text: String,
        val isItalicized: Boolean = false,
    ) : MessageKind

    /** A custom message with no renderable content, such as a system message. */
    data object Custom : MessageKind

    /**
     * A photo message, or a document rendered as a photo through its
     * thumbnail.
     *
     * @property item The media file to render.
     */
    data class Photo(
        val item: MediaFile,
    ) : MessageKind

    /**
     * A plain text message.
     *
     * @property text The text to render.
     */
    data class Text(
        val text: String,
    ) : MessageKind

    /**
     * A video message, rendering its thumbnail with a play affordance.
     *
     * @property item The media file to render.
     */
    data class Video(
        val item: MediaFile,
    ) : MessageKind
}
