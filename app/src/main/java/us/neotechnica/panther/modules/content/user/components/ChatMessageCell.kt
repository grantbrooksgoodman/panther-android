//
//  ChatMessageCell.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageContextMenu
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellColors
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellFloats
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellStrings
import us.neotechnica.panther.modules.content.user.services.AudioMessagePlaybackService
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.isMediaMessage
import us.neotechnica.panther.modules.session.entity.extensions.isSystemMessage
import androidx.compose.material3.Text as Material3Text

/**
 * A single chat row: an optional day separator, the message bubble
 * (with a long-press context menu), and, for the last confirmed own
 * message in a one-to-one chat, a delivery status label.
 *
 * @param row The row's display inputs.
 * @param onToggleAlternate Toggles the alternate text for a message ID.
 * @param onToggleAudioTranscription Toggles an audio message's
 *   transcription for a message ID.
 * @param onTapMedia Opens the media preview for the given media message ID.
 * @param onReact Applies the given reaction style to the given message.
 * @param onSpeak Speaks the given displayed text for the given message ID.
 * @param onFailedIndicatorTapped Presents the retry/delete action sheet
 *   for the given failed outbox message ID.
 * @param onSaveMedia Saves the given media file to the device.
 */
@Composable
@Suppress("LongParameterList")
fun ChatMessageCell(
    row: ChatMessageRowData,
    onToggleAlternate: (String) -> Unit,
    onToggleAudioTranscription: (String) -> Unit,
    onTapMedia: (String) -> Unit,
    onReact: (Message, Reaction.Style) -> Unit,
    onSpeak: (String, String) -> Unit,
    onFailedIndicatorTapped: (String) -> Unit,
    onSaveMedia: (MediaFile) -> Unit,
) {
    val colors = LocalPantherColors.current
    val message = row.message

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ChatMessageCellFloats.rowHorizontalPadding,
                    vertical = ChatMessageCellFloats.rowVerticalPadding,
                ),
    ) {
        if (message.isSystemMessage) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = ChatMessageCellFloats.separatorVerticalPadding),
                contentAlignment = Alignment.Center,
            ) {
                Material3Text(
                    text = systemMessageString(row.translation?.output ?: "", message.sentDate),
                    color = colors.subtitleText,
                    style = Font.system(FontScale.Small).textStyle,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        separatorDate(message, row.previousMessage)?.let { date ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = ChatMessageCellFloats.separatorVerticalPadding),
                contentAlignment = Alignment.Center,
            ) {
                Material3Text(
                    text = separatorAnnotatedString(date),
                    color = colors.subtitleText,
                    style = Font.system(FontScale.Small).textStyle,
                    textAlign = TextAlign.Center,
                )
            }
        }

        MessageContent(
            row = row,
            onToggleAlternate = onToggleAlternate,
            onToggleAudioTranscription = onToggleAudioTranscription,
            onTapMedia = onTapMedia,
            onReact = onReact,
            onSpeak = onSpeak,
            onFailedIndicatorTapped = onFailedIndicatorTapped,
            onSaveMedia = onSaveMedia,
        )

        BottomLabel(row = row, isOwn = message.isFromCurrentUser)
    }
}

/**
 * The message's bubble (media, audio, or text) with its long-press
 * context menu, preceded by the sender's avatar in received group
 * messages. An audio message renders as its transcription bubble when
 * that alternate is displayed.
 */
