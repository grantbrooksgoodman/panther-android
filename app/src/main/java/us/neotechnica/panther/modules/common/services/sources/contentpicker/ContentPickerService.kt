//
//  ContentPickerService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources.contentpicker

import android.content.Context

/**
 * The content pickers available for a media message: camera
 * capture, the document picker, and the system photo and video
 * picker.
 *
 * A host registers each picker's launcher from its composition
 * through `RegisterContentPickerLaunchers`; the media action
 * handler presents them.
 */
object ContentPickerService {
    /** The picker that captures a photo with the camera. */
    val camera = CameraPickerService()

    /** The picker that selects a document. */
    val document = DocumentPickerService()

    /** The picker that selects a photo or video from the library. */
    val media = MediaPickerService()

    /** Prepares the pickers with the application context. */
    fun initialize(context: Context) {
        media.initialize(context)
    }
}
