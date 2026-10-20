//
//  MediaActionHandlerService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

@file:Suppress("FileLength", "LargeClass")

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.annotation.OptIn
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.bundle.media
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.modules.common.models.ContentPickerResult
import us.neotechnica.panther.modules.common.models.DocumentFileExtension
import us.neotechnica.panther.modules.common.models.ImageFileExtension
import us.neotechnica.panther.modules.common.models.MediaFile
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.common.models.VideoFileExtension
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.HapticsService
import us.neotechnica.panther.modules.common.services.sources.contentpicker.ContentPickerService
import us.neotechnica.panther.modules.common.services.sources.contentpicker.ContentPickerSource
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceStrings
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// MARK: - Constants Accessors

private typealias Floats = ChatPageViewServiceFloats.MediaActionHandler
private typealias Strings = ChatPageViewServiceStrings.MediaActionHandler

/**
 * The service that handles media attachment actions.
 *
 * [MediaActionHandlerService] presents the attach-media action
 * sheet, then, based on the user's choice, presents the camera,
 * document, or photo-and-video picker. It processes the selected
 * item – compressing images and videos and generating thumbnails
 * where needed – and sends it as a media message.
 *
 * **Note:** PDF thumbnails are rendered; other document types are
 * sent without one.
 */
object MediaActionHandlerService {
    // MARK: - Types

    /** The transcoder the video path runs. */
    internal class Dependencies(
        val compressVideo: suspend (Uri, File) -> Unit = ::compressVideo,
    )

    // MARK: - Dependencies

    private val conversationSession: ConversationSessionService by Dependency { it.clientSession.entity.conversation }
    private val hapticsService: HapticsService by Dependency { it.commonServices.haptics }

    // MARK: - Properties

    private var appContext: Context? = null
    private var dependencies: Dependencies? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * A Boolean value that indicates whether a content picker is
     * currently presented.
     */
    var isPresentingPickerController = false
        private set

    /**
     * A hook run after a picked item is sent as a media message, with
     * the conversation it was sent to. The new chat page uses it to
     * navigate into the conversation its first message created.
     */
    var onMediaMessageSent: ((Conversation) -> Unit)? = null

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /** Replaces the transcoder the video path runs. */
    internal fun setDependencies(dependencies: Dependencies?) {
        this.dependencies = dependencies
    }

    // MARK: - Attach Media Button Tapped

    /**
     * Presents the attach-media action sheet.
     *
     * The sheet offers to take a photo, select a document, or
     * select a photo or video; choosing an option presents the
     * corresponding picker.
     */
    fun attachMediaButtonTapped() {
        hapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)

        val takePhotoAction = Action("Take photo") { presentCameraPicker() }
        val selectDocumentAction = Action("Select document") { presentDocumentPicker() }
        val selectPhotoOrVideoAction = Action("Select photo or video") { presentMediaPicker() }

