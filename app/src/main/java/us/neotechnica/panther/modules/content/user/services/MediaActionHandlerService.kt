//
//  MediaActionHandlerService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import us.neotechnica.panther.bundle.media
import us.neotechnica.panther.modules.common.models.DocumentFileExtension
import us.neotechnica.panther.modules.common.models.ImageFileExtension
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.common.models.VideoFileExtension
import us.neotechnica.panther.modules.content.user.constants.MediaActionHandlerFloats
import us.neotechnica.panther.modules.content.user.constants.MediaActionHandlerStrings
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// MARK: - Constants Accessors

private typealias Floats = MediaActionHandlerFloats
private typealias Strings = MediaActionHandlerStrings

/**
 * Processes media selected from a content picker into a sendable
 * [MediaFile] – compressing images, staging videos and documents, and
 * generating thumbnails.
 *
 * **Note:** Videos are staged unchanged (there is no system
 * transcoder; see `DEVIATIONS.md`). PDF thumbnails are rendered;
 * other document types are sent without one. Sending the resulting
 * media file lands with the send pipeline.
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
    suspend fun processImage(uri: Uri): MediaFile = withContext(Dispatchers.IO) { imageMediaFile(uri) }

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
            val destination = FileStore.resolve(relativePath) ?: throw failure("Failed to resolve local media path.")

            runCatching { compressVideo(uri, destination) }
                .onFailure {
                    Logger.log("Failed to transcode video; staging unchanged. ${it.message}")
                    copyToFile(uri, relativePath)
                }

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

    // MARK: - Compress Image

    /**
     * Decodes the image at [uri] and returns its JPEG data compressed to
     * approximately [targetKB] kilobytes.
     *
     * @throws Exception if the image cannot be read.
     */
    suspend fun compressImageToKB(
        uri: Uri,
        targetKB: Int,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            val bitmap =
                requireContext().contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
                    ?: throw failure("Failed to process image data.")
            bitmap.jpegCompressedToKB(targetKB)
        }

    // MARK: - Save Media

    /**
     * Saves the given media file to the device's shared media
     * collections: images to Pictures, videos to Movies, and other files
     * to Downloads.
     *
     * @throws Exception if the file cannot be read or written.
     */
    suspend fun saveMedia(mediaFile: MediaFile): Unit =
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

    // MARK: - Auxiliary

    private fun downloadsContentUri(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

    @Suppress("LongParameterList")
    private fun writeToMediaStore(
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

    /**
     * Transcodes the video at [inputUri] to [outputFile] as an H.264/AAC
     * MP4, scaling the short side to a medium quality target.
     *
     * @throws Exception if the existing output cannot be removed.
     */
    @OptIn(UnstableApi::class)
    private suspend fun compressVideo(
        inputUri: Uri,
        outputFile: File,
    ) {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists() && !outputFile.delete()) {
            throw failure("Failed to remove existing video output.")
        }

        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val transformer =
                    Transformer
                        .Builder(requireContext())
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(
                            object : Transformer.Listener {
                                override fun onCompleted(
                                    composition: Composition,
                                    exportResult: ExportResult,
                                ) {
                                    if (continuation.isActive) continuation.resume(Unit)
                                }

                                override fun onError(
                                    composition: Composition,
                                    exportResult: ExportResult,
                                    exportException: ExportException,
                                ) {
                                    if (continuation.isActive) continuation.resumeWithException(exportException)
                                }
                            },
                        ).build()

                val editedMediaItem =
                    EditedMediaItem
                        .Builder(MediaItem.fromUri(inputUri))
                        .setEffects(
                            Effects(
                                emptyList<AudioProcessor>(),
                                listOf(Presentation.createForHeight(Floats.VIDEO_TARGET_HEIGHT)),
                            ),
                        ).build()

                transformer.start(editedMediaItem, outputFile.absolutePath)
            }
        }
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

    private fun requireContext(): Context = appContext ?: throw failure("Media action handler is not initialized.")

    private fun failure(message: String): Exception = Exception(message, metadata = ExceptionMetadata(this))

    // MARK: - Companion

    private const val INITIAL_QUALITY = 100
    private const val QUALITY_STEP = 5
    private const val BYTES_PER_KB = 1024
}
