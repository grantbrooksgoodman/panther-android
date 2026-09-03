//
//  ChatMessageCell.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageContextMenu
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellColors
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellFloats
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellStrings
import us.neotechnica.panther.modules.content.user.services.ContextMenuActionHandlerService
import us.neotechnica.panther.modules.localization.models.LocalizationSource
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.networking.modules.schema.conversation.models.Reaction
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.extensions.isFromCurrentUser
import us.neotechnica.panther.networking.modules.session.extensions.isMediaMessage
import us.neotechnica.panther.networking.modules.session.extensions.isSystemMessage
import us.neotechnica.panther.networking.modules.session.extensions.otherParticipantReadReceipt
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.Text as Material3Text

/**
 * A single chat row: an optional day separator, the message bubble
 * (with a long-press context menu), and, for the last confirmed own
 * message in a one-to-one chat, a delivery status label.
 *
 * @param row The row's display inputs.
 * @param onToggleAlternate Toggles the alternate text for a message ID.
 * @param onTapMedia Opens the media preview for the given media message ID.
 * @param onReact Applies the given reaction style to the given message.
 * @param onSpeak Speaks the given displayed text for the given message ID.
 */
@Composable
fun ChatMessageCell(
    row: ChatMessageRowData,
    onToggleAlternate: (String) -> Unit,
    onTapMedia: (String) -> Unit,
    onReact: (Message, Reaction.Style) -> Unit,
    onSpeak: (String, String) -> Unit,
) {
    val colors = LocalPantherColors.current
    val clipboard = LocalClipboardManager.current
    val message = row.message

    Column(
        modifier =
            Modifier.fillMaxWidth().padding(
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

        val isOwn = message.isFromCurrentUser
        val displayText = displayText(row)
        val reactionChoices = reactionChoicesFor(row, onReact)
        val alignment = if (isOwn) ContextMenuAlignment.TRAILING else ContextMenuAlignment.LEADING

        Row(
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (row.isGroup && !isOwn) {
                SenderAvatar(show = row.showSenderAvatar, initials = row.senderInitials)
            }
            if (message.isMediaMessage) {
                MessageContextMenu(actions = emptyList(), alignment = alignment, reactionChoices = reactionChoices) {
                    Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
                        SenderNameLabel(row)
                        MediaMessageBubble(
                            mediaFile = row.mediaFile,
                            isOwn = isOwn,
                            onTap = { onTapMedia(message.id) },
                        )
                    }
                }
            } else if (row.audioReference != null) {
                MessageContextMenu(actions = emptyList(), alignment = alignment, reactionChoices = reactionChoices) {
                    Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
                        SenderNameLabel(row)
                        AudioMessageBubble(reference = row.audioReference, isOwn = isOwn)
                    }
                }
            } else {
                MessageContextMenu(
                    actions = actionsFor(row, displayText, onToggleAlternate, onSpeak) { clipboard.setText(AnnotatedString(displayText)) },
                    alignment = alignment,
                    reactionChoices = reactionChoices,
                ) {
                    Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
                        SenderNameLabel(row)
                        MessageBubble(
                            displayText,
                            isOwn,
                            colors.senderBubble,
                            colors.receiverBubble,
                            colors.titleText,
                            isAlternate = row.showAlternate,
                        )
                    }
                }
            }
        }

        BottomLabel(row = row, isOwn = isOwn)
    }
}

/**
 * The sender's display name shown above the first message in a run from
 * a group participant, or nothing when it should be hidden.
 */
@Composable
private fun SenderNameLabel(row: ChatMessageRowData) {
    if (!row.isGroup || row.message.isFromCurrentUser || row.senderName == null) return
    val colors = LocalPantherColors.current
    Components.Text(
        row.senderName,
        color = colors.subtitleText,
        font = Font.systemMedium(FontScale.Small),
        modifier =
            Modifier.padding(
                start = ChatMessageCellFloats.senderNameStartPadding,
                bottom = ChatMessageCellFloats.senderNameBottomPadding,
            ),
    )
}

