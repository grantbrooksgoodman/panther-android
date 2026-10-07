//
//  ChatMessageCell+DataSource.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellFloats
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellStrings
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.otherParticipantReadReceipt
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Builds the annotated string used to highlight the message currently
 * being spoken aloud: the whole text takes a base color – white for own
 * messages or in dark mode, otherwise black – and the spoken range is
 * highlighted in red.
 */
@Composable
internal fun spokenAnnotatedString(
    text: String,
    isOwn: Boolean,
    highlightRange: IntRange?,
): AnnotatedString {
    val baseColor = if (isOwn || isSystemInDarkTheme()) Color.White else Color.Black
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = baseColor), 0, text.length)
        highlightRange?.let { range ->
            val start = range.first.coerceIn(0, text.length)
            val end = (range.last + 1).coerceIn(start, text.length)
            if (start < end) addStyle(SpanStyle(color = Color.Red), start, end)
        }
    }
}

internal fun displayText(row: ChatMessageRowData): String {
    val translation = row.translation ?: return ""
    val primary = if (row.message.isFromCurrentUser) translation.input.value else translation.output
    val alternate = if (row.message.isFromCurrentUser) translation.output else translation.input.value
    return sanitized(if (row.showAlternate) alternate else primary)
}

internal fun separatorDate(
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
 * prefix rendered bold.
 */
internal fun separatorAnnotatedString(date: Date): AnnotatedString =
    buildAnnotatedString {
        val parts = separatorParts(date)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(parts.prefix) }
        append(" ${parts.time}")
    }

/**
 * The attributed string for a system message: a bold date-separator line
 * (`Today 13:35`) followed by the activity text, with participant names
 * (wrapped in `⌘…⌘` sentinels) rendered bold.
 */
internal fun systemMessageString(
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

internal fun statusText(
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
