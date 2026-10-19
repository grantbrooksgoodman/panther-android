//
//  ChatMessageCell+DataSource.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewStrings
import us.neotechnica.panther.modules.content.user.extensions.chatPageMessageSeparatorAttributedDateString
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.extensions.isOutboxMessage
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.AlternateMessageService
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.subsystem.modules.foundation.extensions.formattedShortString
import us.neotechnica.panther.subsystem.modules.foundation.extensions.languageExonym
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import java.text.DateFormat
import java.util.Date
import java.util.Locale

// MARK: - Constants Accessors

private typealias DataSourceColors = ChatPageViewColors.MessagesDataSource
private typealias DataSourceFloats = ChatPageViewFloats.MessagesDataSource
private typealias DataSourceStrings = ChatPageViewStrings.MessagesDataSource
private typealias DataSourceLayoutFloats = ChatPageViewFloats.MessagesLayoutDelegate

/**
 * Returns the attributed text for the cell's bottom label, or `null`
 * when the cell shows none.
 *
 * A failed outbox message reads "Not Delivered". Otherwise the label
 * names the language of displayed alternate content and, for the last
 * confirmed own message in a one-to-one conversation – while it is
 * the last message in the list or carries reactions – the message's
 * delivery or read status. Reactions render as chips beside the label
 * rather than within it.
 *
 * @param row The row to build the label for.
 */
internal fun cellBottomLabelAttributedText(row: ChatMessageRowData): AnnotatedString? {
    val message = row.message
    if (message.isMock || (message.isOutboxMessage && !row.isFailed)) return null

    if (row.isFailed) {
        return buildAnnotatedString {
            withBoldStyle(DataSourceColors.cellBottomLabelAttributedTextNotDeliveredForeground) {
                append(LocalizedStringKey.NotDelivered.localized())
            }
        }
    }

    val languagePair = message.translation?.languagePair
    val fromLanguageExonym = languagePair?.from?.languageExonym
    val toLanguageExonym = languagePair?.to?.languageExonym

    var alternateMessageString: String? = null
    var alternateMessageExonym: String? = null
    if (AlternateMessageService.isDisplayingAlternateText(message) && fromLanguageExonym != null && toLanguageExonym != null) {
        val originalString = LocalizedStringKey.OriginalInLanguage.localized().replace("⌘", fromLanguageExonym)
        val translationString = LocalizedStringKey.TranslationInLanguage.localized().replace("⌘", toLanguageExonym)
        alternateMessageString = if (message.isFromCurrentUser) translationString else originalString
        alternateMessageExonym = if (message.isFromCurrentUser) toLanguageExonym else fromLanguageExonym
    }

    // The alternate footer suppresses the delivery status.
    if (alternateMessageString != null) {
        return buildAnnotatedString {
            appendWithBoldKeywords(alternateMessageString, listOf(alternateMessageExonym.orEmpty()))
        }
    }

    val hasStatusRoom = row.isLastMessage || row.reactions.isNotEmpty()
    if (!row.isGroup && row.isLastConfirmedOwnMessage && message.isFromCurrentUser && hasStatusRoom) {
        val readDate = message.readReceipts?.firstOrNull { it.userID != User.currentUserID }?.readDate
        return buildAnnotatedString {
            if (readDate == null) {
                withBoldStyle(DataSourceColors.cellBottomLabelAttributedTextBoldAttributesForeground) {
                    append(LocalizedStringKey.Delivered.localized())
                }
            } else {
                withBoldStyle(DataSourceColors.cellBottomLabelAttributedTextBoldAttributesForeground) {
                    append(LocalizedStringKey.Read.localized())
                }
                withStandardStyle { append(" ${readDate.formattedShortString}") }
            }
        }
    }

    return null
}

/**
 * Returns the attributed text for the cell's top label, showing the
 * message's date separator, or `null` when the message follows its
 * predecessor closely enough to need none.
 *
 * @param row The row to build the label for.
 */
