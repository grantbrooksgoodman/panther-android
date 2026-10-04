//
//  MediaItemViewData.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import us.neotechnica.panther.modules.networking.message.models.MediaFile

/**
 * The display inputs for one row in a conversation's shared-media list.
 *
 * @property file The media file the row describes.
 * @property mediaTypeLabelText The text the media-type label displays.
 * @property senderLabelText The text the sender label displays.
 * @property timestampLabelText The text the timestamp label displays.
 */
data class MediaItemViewData(
    val file: MediaFile,
    val mediaTypeLabelText: String,
    val senderLabelText: String,
    val timestampLabelText: String,
)
