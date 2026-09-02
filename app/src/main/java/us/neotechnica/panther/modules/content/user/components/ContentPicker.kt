//
//  ContentPicker.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 01/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import us.neotechnica.panther.modules.content.user.constants.MediaActionHandlerStrings
import us.neotechnica.panther.modules.content.user.services.MediaActionHandlerService
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import java.io.File

/**
 * The content pickers available for a media message: the system photo and
 * video picker, camera capture, and the document picker.
 *
 * @property launchPhotoOrVideo Presents the system photo and video picker.
 * @property launchCamera Presents the camera to capture a photo.
 * @property launchDocument Presents the document picker.
 */
class ContentPickers(
    val launchPhotoOrVideo: () -> Unit,
    val launchCamera: () -> Unit,
    val launchDocument: () -> Unit,
)

/**
 * Remembers the content pickers, processing each selection into a
 * [MediaFile] through [MediaActionHandlerService].
 *
 * Standing in for the iOS `ContentPickerView` flows, this maps photo and
 * video selection to the system Photo Picker (`PickVisualMedia`), camera
 * capture to a `FileProvider`-backed capture intent, and document
 * selection to the Storage Access Framework.
 *
 * @param onPicked Invoked with the processed media file on selection.
 * @param onFailed Invoked with the resulting `Exception` on failure.
 *
 * @return The launchers for the three sources.
 */
@Composable
fun rememberContentPickers(
    onPicked: (MediaFile) -> Unit,
    onFailed: (Exception) -> Unit,
): ContentPickers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun process(operation: suspend () -> MediaFile) {
        scope.launch {
            try {
                onPicked(operation())
            } catch (exception: Exception) {
                onFailed(exception)
            }
        }
    }

    val mediaLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
            process { if (isVideo) MediaActionHandlerService.processVideo(uri) else MediaActionHandlerService.processImage(uri) }
        }

    val documentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            process { MediaActionHandlerService.processDocument(uri) }
        }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraUri
            if (success && uri != null) process { MediaActionHandlerService.processImage(uri) }
        }

    return ContentPickers(
        launchPhotoOrVideo = {
            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        },
        launchCamera = {
            val uri = captureUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        },
        launchDocument = {
            documentLauncher.launch(arrayOf(WILDCARD_MIME_TYPE))
        },
    )
}

private fun captureUri(context: Context): Uri {
    val file = File(context.filesDir, "$MEDIA_DIRECTORY/${MediaActionHandlerStrings.DEFAULT_IMAGE_NAME}-capture.jpg")
    file.parentFile?.mkdirs()
    if (!file.exists()) file.createNewFile()
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private const val MEDIA_DIRECTORY = "media"
private const val WILDCARD_MIME_TYPE = "*/*"