/**
 * The sender's avatar shown to the leading edge of a received group
 * message, aligned to the bubble's bottom. Renders the sender's initials
 * when a contact match exists, otherwise a generic person glyph. When
 * [show] is `false`, it reserves the same width so consecutive bubbles
 * stay aligned.
 */
@Composable
private fun SenderAvatar(
    show: Boolean,
    initials: String,
) {
    Box(modifier = Modifier.padding(end = ChatMessageCellFloats.senderAvatarSpacing).size(ChatMessageCellFloats.senderAvatarSize)) {
        if (!show) return@Box
        AvatarImageView(
            modifier = Modifier.fillMaxSize(),
            initials = initials,
            glyphSize = ChatMessageCellFloats.senderAvatarGlyphSize,
            initialsFont = Font.systemSemibold(FontScale.Small),
        )
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

@Composable
private fun ReactionChipView(chip: ReactionChip) {
    val colors = LocalPantherColors.current
    val background =
        if (chip.isOwn) {
            chip.style.squareIconColor.copy(alpha = ChatMessageCellFloats.REACTION_OWN_HIGHLIGHT_ALPHA)
        } else {
            colors.reactionButtonBackground
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(end = ChatMessageCellFloats.reactionChipSpacing)
                .clip(RoundedCornerShape(ChatMessageCellFloats.reactionChipCornerRadius))
                .background(background)
                .padding(
                    horizontal = ChatMessageCellFloats.reactionChipHorizontalPadding,
                    vertical = ChatMessageCellFloats.reactionChipVerticalPadding,
                ),
    ) {
        Components.Text(
            chip.style.emojiValue,
            color = colors.titleText,
            font = Font.system(FontScale.Custom(ChatMessageCellFloats.REACTION_FONT_SIZE)),
        )
        if (chip.count > 1) {
            Components.Text(
                chip.count.toString(),
                color = colors.subtitleText,
                font = Font.system(FontScale.Small),
                modifier = Modifier.padding(start = ChatMessageCellFloats.reactionChipCountStartPadding),
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun MessageBubble(
    text: String,
    isOwn: Boolean,
    senderBubble: Color,
    receiverBubble: Color,
    receivedTextColor: Color,
    isAlternate: Boolean,
) {
    val shape =
        RoundedCornerShape(
            topStart = ChatMessageCellFloats.bubbleRadius,
            topEnd = ChatMessageCellFloats.bubbleRadius,
            bottomStart = if (isOwn) ChatMessageCellFloats.bubbleRadius else ChatMessageCellFloats.bubbleTailRadius,
            bottomEnd = if (isOwn) ChatMessageCellFloats.bubbleTailRadius else ChatMessageCellFloats.bubbleRadius,
        )

    Box(
        modifier =
            Modifier
                .widthIn(max = ChatMessageCellFloats.bubbleMaxWidth)
                .clip(shape)
                .background(if (isOwn) senderBubble else receiverBubble)
                .padding(
                    horizontal = ChatMessageCellFloats.bubbleHorizontalPadding,
                    vertical = ChatMessageCellFloats.bubbleVerticalPadding,
                ),
    ) {
        Components.Text(
            text.ifBlank { " " },
            color = if (isOwn) Color.White else receivedTextColor,
            font = if (isAlternate) Font.systemItalic() else Font.system,
        )
    }
}

// MARK: - Auxiliary

private fun actionsFor(
    row: ChatMessageRowData,
    displayText: String,
    onToggleAlternate: (String) -> Unit,
    onSpeak: (String, String) -> Unit,
    onCopy: () -> Unit,
): List<ContextMenuAction> {
    val actions = mutableListOf<ContextMenuAction>()
    actions.add(ContextMenuAction(LocalizedStringKey.Copy.localized(), "doc.on.doc") { onCopy() })

    val isSpeaking = TextToSpeechService.isSpeaking
    actions.add(
        ContextMenuAction(
            (if (isSpeaking) LocalizedStringKey.StopSpeaking else LocalizedStringKey.Speak).localized(),
            if (isSpeaking) "speaker.slash.circle" else "speaker.wave.2.circle",
        ) { onSpeak(row.message.id, displayText) },
    )

    // The report and view-alternate actions are hidden while speaking, mirroring iOS.
    if (isSpeaking) return actions

    ContextMenuActionHandlerService.reportMistranslationAction(row)?.let { actions.add(it) }

    if (shouldShowAlternateAction(row)) {
        val title =
            if (row.message.isFromCurrentUser) {
                if (row.showAlternate) LocalizedStringKey.ViewOriginal else LocalizedStringKey.ViewTranslation
            } else {
                if (row.showAlternate) LocalizedStringKey.ViewTranslation else LocalizedStringKey.ViewOriginal
            }
        actions.add(ContextMenuAction(title.localized(), "globe") { onToggleAlternate(row.message.id) })
    }

    return actions
}

/**
 * Whether the view-original/translation toggle should be offered, lifted
 * from the iOS `getViewAlternateAction`: available in one-to-one chats or
 * for messages not from the current user (so in group chats only on
 * received messages), and only when the translation is not idempotent
 * (e.g. `en`→`en`), has letters, and its counterpart language differs
 * from the current user's.
 */
private fun shouldShowAlternateAction(row: ChatMessageRowData): Boolean {
    if (row.isGroup && row.message.isFromCurrentUser) return false
    val translation = row.translation ?: return false
    val pair = translation.languagePair
    if (pair.isIdempotent) return false
    if (translation.input.value.none { it.isLetter() }) return false
    val relevantLanguageCode = if (row.message.isFromCurrentUser) pair.to else pair.from
    return relevantLanguageCode != RuntimeStorage.languageCode
}

private data class ReactionChip(
    val style: Reaction.Style,
    val count: Int,
    val isOwn: Boolean,
)

private fun reactionChips(reactions: List<Reaction>): List<ReactionChip> =
    reactions
        .groupBy { it.style }
        .entries
        .sortedBy { it.key.orderValue }
        .map { (style, styleReactions) ->
            ReactionChip(
                style = style,
                count = styleReactions.size,
                isOwn = styleReactions.any { it.userID == User.currentUserID },
            )
        }

private fun reactionChoicesFor(
    row: ChatMessageRowData,
    onReact: (Message, Reaction.Style) -> Unit,
): List<ReactionChoice> {
    val ownStyles = row.reactions.filter { it.userID == User.currentUserID }.map { it.style }.toSet()
    return Reaction.Style.orderedCases.map { style ->
        ReactionChoice(
            emoji = style.emojiValue,
            selectedColor = style.squareIconColor,
            isSelected = style in ownStyles,
            isDoubleTapDefault = style == Reaction.Style.LOVE,
            onSelect = { onReact(row.message, style) },
        )
    }
}

// The background color of a reaction style's square icon, mirroring the
// iOS `Reaction.Style.squareIconBackgroundColor` hex values.
private val Reaction.Style.squareIconColor: Color
    get() =
        when (this) {
            Reaction.Style.DISLIKE -> Color(0xFFFF5252)
            Reaction.Style.EMPHASIS -> Color(0xFF0FB9B1)
            Reaction.Style.LAUGH -> Color(0xFFC56CF0)
            Reaction.Style.LIKE -> Color(0xFF27AE60)
            Reaction.Style.LOVE -> Color(0xFF30AAF2)
            Reaction.Style.QUESTION -> Color(0xFFFFB142)
        }

private fun displayText(row: ChatMessageRowData): String {
    val translation = row.translation ?: return ""
    val primary = if (row.message.isFromCurrentUser) translation.input.value else translation.output
    val alternate = if (row.message.isFromCurrentUser) translation.output else translation.input.value
    return sanitized(if (row.showAlternate) alternate else primary)
}

private fun separatorDate(
    message: Message,
    previousMessage: Message?,
): Date? {
    val show =
        previousMessage == null ||
            (message.sentDate.time - previousMessage.sentDate.time) > ChatMessageCellFloats.DAY_SEPARATOR_GAP_MILLIS
    if (!show) return null
    return message.sentDate
}

/**
 * The attributed day-separator string (`Today 9:26 PM`), with the day
 * prefix rendered bold, matching the iOS message separator.
 */
private fun separatorAnnotatedString(date: Date): AnnotatedString =
    buildAnnotatedString {
        val parts = separatorParts(date)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(parts.prefix) }
        append(" ${parts.time}")
    }

/**
 * The attributed string for a system message: a bold date-separator line
 * (`Today 13:35`) followed by the activity text, with participant names
 * (wrapped in `⌘…⌘` sentinels) rendered bold. Mirrors the iOS
 * `Message.attributedSystemString`.
 */
private fun systemMessageString(
    output: String,
    date: Date,
): AnnotatedString =
    buildAnnotatedString {
        val parts = separatorParts(date)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(parts.prefix) }
        append(" ${parts.time}\n")
        appendActivity(output)
    }

private fun AnnotatedString.Builder.appendActivity(text: String) {
    val cleaned = text.replace("⁂", "").replace("※", "")
    var isBold = false
    for (segment in cleaned.split("⌘")) {
        if (segment.isNotEmpty()) {
            if (isBold) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(segment) }
            } else {
                append(segment)
            }
        }
        isBold = !isBold
    }
}

private data class SeparatorParts(
    val prefix: String,
    val time: String,
)

/**
 * Resolves a day-separator word (e.g. `Today`, `Yesterday`), which live
 * in different localization tables, preferring the app table and falling
 * back to the subsystem table before the missing placeholder.
 */
private fun dayWord(key: LocalizedStringKey): String {
    val appValue = key.localized(LocalizationSource.APP)
    return if (appValue != LocalizedStringResolver.MISSING) appValue else key.localized(LocalizationSource.SUBSYSTEM)
}

private fun separatorParts(date: Date): SeparatorParts {
    val messageDay = Calendar.getInstance().apply { time = date }
    val today = Calendar.getInstance()
    val time = SimpleDateFormat(ChatMessageCellStrings.TIME_FORMAT, Locale.getDefault()).format(date)
    val daysApart = (today.timeInMillis - messageDay.timeInMillis) / ChatMessageCellFloats.MILLIS_PER_DAY
    val prefix =
        when {
            isSameDay(messageDay, today) -> dayWord(LocalizedStringKey.Today)
            isYesterday(messageDay, today) -> dayWord(LocalizedStringKey.Yesterday)
            daysApart < ChatMessageCellFloats.DAYS_IN_WEEK ->
                SimpleDateFormat(
                    ChatMessageCellStrings.DAY_OF_WEEK_FORMAT,
                    Locale.getDefault(),
                ).format(date)
            else -> SimpleDateFormat(ChatMessageCellStrings.FULL_DATE_FORMAT, Locale.getDefault()).format(date)
        }
    return SeparatorParts(prefix, time)
}

private fun isYesterday(
    day: Calendar,
    today: Calendar,
): Boolean {
    val yesterday = today.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(day, yesterday)
}

private fun statusText(
    message: Message,
    isFailed: Boolean,
): Pair<String, Boolean>? {
    if (isFailed) return LocalizedStringKey.NotDelivered.localized() to true
    val readReceipt = message.otherParticipantReadReceipt
    return if (readReceipt != null) {
        val time = SimpleDateFormat(ChatMessageCellStrings.TIME_FORMAT, Locale.getDefault()).format(readReceipt.readDate)
        "${LocalizedStringKey.Read.localized()} $time" to false
    } else {
        LocalizedStringKey.Delivered.localized() to false
    }
}

private fun isSameDay(
    left: Calendar,
    right: Calendar,
): Boolean =
    left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
        left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)

private fun sanitized(value: String): String = value.replace("⁂", "").replace("⌘", "").replace("※", "")
