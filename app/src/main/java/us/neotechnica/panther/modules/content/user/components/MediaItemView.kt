//
//  MediaItemView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageContextMenu
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.constants.StackFloats
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.services.sources.DocumentExportService
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.MediaItemViewColors
import us.neotechnica.panther.modules.content.user.constants.MediaItemViewFloats
import us.neotechnica.panther.modules.content.user.constants.MediaItemViewStrings
import us.neotechnica.panther.modules.content.user.constants.UserContentExtensionsStrings
import us.neotechnica.panther.modules.content.user.models.MediaItemViewData
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.MediaActionHandlerService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.MediaMessagePreviewService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.saveMedia
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Constants Accessors

private typealias MediaItemColors = MediaItemViewColors
private typealias MediaItemFloats = MediaItemViewFloats
private typealias MediaItemStrings = MediaItemViewStrings

/**
 * A row describing a media file in a conversation's media list.
 *
 * Use [MediaItemView] to display a media file's thumbnail, type,
 * sender, and timestamp. Tapping the row performs the given action; a
 * long press lifts the row and offers to save the file – images and
 * videos to the device's media collections, and other files through
 * the system document creator.
 *
 * @param metadata The display metadata for the item.
 * @param action The action performed when the user taps the row.
 */
@Composable
fun MediaItemView(
    metadata: MediaItemViewData,
    action: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var rowWidth by remember { mutableIntStateOf(0) }

    MessageContextMenu(
        actions =
            listOf(
                ContextMenuAction(
                    title = LocalizedStringKey.SaveFile.localized(),
                    systemImageName = MediaItemStrings.SAVE_ACTION_IMAGE_SYSTEM_NAME,
                ) { scope.launch { saveFile(metadata) } },
            ),
        alignment = ContextMenuAlignment.LEADING,
        liftScale = 0f,
        liftedBackground = colors.groupedRowBackground,
        minimumPressDuration = LocalViewConfiguration.current.longPressTimeoutMillis.milliseconds,
        onTap = { action() },
        modifier = Modifier.fillMaxWidth().onSizeChanged { rowWidth = it.width },
    ) {
        // The lifted copy keeps the row's measured width rather than
        // filling the host it is lifted into.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StackFloats.defaultSpacing),
            modifier =
                (if (rowWidth > 0) Modifier.width(with(density) { rowWidth.toDp() }) else Modifier.fillMaxWidth())
                    .padding(
                        horizontal = ChatInfoPageViewFloats.cardHorizontalPadding,
                        vertical = ChatInfoPageViewFloats.rowVerticalPadding,
                    ),
        ) {
            Thumbnail(metadata)

            Column {
                Components.Text(
                    metadata.mediaTypeLabelText,
                    foregroundColor = colors.titleText,
                    font = Font.systemSemibold(),
                )
                Components.Text(
                    metadata.senderLabelText,
                    foregroundColor = MediaItemColors.senderLabelForeground,
                    font = Font.system(FontScale.Small),
                    modifier = Modifier.padding(top = 1.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Components.Text(
                metadata.timestampLabelText,
                foregroundColor = MediaItemColors.timestampLabelForeground,
                font = Font.system(FontScale.Small),
            )
        }
    }
}

// MARK: - Thumbnail

@Composable
private fun Thumbnail(metadata: MediaItemViewData) {
    val file = metadata.file
    val image =
        remember(file.relativePath) {
            val source = if (file.fileExtension.isImage) file.localPathFile else file.thumbnailFile
            MediaImageLoader.decode(source, MediaItemFloats.THUMBNAIL_DECODE_MAX_DIMENSION)
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(width = MediaItemFloats.imageFrameWidth, height = MediaItemFloats.imageFrameHeight)
                .clip(RoundedCornerShape(MediaItemFloats.imageCornerRadius)),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = metadata.mediaTypeLabelText,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Components.Symbol(
                UserContentExtensionsStrings.UIImage.MISSING_IMAGE_SYSTEM_NAME,
                foregroundColor = MediaItemColors.missingImageForeground,
                usesIntrinsicSize = false,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// MARK: - Auxiliary

// Saves the item's file: images and videos to the device's media
// collections with a success HUD, other files through the document
// creator with a success HUD unless the user cancels. Failures surface
// as a toast.
private suspend fun saveFile(metadata: MediaItemViewData) {
    val file = metadata.file
    try {
        // An image that cannot be decoded exports as a document instead.
        val isDecodableImage = file.fileExtension.isImage && MediaMessagePreviewService.image(file) != null
        if (isDecodableImage || file.fileExtension.isVideo) {
            MediaActionHandlerService.saveMedia(file)
            HUD.showSuccess()
        } else {
            val localFile =
                file.localPathFile
                    ?: throw Exception("Failed to resolve local media path.", metadata = ExceptionMetadata(metadata))
            DocumentExportService.presentExportController(forFileAt = localFile)
            DocumentExportService.onDismiss { cancelled ->
                if (!cancelled) HUD.showSuccess()
            }
        }
    } catch (exception: Exception) {
        Logger.log(exception, with = AlertType.toast)
    }
}
