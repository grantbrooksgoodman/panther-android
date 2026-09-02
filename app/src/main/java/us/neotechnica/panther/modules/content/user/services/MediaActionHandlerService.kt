//
//  MediaActionHandlerService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 01/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.neotechnica.panther.modules.content.user.constants.MediaActionHandlerFloats
import us.neotechnica.panther.modules.content.user.constants.MediaActionHandlerStrings
import us.neotechnica.panther.networking.modules.common.models.DocumentFileExtension
import us.neotechnica.panther.networking.modules.common.models.ImageFileExtension
import us.neotechnica.panther.networking.modules.common.models.MediaFileExtension
import us.neotechnica.panther.networking.modules.common.models.VideoFileExtension
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import java.io.ByteArrayOutputStream

// MARK: - Constants Accessors

private typealias Floats = MediaActionHandlerFloats
private typealias Strings = MediaActionHandlerStrings

/**
 * Processes media selected from a content picker into a sendable
 * [MediaFile] – compressing images, staging videos and documents, and
 * generating thumbnails.
 *
 * **Note:** iOS transcodes videos with `AVAssetExportSession`; Android has
 * no equivalent, so videos are staged unchanged (see `DEVIATIONS.md`).
 * iOS generates a document thumbnail for any type with `QLThumbnailGenerator`;
 * Android renders PDF thumbnails only, sending other document types without
 * one. Sending the resulting media file lands with the send pipeline
 * (Phase R3.4).
 */
object MediaActionHandlerService {
    // MARK: - Properties

    private var appContext: Context? = null

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Process Image

    /**
     * Compresses the image at [uri] and stages it as a sendable media
     * file.
     *
     * @throws Exception if the image cannot be read or compressed.
     */
    suspend fun processImage(uri: Uri): MediaFile =
        withContext(Dispatchers.IO) { imageMediaFile(uri) }

    // MARK: - Process Video

    /**
     * Stages the video at [uri] and generates its thumbnail.
     *
     * @throws Exception if the video cannot be read or its thumbnail
     *   cannot be generated.
     */
    suspend fun processVideo(uri: Uri): MediaFile =
        withContext(Dispatchers.IO) {
            val fileExtension = MediaFileExtension.Video(VideoFileExtension.MP4)
            val relativePath = "${NetworkPath.media.rawValue}/${Strings.DEFAULT_VIDEO_NAME}.${fileExtension.rawValue}"
            copyToFile(uri, relativePath)
            writeThumbnail(videoThumbnail(uri), relativePath)
            MediaFile(relativePath, Strings.DEFAULT_VIDEO_NAME, fileExtension)
        }

    // MARK: - Process Document

    /**
     * Stages the document at [uri], routing image documents through
     * [processImage] and generating a PDF thumbnail where possible.
     *
     * @throws Exception if the file type cannot be determined or the file
     *   cannot be read.
     */
    suspend fun processDocument(uri: Uri): MediaFile =
        withContext(Dispatchers.IO) {
            val extension =
                displayName(uri)?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }
                    ?: throw failure("Failed to determine file type.")
            val fileExtension = MediaFileExtension.from(extension) ?: throw failure("Failed to determine file type.")
            if (fileExtension.isImage) return@withContext imageMediaFile(uri)

            val relativePath = "${NetworkPath.media.rawValue}/${Strings.DEFAULT_DOCUMENT_NAME}.${fileExtension.rawValue}"
            copyToFile(uri, relativePath)
            if (fileExtension.rawValue == DocumentFileExtension.Pdf.rawValue) {
                pdfThumbnail(relativePath)?.let { writeThumbnail(it, relativePath) }
            }
            MediaFile(relativePath, Strings.DEFAULT_DOCUMENT_NAME, fileExtension)
        }

    // MARK: - Auxiliary

    private fun imageMediaFile(uri: Uri): MediaFile {
        val bitmap =
            requireContext().contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
                ?: throw failure("Failed to process image data.")
        val fileExtension = MediaFileExtension.Image(ImageFileExtension.JPEG)
        val relativePath = "${NetworkPath.media.rawValue}/${Strings.DEFAULT_IMAGE_NAME}.${fileExtension.rawValue}"
        FileStore.write(relativePath, bitmap.jpegCompressedToKB(Floats.IMAGE_COMPRESSION_SIZE_KB))
            ?: throw failure("Failed to write image.")
        return MediaFile(relativePath, Strings.DEFAULT_IMAGE_NAME, fileExtension)
    }

    private fun copyToFile(
        uri: Uri,
        relativePath: String,
    ) {
        val destination = FileStore.resolve(relativePath) ?: throw failure("Failed to resolve local media path.")
        destination.parentFile?.mkdirs()
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: throw failure("Failed to read media.")
    }

    private fun writeThumbnail(
        thumbnail: Bitmap,
        mediaRelativePath: String,
    ) {
        val thumbnailPath = mediaRelativePath.substringBeforeLast('.') + MediaFile.THUMBNAIL_IMAGE_NAME_SUFFIX
        FileStore.write(thumbnailPath, thumbnail.jpegCompressedToKB(Floats.IMAGE_COMPRESSION_SIZE_KB))
            ?: throw failure("Failed to process thumbnail data.")
    }

    private fun videoThumbnail(uri: Uri): Bitmap {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(requireContext(), uri)
            retriever.getFrameAtTime(Floats.THUMBNAIL_FRAME_TIME_MICROSECONDS, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: throw failure("Failed to generate video thumbnail.")
        } finally {
            retriever.release()
        }
    }

    private fun pdfThumbnail(relativePath: String): Bitmap? =
        runCatching {
            val file = FileStore.resolve(relativePath) ?: return@runCatching null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    if (renderer.pageCount == 0) {
                        null
                    } else {
                        renderer.openPage(0).use { page ->
                            val target = Floats.THUMBNAIL_IMAGE_SIZE * Floats.THUMBNAIL_IMAGE_SCALE
                            val scale = minOf(target.toFloat() / page.width, target.toFloat() / page.height)
                            val bitmap =
                                Bitmap.createBitmap(
                                    (page.width * scale).toInt().coerceAtLeast(1),
                                    (page.height * scale).toInt().coerceAtLeast(1),
                                    Bitmap.Config.ARGB_8888,
                                )
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                    }
                }
            }
        }.getOrNull()

    private fun displayName(uri: Uri): String? =
        requireContext()
            .contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    private fun Bitmap.jpegCompressedToKB(targetKB: Int): ByteArray {
        var quality = INITIAL_QUALITY
        var data: ByteArray
        do {
            data =
                ByteArrayOutputStream().use { stream ->
                    compress(Bitmap.CompressFormat.JPEG, quality, stream)
                    stream.toByteArray()
                }
            quality -= QUALITY_STEP
        } while (data.size > targetKB * BYTES_PER_KB && quality >= QUALITY_STEP)
        return data
    }

    private fun requireContext(): Context =
        appContext ?: throw failure("Media action handler is not initialized.")

    private fun failure(message: String): Exception = Exception(message, metadata = ExceptionMetadata(this))

    // MARK: - Companion

    private const val INITIAL_QUALITY = 100
    private const val QUALITY_STEP = 5
    private const val BYTES_PER_KB = 1024
}
