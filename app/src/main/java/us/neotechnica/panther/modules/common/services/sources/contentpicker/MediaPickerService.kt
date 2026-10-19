//
//  MediaPickerService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources.contentpicker

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.modules.common.models.ContentPickerResult
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceStrings
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.Timeout
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// MARK: - Constants Accessors

private typealias Strings = ChatPageViewServiceStrings.MediaActionHandler

/**
 * Use [MediaPickerService] to choose a photo or video from the user's
 * photo library.
 *
 * Call [present] to show the picker, and register a dismissal handler
 * with [onDismiss] to receive the selected item. Selected items are
 * copied to the app's temporary directory before delivery.
 */
class MediaPickerService : ContentPickerSource() {
    // MARK: - Types

    /** The facilities the confirmation and loading flow uses. */
    internal class Dependencies(
        val hideHUD: () -> Unit = { HUD.hide() },
        val loadMedia: suspend (ContentPickerResult) -> ContentPickerResult,
        val presentActionSheet: suspend (ActionSheet, List<ActionSheet.TranslationOptionKey>) -> Unit = { actionSheet, keys ->
            actionSheet.present(translating = keys)
        },
        val showProgressHUD: () -> Unit = { HUD.showProgress(isModal = true) },
    )

    // MARK: - Properties

    private var appContext: Context? = null
    private var dependencies: Dependencies? = null
    private var timeout: Timeout? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Initialization

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /** Replaces the facilities the confirmation and loading flow uses. */
    internal fun setDependencies(dependencies: Dependencies?) {
        this.dependencies = dependencies
    }

    // MARK: - Handle Result

    /**
     * Presents a confirmation action sheet for the selected item, then
     * loads it and delivers the result to the dismissal handler.
     *
     * A modal progress indicator appears if loading takes longer than
     * one second. The system picker has already closed when the
     * selection arrives, so cancelling the confirmation delivers a
     * cancellation rather than returning to the picker.
     *
     * @param result The selection, or `null` if the user canceled.
     */
    override fun handleResult(result: ContentPickerResult?) {
        val selection = result ?: return deliver(null)

        val confirmAction =
            Action("Confirm", style = ActionStyle.PREFERRED) {
                Task.delayed(by = PICKER_DISMISSAL_DELAY) { load(selection) }
            }
        val cancelAction = Action(LocalizedStringKey.Cancel.localized(), style = ActionStyle.CANCEL) { deliver(null) }

        scope.launch {
            resolvedDependencies().presentActionSheet(
                ActionSheet(actions = listOf(confirmAction, cancelAction)),
                listOf(ActionSheet.TranslationOptionKey.Actions(listOf(confirmAction))),
            )
        }
    }

    // MARK: - Auxiliary

    private fun deliver(result: ContentPickerResult?) {
        super.handleResult(result)
    }

    private fun dismissReturningFailure(exception: Exception) {
        timeout?.cancel()
        resolvedDependencies().hideHUD()
        handleFailure(exception)
    }

    private suspend fun load(selection: ContentPickerResult) {
        val dependencies = resolvedDependencies()
        timeout = Timeout(after = PROGRESS_TIMEOUT) { dependencies.showProgressHUD() }

        val loaded =
            try {
                dependencies.loadMedia(selection)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                return withContext(Dispatchers.Main) {
                    dismissReturningFailure(Exception.from(throwable, metadata = ExceptionMetadata(this@MediaPickerService)))
                }
            }

        withContext(Dispatchers.Main) {
            timeout?.cancel()
            dependencies.hideHUD()
            deliver(loaded)
        }
    }

    // Copies the selected item into the temporary directory, so the
    // delivered item is a local file whatever the picker's content
    // provider backs it with.
    private suspend fun copyToTemporaryDirectory(selection: ContentPickerResult): ContentPickerResult =
        withContext(Dispatchers.IO) {
            val context = appContext ?: throw failure("Media picker is not initialized.")
            val directory = FileStore.temporaryDirectories.firstOrNull() ?: context.cacheDir

            val (uri, name) =
                when (selection) {
                    is ContentPickerResult.Document -> selection.uri to Strings.DEFAULT_DOCUMENT_NAME
                    is ContentPickerResult.Image -> selection.uri to Strings.DEFAULT_IMAGE_NAME
                    is ContentPickerResult.Video -> selection.uri to Strings.DEFAULT_VIDEO_NAME
                }

            val mimeType = context.contentResolver.getType(uri)
            val fileExtension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            val destination = File(directory, listOfNotNull(name, fileExtension).joinToString("."))
            destination.delete()

            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            } ?: throw failure("Failed to process media.")

            val fileUri = Uri.fromFile(destination)
            when (selection) {
                is ContentPickerResult.Document -> ContentPickerResult.Document(fileUri)
                is ContentPickerResult.Image -> ContentPickerResult.Image(fileUri)
                is ContentPickerResult.Video -> ContentPickerResult.Video(fileUri)
            }
        }

    private fun failure(message: String): Exception = Exception(message, metadata = ExceptionMetadata(this))

    private fun resolvedDependencies(): Dependencies =
        dependencies ?: Dependencies(loadMedia = ::copyToTemporaryDirectory).also { dependencies = it }

    // MARK: - Companion

    private companion object {
        val PICKER_DISMISSAL_DELAY = 250.milliseconds
        val PROGRESS_TIMEOUT = 1.seconds
    }
}
