//
//  DocumentExportService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.modules.common.extensions.toException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import java.io.File

/**
 * Use [DocumentExportService] to export a file with the system document
 * creator.
 *
 * Call [presentExportController] to show the picker, and register a
 * dismissal handler with [onDismiss] to respond when the export
 * completes. The file is exported from a temporary copy that is removed
 * on dismissal. A host registers the activity result launcher that
 * presents the picker through [registerLauncher].
 */
object DocumentExportService {
    // MARK: - Properties

    private var appContext: Context? = null
    private var launcher: ((Intent) -> Unit)? = null
    private var onDismissHandler: ((Boolean) -> Unit)? = null
    private var temporaryFile: File? = null

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Register Launcher

    /**
     * Registers the launcher that presents the document creator.
     *
     * @param launch Launches the given create-document intent.
     */
    fun registerLauncher(launch: (Intent) -> Unit) {
        launcher = launch
    }

    /**
     * Clears the registered launcher, if it is the given one.
     *
     * @param launch The launcher to clear.
     */
    fun unregisterLauncher(launch: (Intent) -> Unit) {
        if (launcher === launch) launcher = null
    }

    // MARK: - Present Export Controller

    /**
     * Presents the system document creator for exporting the given
     * file.
     *
     * The file is copied to the app's cache directory under a localized
     * default name before presentation; the destination the user
     * chooses receives a copy.
     *
     * @param forFileAt The file to export.
     *
     * @throws Exception if the file type cannot be determined, if no
     *   launcher is registered, or if the file cannot be copied.
     */
    fun presentExportController(forFileAt: File) {
        val fileExtension = forFileAt.name.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
        if (fileExtension == null) throw Exception("Failed to determine file type.", metadata = ExceptionMetadata(this))
        val context = appContext ?: throw Exception("Document export service is not initialized.", metadata = ExceptionMetadata(this))
        val launch = launcher ?: throw Exception("No document export launcher is registered.", metadata = ExceptionMetadata(this))

        val fileName = "${LocalizedStringKey.Document.localized().lowercase()}.$fileExtension"
        val temporaryFile = File(context.cacheDir, fileName)
        runCatching { forFileAt.copyTo(temporaryFile, overwrite = true) }
            .getOrElse { throw Exception("Failed to copy file to temporary directory.", metadata = ExceptionMetadata(this)) }
        this.temporaryFile = temporaryFile

        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase()) ?: DEFAULT_MIME_TYPE
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
        launch(intent)
    }

    // MARK: - On Dismiss

    /**
     * Registers a handler to run when the picker is dismissed.
     *
     * The handler receives `true` if the user canceled the export;
     * otherwise, `false`. The handler is cleared after it runs.
     * Registering a new handler replaces any existing one.
     *
     * @param perform The handler to run.
     */
    fun onDismiss(perform: (Boolean) -> Unit) {
        onDismissHandler = perform
    }

    // MARK: - Handle Result

    /**
     * Completes the export with the destination the user chose, then
     * delivers the dismissal to the registered handler.
     *
     * @param destination The chosen destination, or `null` if the user
     *   canceled.
     */
    fun handleResult(destination: Uri?) {
        val source = temporaryFile
        if (destination == null || source == null) return onDismiss(cancelled = true)

        // The export counts as completed only when the copy succeeds;
        // a failed copy is reported and treated as cancelled.
        val copied =
            runCatching {
                val output =
                    appContext?.contentResolver?.openOutputStream(destination)
                        ?: throw Exception("Failed to open export destination.", metadata = ExceptionMetadata(this))
                output.use { source.inputStream().use { input -> input.copyTo(it) } }
            }.onFailure { throwable ->
                Logger.log(throwable.toException())
            }.isSuccess

        onDismiss(cancelled = !copied)
    }

    // MARK: - Auxiliary

    private fun onDismiss(cancelled: Boolean) {
        val handler = onDismissHandler
        onDismissHandler = null
        temporaryFile?.delete()
        temporaryFile = null
        handler?.invoke(cancelled)
    }

    private const val DEFAULT_MIME_TYPE = "application/octet-stream"
}
