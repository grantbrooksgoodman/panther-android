//
//  ContentPickerSource.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources.contentpicker

import us.neotechnica.panther.modules.common.models.ContentPickerResult
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

/**
 * A content picker that a host presents through a registered launcher
 * and that reports its selection to a dismissal handler.
 *
 * Subclasses name the camera, document, and media pickers; the host
 * registers each picker's launcher from its composition.
 */
abstract class ContentPickerSource {
    // MARK: - Properties

    private var launcher: (() -> Unit)? = null
    private var onDismissHandler: ((Result<ContentPickerResult>?) -> Unit)? = null

    // MARK: - Computed Properties

    /** A Boolean value that indicates whether a launcher is registered. */
    val canPresent: Boolean
        get() = launcher != null

    // MARK: - Register Launcher

    /**
     * Registers the launcher that presents the picker.
     *
     * @param launch Presents the picker.
     */
    fun registerLauncher(launch: () -> Unit) {
        launcher = launch
    }

    /**
     * Clears the registered launcher, if it is the given one.
     *
     * @param launch The launcher to clear.
     */
    fun unregisterLauncher(launch: () -> Unit) {
        if (launcher === launch) launcher = null
    }

    // MARK: - Present

    /** Presents the picker through its registered launcher, if any. */
    fun present() {
        launcher?.invoke()
    }

    // MARK: - On Dismiss

    /**
     * Registers a handler to run when the picker is dismissed.
     *
     * The handler receives the selection, a failure, or `null` if the
     * user canceled. The handler is cleared after it runs.
     *
     * @param perform The handler to run.
     */
    fun onDismiss(perform: (Result<ContentPickerResult>?) -> Unit) {
        onDismissHandler = perform
    }

    // MARK: - Handle Result

    /**
     * Delivers the picker's result to the dismissal handler.
     *
     * @param result The selection, or `null` if the user canceled.
     */
    open fun handleResult(result: ContentPickerResult?) {
        val handler = onDismissHandler
        onDismissHandler = null
        handler?.invoke(result?.let { Result.success(it) })
    }

    /**
     * Delivers a failure to the dismissal handler.
     *
     * @param exception The failure.
     */
    fun handleFailure(exception: Exception) {
        val handler = onDismissHandler
        onDismissHandler = null
        handler?.invoke(Result.failure(exception))
    }
}

/** The picker that captures a photo with the camera. */
class CameraPickerService : ContentPickerSource()

/**
 * The picker that selects a document, restricted to JPEG and PNG
 * images, MP4 videos, PDFs, and plain text files.
 */
class DocumentPickerService : ContentPickerSource() {
    companion object {
        /** The MIME types the document picker offers. */
        val allowedMimeTypes = arrayOf("image/jpeg", "image/png", "video/mp4", "application/pdf", "text/plain")
    }
}