internal fun cellTopLabelAttributedText(row: ChatMessageRowData): AnnotatedString? {
    val previousMessage = row.previousMessage
    val secondsSincePrevious =
        previousMessage?.let { (row.message.sentDate.time - it.sentDate.time) / MILLISECONDS_PER_SECOND }
    val isCloseToPrevious =
        secondsSincePrevious != null &&
            secondsSincePrevious <= DataSourceLayoutFloats.CELL_TOP_LABEL_HEIGHT_SENT_DATE_SECONDS_COMPARATOR
    if (isCloseToPrevious) return null

    return row.message.sentDate.chatPageMessageSeparatorAttributedDateString
}

/**
 * Returns the attributed text for the message's timestamp label,
 * shown when the user drags the list: the message's sent time in
 * the short style.
 *
 * @param message The message to build the label for.
 */
internal fun messageTimestampLabelAttributedText(message: Message): AnnotatedString =
    buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontSize = DataSourceFloats.MESSAGE_TIMESTAMP_LABEL_ATTRIBUTED_TEXT_ATTRIBUTES_SYSTEM_FONT_SIZE.sp,
                color = DataSourceColors.messageTimestampLabelAttributedTextAttributesForeground,
            ),
        ) {
            append(shortTimeString(message.sentDate))
        }
    }

/**
 * Returns the attributed text for the message's top label,
 * showing the sender's name in group conversations, or `null`
 * when the row shows none.
 *
 * @param row The row to build the label for.
 */
internal fun messageTopLabelAttributedText(row: ChatMessageRowData): AnnotatedString? {
    if (!row.isGroup || row.message.isFromCurrentUser) return null
    val senderName = row.senderName ?: return null
    return buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontSize = DataSourceFloats.MESSAGE_TOP_LABEL_ATTRIBUTED_TEXT_ATTRIBUTES_FONT_SIZE.sp,
                color = DataSourceColors.messageTopLabelAttributedTextAttributesForeground,
            ),
        ) {
            append("${DataSourceStrings.MESSAGE_TOP_LABEL_ATTRIBUTED_TEXT_PREFIX}$senderName")
        }
    }
}

// MARK: - Auxiliary

private fun AnnotatedString.Builder.appendWithBoldKeywords(
    string: String,
    keywords: List<String>,
) {
    val start = length
    withStandardStyle { append(string) }
    keywords.filter { it.isNotEmpty() }.forEach { keyword ->
        var index = string.indexOf(keyword)
        while (index >= 0) {
            addStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = DataSourceFloats.CELL_BOTTOM_LABEL_ATTRIBUTED_TEXT_BOLD_ATTRIBUTES_SYSTEM_FONT_SIZE.sp,
                    color = DataSourceColors.cellBottomLabelAttributedTextBoldAttributesForeground,
                ),
                start + index,
                start + index + keyword.length,
            )
            index = string.indexOf(keyword, index + keyword.length)
        }
    }
}

private fun shortTimeString(date: Date): String = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(date)

private fun AnnotatedString.Builder.withBoldStyle(
    color: Color,
    block: AnnotatedString.Builder.() -> Unit,
) {
    withStyle(
        SpanStyle(
            fontWeight = FontWeight.Bold,
            fontSize = DataSourceFloats.CELL_BOTTOM_LABEL_ATTRIBUTED_TEXT_BOLD_ATTRIBUTES_SYSTEM_FONT_SIZE.sp,
            color = color,
        ),
    ) { block() }
}

private fun AnnotatedString.Builder.withStandardStyle(block: AnnotatedString.Builder.() -> Unit) {
    withStyle(
        SpanStyle(
            fontSize = DataSourceFloats.CELL_BOTTOM_LABEL_ATTRIBUTED_TEXT_STANDARD_ATTRIBUTES_SYSTEM_FONT_SIZE.sp,
            color = DataSourceColors.cellBottomLabelAttributedTextStandardAttributesForeground,
        ),
    ) { block() }
}

private const val MILLISECONDS_PER_SECOND = 1000L
