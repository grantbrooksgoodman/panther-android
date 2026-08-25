//
//  MediaPreviewOverlay.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 24/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import android.content.Context
import android.content.Intent
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.modules.content.user.constants.MediaPreviewOverlayColors
import us.neotechnica.panther.modules.content.user.constants.MediaPreviewOverlayFloats
import us.neotechnica.panther.modules.content.user.constants.MediaPreviewOverlayStrings
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile

/**
 * A full-screen preview over a conversation's media, opened from a chat
 * bubble or the ChatInfo attachments list. Swipe to page through every
 * item, starting at [startIndex]: images are pinch-zoomable, videos play
 * inline, and documents open in an external viewer. Mirrors the iOS
 * `QuickViewer` media preview.
 *
 * @param mediaFiles The conversation's media, in display order.
 * @param startIndex The index of the item to show first.
 * @param onDismiss Dismisses the preview.
 */
@Composable
fun MediaPreviewOverlay(
    mediaFiles: List<MediaFile>,
    startIndex: Int,
    onDismiss: () -> Unit,
) {
    if (mediaFiles.isEmpty()) return
    BackHandler(onBack = onDismiss)

    val pagerState =
        rememberPagerState(initialPage = startIndex.coerceIn(0, mediaFiles.lastIndex)) { mediaFiles.size }

    Box(modifier = Modifier.fillMaxSize().background(MediaPreviewOverlayColors.background)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            MediaPage(mediaFiles[page])
        }

        CloseButton(
            onClick = onDismiss,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(MediaPreviewOverlayFloats.closeButtonPadding),
        )
    }
}

// MARK: - Page

@Composable
private fun MediaPage(mediaFile: MediaFile) {
    val fileExtension = mediaFile.fileExtension
    when {
        fileExtension.isImage -> ZoomableImage(mediaFile)
        fileExtension.isVideo -> VideoPlayer(mediaFile)
        else -> DocumentPage(mediaFile)
    }
}

// MARK: - Image

@Composable
private fun ZoomableImage(mediaFile: MediaFile) {
    val image =
        remember(mediaFile.relativePath) {
            MediaImageLoader.decode(mediaFile.localPathFile, MediaPreviewOverlayFloats.IMAGE_DECODE_MAX_DIMENSION)
        }
    var scale by remember { mutableFloatStateOf(MediaPreviewOverlayFloats.MIN_ZOOM) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState =
        rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(MediaPreviewOverlayFloats.MIN_ZOOM, MediaPreviewOverlayFloats.MAX_ZOOM)
            offset = if (scale > MediaPreviewOverlayFloats.MIN_ZOOM) offset + panChange else Offset.Zero
        }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = LocalizedStringKey.Image.localized(),
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        ).transformable(transformableState),
            )
        }
    }
}

// MARK: - Video

@Composable
private fun VideoPlayer(mediaFile: MediaFile) {
    val path = mediaFile.localPathFile?.path ?: return
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            VideoView(context).apply {
                setVideoPath(path)
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setOnPreparedListener { start() }
            }
        },
    )
}

// MARK: - Document

@Composable
private fun DocumentPage(mediaFile: MediaFile) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Components.Symbol(
            "doc.fill",
            color = MediaPreviewOverlayColors.foreground,
            modifier = Modifier.size(MediaPreviewOverlayFloats.documentGlyphSize),
        )
        Components.Text(
            "${mediaFile.name}.${mediaFile.fileExtension.rawValue}",
            color = MediaPreviewOverlayColors.foreground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.padding(
                    top = MediaPreviewOverlayFloats.documentSpacing,
                    start = MediaPreviewOverlayFloats.documentNameHorizontalPadding,
                    end = MediaPreviewOverlayFloats.documentNameHorizontalPadding,
                ),
        )
        Components.CapsuleButton(
            MediaPreviewOverlayStrings.OPEN,
            onClick = { openExternally(context, mediaFile) },
            primary = true,
            modifier = Modifier.padding(top = MediaPreviewOverlayFloats.documentSpacing),
        )
    }
}

// MARK: - Close Button

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(MediaPreviewOverlayFloats.closeButtonSize)
                .clip(CircleShape)
                .background(MediaPreviewOverlayColors.closeButtonBackground)
                .clickable(onClick = onClick),
    ) {
        Components.Symbol(
            "xmark",
            color = MediaPreviewOverlayColors.foreground,
            modifier = Modifier.size(MediaPreviewOverlayFloats.closeButtonGlyphSize),
        )
    }
}

// MARK: - Auxiliary

private fun openExternally(
    context: Context,
    mediaFile: MediaFile,
) {
    val file = mediaFile.localPathFile ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mediaFile.fileExtension.contentTypeString)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    runCatching { context.startActivity(Intent.createChooser(intent, null)) }
}
