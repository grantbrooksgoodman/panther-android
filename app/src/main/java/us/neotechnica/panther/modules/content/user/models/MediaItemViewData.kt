//
//  MediaItemViewData.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 24/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile

/**
 * The display inputs for one row in a conversation's shared-media list,
 * mirroring the iOS `MediaItemView.Metadata`.
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
