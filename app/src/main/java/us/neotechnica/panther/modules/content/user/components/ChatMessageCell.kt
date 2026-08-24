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
import androidx.compose.foundation.shape.CircleShape
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
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageContextMenu
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.localization.models.LocalizationSource
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.session.extensions.isFromCurrentUser
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
 */
@Composable
fun ChatMessageCell(
    row: ChatMessageRowData,
    onToggleAlternate: (String) -> Unit,
) {
    val colors = LocalPantherColors.current
    val clipboard = LocalClipboardManager.current
    val message = row.message

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        if (message.isSystemMessage) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
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
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
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

        Row(
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (row.isGroup && !isOwn) {
                SenderAvatar(show = row.showSenderAvatar, initials = row.senderInitials)
            }
            MessageContextMenu(
                actions = actionsFor(row, onToggleAlternate) { clipboard.setText(AnnotatedString(displayText)) },
                alignment = if (isOwn) ContextMenuAlignment.TRAILING else ContextMenuAlignment.LEADING,
            ) {
                Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
                    if (row.isGroup && !isOwn && row.senderName != null) {
                        Components.Text(
                            row.senderName,
                            color = colors.subtitleText,
                            font = Font.systemMedium(FontScale.Small),
                            modifier = Modifier.padding(start = 12.dp, bottom = 2.dp),
                        )
                    }
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

        BottomLabel(row = row, isOwn = isOwn)
    }
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
    val colors = LocalPantherColors.current
    Box(modifier = Modifier.padding(end = SENDER_AVATAR_SPACING).size(SENDER_AVATAR_SIZE)) {
        if (!show) return@Box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().clip(CircleShape).background(SENDER_AVATAR_BACKGROUND),
        ) {
            if (initials.isNotBlank()) {
                Components.Text(initials, color = colors.background, font = Font.systemSemibold(FontScale.Small))
            } else {
                Components.Symbol("person", color = colors.background, modifier = Modifier.size(SENDER_AVATAR_GLYPH_SIZE))
            }
        }
    }
}

/**
 * The label below a message bubble: its reactions (as emoji) and, for the
 * last confirmed own message in a one-to-one chat, its delivery status.
 * Aligns to the message's side, matching iOS's cell bottom label.
 */
@Composable
private fun BottomLabel(
    row: ChatMessageRowData,
    isOwn: Boolean,
) {
    val colors = LocalPantherColors.current
    val status =
        if (isOwn && row.isLastConfirmedOwnMessage && !row.isGroup) statusText(row.message, row.isFailed) else null
    if (row.reactionsText.isEmpty() && status == null) return

    // Align the label to the message bubble, not the screen edge: group
    // received bubbles are indented past the sender avatar column.
    val leadingInset = if (row.isGroup && !isOwn) SENDER_AVATAR_SIZE + SENDER_AVATAR_SPACING else 0.dp

    Row(
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, start = 4.dp + leadingInset, end = 4.dp),
    ) {
        if (row.reactionsText.isNotEmpty()) {
            Components.Text(
                row.reactionsText,
                color = colors.subtitleText,
                font = Font.system(FontScale.Custom(REACTION_FONT_SIZE)),
            )
        }
        if (status != null) {
            if (row.reactionsText.isNotEmpty()) {
                Components.Text(" | ", color = colors.subtitleText, font = Font.system(FontScale.Small))
            }
            Components.Text(
                status.first,
                color = if (status.second) ERROR_COLOR else colors.subtitleText,
                font = Font.system(FontScale.Small),
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
            topStart = BUBBLE_RADIUS,
            topEnd = BUBBLE_RADIUS,
            bottomStart = if (isOwn) BUBBLE_RADIUS else BUBBLE_TAIL_RADIUS,
            bottomEnd = if (isOwn) BUBBLE_TAIL_RADIUS else BUBBLE_RADIUS,
        )

    Box(
        modifier =
            Modifier
                .widthIn(max = BUBBLE_MAX_WIDTH)
                .clip(shape)
                .background(if (isOwn) senderBubble else receiverBubble)
                .padding(horizontal = 14.dp, vertical = 9.dp),
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
    onToggleAlternate: (String) -> Unit,
    onCopy: () -> Unit,
): List<ContextMenuAction> {
    val actions = mutableListOf<ContextMenuAction>()
    actions.add(ContextMenuAction(LocalizedStringKey.Copy.localized(), "doc.on.doc") { onCopy() })

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
            (message.sentDate.time - previousMessage.sentDate.time) > DAY_SEPARATOR_GAP_MILLIS
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
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    val daysApart = (today.timeInMillis - messageDay.timeInMillis) / MILLIS_PER_DAY
    val prefix =
        when {
            isSameDay(messageDay, today) -> dayWord(LocalizedStringKey.Today)
            isYesterday(messageDay, today) -> dayWord(LocalizedStringKey.Yesterday)
            daysApart < DAYS_IN_WEEK -> SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
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
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(readReceipt.readDate)
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

private const val DAY_SEPARATOR_GAP_MILLIS = 5_400_000L
private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
private const val DAYS_IN_WEEK = 7
private const val REACTION_FONT_SIZE = 14f
private val BUBBLE_RADIUS = 18.dp
private val BUBBLE_TAIL_RADIUS = 4.dp
private val BUBBLE_MAX_WIDTH = 280.dp
private val ERROR_COLOR = Color(0xFFFF3B30)
private val SENDER_AVATAR_SIZE = 30.dp
private val SENDER_AVATAR_GLYPH_SIZE = 18.dp
private val SENDER_AVATAR_SPACING = 6.dp
private val SENDER_AVATAR_BACKGROUND = Color(0xFFC7C7CC)
