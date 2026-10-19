//
//  MediaActionHandlerService+SaveMedia.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.neotechnica.panther.modules.common.models.MediaFile
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.PermissionService.PermissionStatus
import us.neotechnica.panther.modules.common.services.sources.PermissionService.PermissionType
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import java.io.File

/**
 * Saves the given media file to the device's shared media
 * collections: images to Pictures, videos to Movies, and other files
 * to Downloads.
 *
 * On Android 9 the save needs the storage permission; this method
 * requests it first and, if the user refuses, prompts them to grant
 * it in Settings.
 *
 * @return `true` if the file was saved; otherwise, `false` if the
 *   storage permission was refused.
 * @throws Exception if the file cannot be read or written.
 */
suspend fun MediaActionHandlerService.saveMedia(mediaFile: MediaFile): Boolean {
    if (!ensureStoragePermission()) return false
    writeToMediaCollection(mediaFile)
    return true
}

// MARK: - Auxiliary

// Requests the storage permission Android 9 needs before writing to
// the media store, prompting for Settings when it is refused.
private suspend fun ensureStoragePermission(): Boolean {
    val permissionService = DependencyValues.current.commonServices.permission
    if (permissionService.storagePermissionStatus == PermissionStatus.GRANTED) return true
    if (permissionService.requestPermission(PermissionType.STORAGE) == PermissionStatus.GRANTED) return true
    permissionService.presentCTA(PermissionType.STORAGE)
    return false
}

private suspend fun MediaActionHandlerService.writeToMediaCollection(mediaFile: MediaFile): Unit =
    withContext(Dispatchers.IO) {
        val source = mediaFile.localPathFile ?: throw failure("Failed to resolve local media path.")
        val extension = mediaFile.fileExtension
        val collection =
            when {
                extension.isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                extension.isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> downloadsContentUri()
            }
        val directory =
            when {
                extension.isImage -> Environment.DIRECTORY_PICTURES
                extension.isVideo -> Environment.DIRECTORY_MOVIES
                else -> Environment.DIRECTORY_DOWNLOADS
            }
        writeToMediaStore(
            source = source,
            displayName = "${mediaFile.name}.${extension.rawValue}",
            mimeType = extension.contentTypeString,
            collection = collection,
            directory = directory,
        )
    }

private fun downloadsContentUri(): Uri =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Files.getContentUri("external")
    }

@Suppress("LongParameterList")
private fun MediaActionHandlerService.writeToMediaStore(
    source: File,
    displayName: String,
    mimeType: String,
    collection: Uri,
    directory: String,
) {
    val resolver = requireContext().contentResolver
    val values =
        ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val publicDirectory = Environment.getExternalStoragePublicDirectory(directory)
                if (!publicDirectory.exists()) publicDirectory.mkdirs()
                put(MediaStore.MediaColumns.DATA, File(publicDirectory, displayName).absolutePath)
            }
        }

    val uri =
        runCatching { resolver.insert(collection, values) }.getOrNull()
            ?: throw failure("Failed to save media.")
    val output = resolver.openOutputStream(uri) ?: throw failure("Failed to write media.")
    runCatching { output.use { stream -> source.inputStream().use { it.copyTo(stream) } } }
        .getOrElse { throw failure("Failed to write media.") }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
}
