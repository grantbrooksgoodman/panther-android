//
//  ContentPickerLaunchers.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.common.services.DocumentExportService
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceStrings
import us.neotechnica.panther.modules.content.user.models.ContentPickerResult
import us.neotechnica.panther.modules.content.user.services.contentpicker.ContentPickerService
import us.neotechnica.panther.modules.content.user.services.contentpicker.DocumentPickerService
import java.io.File

/**
 * Registers the activity result launchers that present the content
 * pickers and the document export picker for the duration of the
 * composition.
 *
 * Maps photo and video selection to the system Photo Picker, camera
 * capture to a `FileProvider`-backed capture intent, document selection
 * to the Storage Access Framework restricted to the supported types,
 * and document export to the system document creator.
 */
@Composable
fun RegisterContentPickerLaunchers() {
    val context = LocalContext.current

    val mediaLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            val result =
                uri?.let {
                    val isVideo = context.contentResolver.getType(it)?.startsWith(VIDEO_MIME_TYPE_PREFIX) == true
                    if (isVideo) ContentPickerResult.Video(it) else ContentPickerResult.Image(it)
                }
            ContentPickerService.media.handleResult(result)
        }

    val documentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            ContentPickerService.document.handleResult(uri?.let { documentResult(context, it) })
        }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraUri
            ContentPickerService.camera.handleResult(if (success && uri != null) ContentPickerResult.Image(uri) else null)
        }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            DocumentExportService.handleResult(result.data?.data)
        }

    DisposableEffect(Unit) {
        val launchMedia = {
            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
        val launchDocument = { documentLauncher.launch(DocumentPickerService.allowedMimeTypes) }
        val launchCamera = {
            val uri = captureUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
        val launchExport = { intent: Intent -> exportLauncher.launch(intent) }

        ContentPickerService.media.registerLauncher(launchMedia)
        ContentPickerService.document.registerLauncher(launchDocument)
        ContentPickerService.camera.registerLauncher(launchCamera)
        DocumentExportService.registerLauncher(launchExport)

        // Only this composition's launchers are cleared: a page that
        // registered its own launchers while this one was leaving keeps
        // them.
        onDispose {
            ContentPickerService.media.unregisterLauncher(launchMedia)
            ContentPickerService.document.unregisterLauncher(launchDocument)
            ContentPickerService.camera.unregisterLauncher(launchCamera)
            DocumentExportService.unregisterLauncher(launchExport)
        }
    }
}

// MARK: - Auxiliary

// A picked document resolves to an image, a video, or a document by its
// file extension.
private fun documentResult(
    context: Context,
    uri: Uri,
): ContentPickerResult {
    val mimeType = context.contentResolver.getType(uri).orEmpty()
    val fileExtension = MediaFileExtension.from(mimeType.substringAfterLast('/'))
    return when {
        mimeType.startsWith(IMAGE_MIME_TYPE_PREFIX) || fileExtension?.isImage == true -> ContentPickerResult.Image(uri)
        mimeType.startsWith(VIDEO_MIME_TYPE_PREFIX) || fileExtension?.isVideo == true -> ContentPickerResult.Video(uri)
        else -> ContentPickerResult.Document(uri)
    }
}

private fun captureUri(context: Context): Uri {
    val file =
        File(
            context.filesDir,
            "$MEDIA_DIRECTORY/${ChatPageViewServiceStrings.MediaActionHandler.DEFAULT_IMAGE_NAME}-capture.jpg",
        )
    file.parentFile?.mkdirs()
    if (!file.exists()) file.createNewFile()
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private const val IMAGE_MIME_TYPE_PREFIX = "image/"
private const val MEDIA_DIRECTORY = "media"
private const val VIDEO_MIME_TYPE_PREFIX = "video/"
