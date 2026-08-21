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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageContextMenu
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.session.extensions.isFromCurrentUser
import us.neotechnica.panther.networking.modules.session.extensions.isSystemMessage
import us.neotechnica.panther.networking.modules.session.extensions.otherParticipantReadReceipt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
        separatorText(message, row.previousMessage)?.let { separator ->
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Components.Text(separator, color = colors.subtitleText, font = Font.systemMedium(FontScale.Small))
            }
        }

        if (message.isSystemMessage) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                Components.Text(
                    sanitized(row.translation?.output ?: ""),
                    color = colors.subtitleText,
                    font = Font.system(FontScale.Small),
                )
            }
            return@Column
        }

        val isOwn = message.isFromCurrentUser
        val displayText = displayText(row)

        Row(
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
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
                    MessageBubble(displayText, isOwn, colors.senderBubble, colors.receiverBubble, colors.titleText)
                }
            }
        }

        if (isOwn && row.isLastConfirmedOwnMessage && !row.isGroup) {
            statusText(message, row.isFailed)?.let { (text, isError) ->
                Box(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, end = 4.dp), contentAlignment = Alignment.CenterEnd) {
                    Components.Text(
                        text,
                        color = if (isError) ERROR_COLOR else colors.subtitleText,
                        font = Font.system(FontScale.Small),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    text: String,
    isOwn: Boolean,
    senderBubble: Color,
    receiverBubble: Color,
    receivedTextColor: Color,
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

    val translation = row.translation
    if (translation != null && translation.input.value != translation.output) {
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

private fun displayText(row: ChatMessageRowData): String {
    val translation = row.translation ?: return ""
    val primary = if (row.message.isFromCurrentUser) translation.input.value else translation.output
    val alternate = if (row.message.isFromCurrentUser) translation.output else translation.input.value
    return sanitized(if (row.showAlternate) alternate else primary)
}

private fun separatorText(
    message: Message,
    previousMessage: Message?,
): String? {
    val show =
        previousMessage == null ||
            (message.sentDate.time - previousMessage.sentDate.time) > DAY_SEPARATOR_GAP_MILLIS
    if (!show) return null
    return formatSeparator(message.sentDate)
}

private fun formatSeparator(date: Date): String {
    val messageDay = Calendar.getInstance().apply { time = date }
    val today = Calendar.getInstance()
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)

    val daysApart = (today.timeInMillis - messageDay.timeInMillis) / MILLIS_PER_DAY
    return when {
        isSameDay(messageDay, today) -> time
        daysApart < 2 -> "${SimpleDateFormat("EEEE", Locale.getDefault()).format(date)} $time"
        daysApart < DAYS_IN_WEEK -> "${SimpleDateFormat("EEE", Locale.getDefault()).format(date)} $time"
        else -> "${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)} $time"
    }
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
private val BUBBLE_RADIUS = 18.dp
private val BUBBLE_TAIL_RADIUS = 4.dp
private val BUBBLE_MAX_WIDTH = 280.dp
private val ERROR_COLOR = Color(0xFFFF3B30)
