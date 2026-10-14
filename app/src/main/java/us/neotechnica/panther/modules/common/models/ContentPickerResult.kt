//
//  ContentPickerResult.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import android.net.Uri

/** The item a content picker returned. */
sealed interface ContentPickerResult {
    /**
     * A document selected from the document picker.
     *
     * @property uri The document's content URI.
     */
    data class Document(
        val uri: Uri,
    ) : ContentPickerResult

    /**
     * An image captured by the camera or selected from the library.
     *
     * @property uri The image's content URI.
     */
    data class Image(
        val uri: Uri,
    ) : ContentPickerResult

    /**
     * A video selected from the library or the document picker.
     *
     * @property uri The video's content URI.
     */
    data class Video(
        val uri: Uri,
    ) : ContentPickerResult
}
