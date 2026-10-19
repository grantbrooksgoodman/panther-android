//
//  ChatMessageCell.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.LocalContextMenuController
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageContextMenu
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewStrings
import us.neotechnica.panther.modules.content.user.constants.UserContentExtensionsColors
import us.neotechnica.panther.modules.content.user.dependencies.chatPageViewService
import us.neotechnica.panther.modules.content.user.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.extensions.isOutboxMessage
import us.neotechnica.panther.modules.content.user.extensions.isPlayingMessage
import us.neotechnica.panther.modules.content.user.extensions.isSpeakingMessage
import us.neotechnica.panther.modules.content.user.extensions.isSystemMessage
import us.neotechnica.panther.modules.content.user.extensions.kind
import us.neotechnica.panther.modules.content.user.models.MessageKind
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.AudioMessagePlaybackService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.MediaMessagePreviewService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Constants Accessors

private typealias CellDataSourceColors = ChatPageViewColors.MessagesDataSource
private typealias CellDataSourceStrings = ChatPageViewStrings.MessagesDataSource
private typealias CellDisplayFloats = ChatPageViewFloats.MessagesDisplayDelegate
private typealias CellLayoutFloats = ChatPageViewFloats.MessagesLayoutDelegate

// MARK: - Dependencies

private val audioMessagePlaybackService: AudioMessagePlaybackService
    get() = DependencyValues.current.chatPageViewService.audioMessagePlayback

/**
 * A single chat row: an optional day separator, the message bubble
 * (with a long-press context menu), and its bottom label – reaction
 * chips, the alternate-content language, or the delivery status.
 * System messages render as a centered, bubble-less activity cell.
 *
 * @param row The row's display inputs.
 * @param onReact Applies the given reaction style to the given message.
 * @param onFailedIndicatorTapped Presents the retry/delete action sheet
 *   for the given failed outbox message ID.
 */
@Composable
fun ChatMessageCell(
    row: ChatMessageRowData,
    onReact: (Message, Reaction.Style) -> Unit,
    onFailedIndicatorTapped: (String) -> Unit,
) {
    val message = row.message
    if (message.isSystemMessage) {
        SystemMessageCell(message)
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CellLayoutFloats.rowHorizontalPadding,
                    vertical = CellLayoutFloats.rowVerticalPadding,
                ),
    ) {
        cellTopLabelAttributedText(row)?.let { label ->
            val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
            Box(
                modifier = Modifier.fillMaxWidth().height(CellLayoutFloats.cellTopLabelHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color =
                        if (isDarkModeActive) {
                            UserContentExtensionsColors.Date.chatPageMessageSeparatorAttributedDateStringStandardAttributesDarkForeground
                        } else {
                            UserContentExtensionsColors.Date.chatPageMessageSeparatorAttributedDateStringStandardAttributesLightForeground
                        },
                    textAlign = TextAlign.Center,
                )
            }
        }

        MessageContent(
            row = row,
            onReact = onReact,
            onFailedIndicatorTapped = onFailedIndicatorTapped,
        )

        BottomLabel(row = row, isOwn = message.isFromCurrentUser)
    }
}

/**
 * The message's bubble (media, audio, or text) with its long-press
 * context menu, preceded by the sender's avatar in received group
 * messages.
 */