@Composable
@Suppress("LongParameterList")
private fun MessageContent(
    row: ChatMessageRowData,
    onToggleAlternate: (String) -> Unit,
    onToggleAudioTranscription: (String) -> Unit,
    onTapMedia: (String) -> Unit,
    onReact: (Message, Reaction.Style) -> Unit,
    onSpeak: (String, String) -> Unit,
    onFailedIndicatorTapped: (String) -> Unit,
    onSaveMedia: (MediaFile) -> Unit,
) {
    val colors = LocalPantherColors.current
    val clipboard = LocalClipboardManager.current
    val message = row.message
    val isOwn = message.isFromCurrentUser
    val displayText = displayText(row)
    val reactionChoices = reactionChoicesFor(row, onReact)
    val alignment = if (isOwn) ContextMenuAlignment.TRAILING else ContextMenuAlignment.LEADING

    val isSpeakingThisMessage = TextToSpeechService.speakingMessageID == message.id
    val speakingHighlightRange = if (isSpeakingThisMessage) TextToSpeechService.spokenRange else null

    Row(
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (row.isGroup && !isOwn) {
            SenderAvatar(show = row.showSenderAvatar, initials = row.senderInitials)
        }
        if (row.isFailed) {
            FailedOutboxIndicator(
                onTap = { onFailedIndicatorTapped(message.id) },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        if (message.isMediaMessage) {
            MessageContextMenu(
                actions = row.mediaFile?.let { mediaActionsFor(message, it, onSaveMedia) } ?: emptyList(),
                alignment = alignment,
                reactionChoices = reactionChoices,
                alignsMenuCardToLeadingEdge = true,
                menuKey = message.id,
                onTap = { onTapMedia(message.id) },
                header = { SenderNameLabel(row) },
            ) {
                MediaMessageBubble(mediaFile = row.mediaFile, isOwn = isOwn)
            }
        } else if (row.audioReference != null) {
            val isPlaying = AudioMessagePlaybackService.playingMessageID == message.id
            val transcriptionText = displayText
            MessageContextMenu(
                actions =
                    audioActionsFor(row, transcriptionText, onToggleAudioTranscription, onSpeak) {
                        clipboard.setText(AnnotatedString(transcriptionText))
                    },
                alignment = alignment,
                reactionChoices = reactionChoices,
                alignsMenuCardToLeadingEdge = true,
                menuKey = message.id,
                header = { SenderNameLabel(row) },
            ) {
                if (row.isDisplayingAudioTranscription) {
                    MessageBubble(
                        transcriptionText,
                        isOwn,
                        colors.senderBubble,
                        colors.receiverBubble,
                        colors.titleText,
                        isAlternate = true,
                        isSpeaking = isSpeakingThisMessage,
                        highlightRange = speakingHighlightRange,
                    )
                } else {
                    AudioMessageBubble(
                        reference = row.audioReference,
                        isOwn = isOwn,
                        isPlaying = isPlaying,
                        progress = if (isPlaying) AudioMessagePlaybackService.progress else 0f,
                        onPlay = { AudioMessagePlaybackService.didTapPlayButton(message, row.audioReference) },
                    )
                }
            }
        } else {
            MessageContextMenu(
                actions = actionsFor(row, displayText, onToggleAlternate, onSpeak) { clipboard.setText(AnnotatedString(displayText)) },
                alignment = alignment,
                reactionChoices = reactionChoices,
                alignsMenuCardToLeadingEdge = true,
                menuKey = message.id,
                header = { SenderNameLabel(row) },
            ) {
                MessageBubble(
                    displayText,
                    isOwn,
                    colors.senderBubble,
                    colors.receiverBubble,
                    colors.titleText,
                    isAlternate = row.showAlternate,
                    isSpeaking = isSpeakingThisMessage,
                    highlightRange = speakingHighlightRange,
                )
            }
        }
    }
}

/**
 * The label below a message bubble: its reaction chips (one per style,
 * with a count and the current user's own reaction highlighted) and, for
 * the last confirmed own message in a one-to-one chat, its delivery
 * status. Aligns to the message's side, matching iOS's cell bottom label.
 */
@Composable
private fun BottomLabel(
    row: ChatMessageRowData,
    isOwn: Boolean,
) {
    val colors = LocalPantherColors.current
    val chips = reactionChips(row.reactions)
    val status =
        if (isOwn && row.isLastConfirmedOwnMessage && !row.isGroup) statusText(row.message, row.isFailed) else null
    if (chips.isEmpty() && status == null) return

    // Align the label to the message bubble, not the screen edge: group
    // received bubbles are indented past the sender avatar column.
    val leadingInset =
        if (row.isGroup &&
            !isOwn
        ) {
            ChatMessageCellFloats.senderAvatarSize + ChatMessageCellFloats.senderAvatarSpacing
        } else {
            0.dp
        }

    Row(
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = ChatMessageCellFloats.bottomLabelTopPadding,
                    start = ChatMessageCellFloats.bottomLabelStartPadding + leadingInset,
                    end = ChatMessageCellFloats.bottomLabelEndPadding,
                ),
    ) {
        chips.forEach { chip -> ReactionChipView(chip) }
        if (status != null) {
            if (chips.isNotEmpty()) {
                Components.Text(ChatMessageCellStrings.STATUS_SEPARATOR, color = colors.subtitleText, font = Font.system(FontScale.Small))
            }
            Components.Text(
                status.first,
                color = if (status.second) ChatMessageCellColors.error else colors.subtitleText,
                font = Font.system(FontScale.Small),
            )
        }
    }
}