        scope.launch {
            ActionSheet(
                title = "Attach media",
                actions = listOf(takePhotoAction, selectDocumentAction, selectPhotoOrVideoAction),
                cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            ).present(
                translating =
                    listOf(
                        ActionSheet.TranslationOptionKey.Title,
                        ActionSheet.TranslationOptionKey.Actions(),
                    ),
            )
        }
    }

    // MARK: - Process & Send Document

    private suspend fun processAndSendDocument(uri: Uri) {
        val fileExtension =
            displayName(uri)?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }
                ?: throw failure("Failed to determine file type.")
        val mediaFileExtension = MediaFileExtension.from(fileExtension) ?: throw failure("Failed to determine file type.")

        // An unknown extension resolves to a plain-text document, so
        // confirm the file truly conforms to plain text before sending it.
        if (mediaFileExtension is MediaFileExtension.Document && mediaFileExtension.fileExtension is DocumentFileExtension.PlainText) {
            val mimeType = requireContext().contentResolver.getType(uri).orEmpty()
            if (!mimeType.startsWith(PLAIN_TEXT_MIME_TYPE_PREFIX)) throw failure("Failed to determine file type.")
        }

        if (mediaFileExtension.isImage) return processAndSendImage(uri)
        if (mediaFileExtension.isVideo) return processAndSendVideo(uri)

        val mediaFile = withContext(Dispatchers.IO) { documentMediaFile(uri, mediaFileExtension) }
        sendMediaMessage(mediaFile)
    }

    // MARK: - Process & Send Image

    private suspend fun processAndSendImage(uri: Uri) {
        val mediaFile = withContext(Dispatchers.IO) { imageMediaFile(uri) }
        sendMediaMessage(mediaFile)
    }

    // MARK: - Process & Send Video

    private suspend fun processAndSendVideo(uri: Uri) {
        val mediaFile = processVideo(uri)
        sendMediaMessage(mediaFile)
    }

    // MARK: - Process Video

    /**
     * Transcodes the video at [uri] into place and generates its
     * thumbnail.
     *
     * @throws Exception if the video cannot be transcoded or its
     *   thumbnail cannot be generated.
     */
    suspend fun processVideo(uri: Uri): MediaFile =
        withContext(Dispatchers.IO) {
            val fileExtension = MediaFileExtension.Video(VideoFileExtension.MP4)
            val relativePath = "${NetworkPath.media.rawValue}/${Strings.DEFAULT_VIDEO_NAME}.${fileExtension.rawValue}"
            val destination = FileStore.resolve(relativePath) ?: throw failure("Failed to resolve local media path.")

            try {
                resolvedDependencies().compressVideo(uri, destination)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                throw exception
            } catch (throwable: Throwable) {
                throw Exception.from(throwable, metadata = ExceptionMetadata(this@MediaActionHandlerService))
            }

            writeThumbnail(videoThumbnail(uri), relativePath)
            MediaFile(relativePath, Strings.DEFAULT_VIDEO_NAME, fileExtension)
        }

    // MARK: - Compress Image

    /**
     * Decodes the image at [uri] and returns its JPEG data
     * compressed to approximately [targetKB] kilobytes.
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

    // MARK: - Auxiliary

    private fun documentMediaFile(
        uri: Uri,
        fileExtension: MediaFileExtension,
    ): MediaFile {
        val relativePath = "${NetworkPath.media.rawValue}/${Strings.DEFAULT_DOCUMENT_NAME}.${fileExtension.rawValue}"
        copyToFile(uri, relativePath)
        if (fileExtension.rawValue == DocumentFileExtension.Pdf.rawValue) {
            pdfThumbnail(relativePath)?.let { writeThumbnail(it, relativePath) }
        }
        return MediaFile(relativePath, Strings.DEFAULT_DOCUMENT_NAME, fileExtension)
    }

    private suspend fun sendMediaMessage(mediaFile: MediaFile) {
        MessageDeliveryService.sendMediaMessage(mediaFile)
        conversationSession.currentConversation?.let { conversation -> onMediaMessageSent?.invoke(conversation) }
    }

    private suspend fun onContentPickerDismissed(callback: Result<ContentPickerResult>?) {
        val result = callback ?: return
        val value = result.getOrElse { throw it as? Exception ?: failure("Failed to process media.") }
        when (value) {
            is ContentPickerResult.Document -> processAndSendDocument(value.uri)
            is ContentPickerResult.Image -> processAndSendImage(value.uri)
            is ContentPickerResult.Video -> processAndSendVideo(value.uri)
        }
    }

    private fun presentCameraPicker() = presentPicker(ContentPickerService.camera)

    private fun presentDocumentPicker() = presentPicker(ContentPickerService.document)

    private fun presentMediaPicker() = presentPicker(ContentPickerService.media)

    private fun presentPicker(picker: ContentPickerSource) {
        picker.present()
        isPresentingPickerController = true

        picker.onDismiss { result ->
            scope.launch {
                isPresentingPickerController = false
                try {
                    onContentPickerDismissed(result)
                } catch (exception: Exception) {
                    Logger.log(exception, with = AlertType.toast)
                }
            }
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
     * Transcodes the video at [inputUri] to [outputFile] as an
     * H.264/AAC MP4, scaling the short side to a medium quality
     * target.
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
                            val target = Floats.THUMBNAIL_IMAGE_SIZE_WIDTH * Floats.THUMBNAIL_IMAGE_SCALE
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

    private fun Bitmap.jpegCompressedToKB(targetKB: Int): ByteArray =
        jpegDataCompressedToKB(targetKB) { quality ->
            ByteArrayOutputStream().use { stream ->
                compress(Bitmap.CompressFormat.JPEG, quality, stream)
                stream.toByteArray()
            }
        }

    /**
     * Returns JPEG data within the tolerated margin of the given
     * size, lowering the quality step by step.
     *
     * @param targetKB The target size, in kilobytes.
     * @param compress Encodes the image at the given JPEG
     *   quality.
     *
     * @throws Exception if no quality meets the target.
     */
    internal fun jpegDataCompressedToKB(
        targetKB: Int,
        compress: (quality: Int) -> ByteArray,
    ): ByteArray {
        val targetBytes = targetKB * BYTES_PER_KB
        val toleratedBytes = (targetBytes * (1 + TOLERATED_MARGIN_OF_ERROR)).toLong()

        var quality = INITIAL_QUALITY
        var data: ByteArray
        do {
            data = compress(quality)
            quality -= QUALITY_STEP
        } while (data.size > targetBytes && quality >= QUALITY_STEP)

        if (data.size > toleratedBytes) throw failure("Failed to compress image.")
        return data
    }

    private fun resolvedDependencies(): Dependencies = dependencies ?: Dependencies().also { dependencies = it }

    internal fun requireContext(): Context = appContext ?: throw failure("Media action handler is not initialized.")

    internal fun failure(message: String): Exception = Exception(message, metadata = ExceptionMetadata(this))

    // MARK: - Companion

    private const val INITIAL_QUALITY = 100
    private const val PLAIN_TEXT_MIME_TYPE_PREFIX = "text/"
    private const val QUALITY_STEP = 5
    private const val BYTES_PER_KB = 1024
    private const val TOLERATED_MARGIN_OF_ERROR = 0.2
}