@Composable
private fun MessageContent(
    row: ChatMessageRowData,
    onReact: (Message, Reaction.Style) -> Unit,
    onFailedIndicatorTapped: (String) -> Unit,
) {
    val controller = LocalContextMenuController.current
    val message = row.message
    val isOwn = message.isFromCurrentUser
    val kind = message.kind

    // Mock and outbox messages carry no menu, and nothing attaches while
    // a send is in flight; the double-tap default goes with them.
    val isSendingMessage by MessageDeliveryService.isSendingMessage.collectAsState()
    val hasContextMenu = !message.isMock && !message.isOutboxMessage && !isSendingMessage
    val reactionChoices = if (hasContextMenu) reactionChoicesFor(message, row.reactions, onReact) else emptyList()
    val alignment = if (isOwn) ContextMenuAlignment.TRAILING else ContextMenuAlignment.LEADING
    val messageLabel = remember { MessageLabel() }

    val isSpeakingThisMessage = message.isSpeakingMessage
    val speakingHighlightRange = if (isSpeakingThisMessage) DependencyValues.current.commonServices.audio.textToSpeech.spokenRange else null
    val isMedia = kind is MessageKind.Photo || kind is MessageKind.Video
    val isText = kind is MessageKind.Text || kind is MessageKind.AttributedText

    Row(
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (row.isGroup && !isOwn) SenderAvatar(row)
        MessageContextMenu(
            actions = if (hasContextMenu) contextMenuActions(message) else emptyList(),
            alignment = alignment,
            reactionChoices = reactionChoices,
            alignsMenuCardToLeadingEdge = true,
            menuKey = message.id,
            // Tapping anywhere in a failed bubble presents the retry sheet;
            // tapping a media bubble opens the preview; tapping a text
            // bubble selects the detected attribute under the touch.
            onTap =
                when {
                    row.isFailed -> {
                        { onFailedIndicatorTapped(message.id) }
                    }

                    isMedia -> {
                        { MediaMessagePreviewService.didTapImage(message, controller?.isPresenting == true) }
                    }

                    isText -> messageLabel::handleGesture

                    else -> null
                },
            header = { SenderNameLabel(row) },
        ) {
            MessageKindContent(
                row = row,
                kind = kind,
                label = messageLabel,
                isSpeaking = isSpeakingThisMessage,
                highlightRange = speakingHighlightRange,
                isPresentingContextMenu = controller?.isPresenting == true,
            )
        }

        if (row.isFailed) {
            FailedOutboxIndicator(
                onTap = { onFailedIndicatorTapped(message.id) },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

// The bubble for the message's kind: audio, attributed or plain text,
// photo, or video.
@Composable
@Suppress("LongParameterList")
private fun MessageKindContent(
    row: ChatMessageRowData,
    kind: MessageKind,
    label: MessageLabel,
    isSpeaking: Boolean,
    highlightRange: IntRange?,
    isPresentingContextMenu: Boolean,
) {
    val message = row.message
    val isOwn = message.isFromCurrentUser
    val isPlayingThisMessage = message.isPlayingMessage

    when (kind) {
        is MessageKind.Audio ->
            AudioMessageBubble(
                item = kind.item,
                isOwn = isOwn,
                isPlaying = isPlayingThisMessage,
                progress = if (isPlayingThisMessage) audioMessagePlaybackService.progress else 0f,
                onPlay = { message.audioComponent?.let { audioMessagePlaybackService.didTapPlayButton(message, it) } },
            )

        is MessageKind.AttributedText ->
            MessageBubble(
                text = kind.text,
                messageID = message.id,
                label = label,
                isOwn = isOwn,
                isItalicized = kind.isItalicized,
                isSpeaking = isSpeaking,
                highlightRange = highlightRange,
            )

        is MessageKind.Text ->
            MessageBubble(text = kind.text, messageID = message.id, label = label, isOwn = isOwn, isItalicized = false)

        is MessageKind.Photo, is MessageKind.Video ->
            MediaMessageBubble(
                mediaFile = (kind as? MessageKind.Photo)?.item ?: (kind as MessageKind.Video).item,
                isOwn = isOwn,
                onPinch = { MediaMessagePreviewService.didTapImage(message, isPresentingContextMenu) },
            )

        MessageKind.Custom -> Unit
    }
}

/**
 * The label below a message bubble: its reaction chips (one per style,
 * with a count and the current user's own reaction highlighted) and the
 * bottom label text – the alternate-content language or the delivery
 * status – joined by a separator. Aligns to the message's side with
 * the outgoing or incoming insets.
 */
@Composable
private fun BottomLabel(
    row: ChatMessageRowData,
    isOwn: Boolean,
) {
    val chips = reactionChips(row.reactions)
    val label = cellBottomLabelAttributedText(row)
    if (chips.isEmpty() && label == null) return

    // Align the label to the message bubble, not the screen edge: group
    // received bubbles are indented past the sender avatar column.
    val leadingInset = if (row.isGroup && !isOwn) CellDisplayFloats.avatarSize + CellDisplayFloats.avatarSpacing else 0.dp

    // The bubble has no tail, so the label sits flush with the bubble's
    // leading or trailing edge.
    Row(
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = ChatPageViewFloats.messageOutgoingCellBottomLabelAlignmentTopTextInset,
                    start = leadingInset,
                ),
    ) {
        // Chips are spaced only between one another and before the label,
        // so the outermost chip aligns with the bubble's edge.
        Row(horizontalArrangement = Arrangement.spacedBy(CellDisplayFloats.reactionChipSpacing)) {
            chips.forEach { chip -> ReactionChipView(chip) }
        }

        if (label != null) {
            if (chips.isNotEmpty()) {
                Spacer(modifier = Modifier.width(CellDisplayFloats.reactionChipSpacing))
                Components.Text(
                    CellDataSourceStrings.BOTTOM_LABEL_COMPONENT_SEPARATOR,
                    foregroundColor = CellDataSourceColors.cellBottomLabelAttributedTextStandardAttributesForeground,
                    font = Font.system(FontScale.Small),
                )
            }
            Text(text = label)
        }
    }
}
