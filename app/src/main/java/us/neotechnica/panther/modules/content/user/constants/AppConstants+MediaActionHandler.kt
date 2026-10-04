//
//  AppConstants+MediaActionHandler.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

// MARK: - Float

object MediaActionHandlerFloats {
    const val IMAGE_COMPRESSION_SIZE_KB = 1000
    const val THUMBNAIL_IMAGE_SIZE = 500
    const val THUMBNAIL_IMAGE_SCALE = 2
    const val THUMBNAIL_FRAME_TIME_MICROSECONDS = 1_000_000L

    // A medium-quality transcode target: the video height is scaled to
    // 480 px (H.264 video, AAC audio), preserving
    // the aspect ratio, with the encoder's default medium bitrate.
    const val VIDEO_TARGET_HEIGHT = 480
}

// MARK: - String

object MediaActionHandlerStrings {
    const val DEFAULT_DOCUMENT_NAME = "document"
    const val DEFAULT_IMAGE_NAME = "image"
    const val DEFAULT_VIDEO_NAME = "video"
}
