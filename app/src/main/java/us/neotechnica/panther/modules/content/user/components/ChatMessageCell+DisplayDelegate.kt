//
//  ChatMessageCell+DisplayDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.services.AttributeDetectionService
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewStrings
import us.neotechnica.panther.modules.content.user.constants.UserContentExtensionsColors
import us.neotechnica.panther.modules.content.user.constants.UserContentExtensionsFloats

// MARK: - Constants Accessors

private typealias DisplayColors = ChatPageViewColors.MessagesDisplayDelegate
private typealias DisplayFloats = ChatPageViewFloats.MessagesDisplayDelegate
private typealias DisplayStrings = ChatPageViewStrings.MessagesDisplayDelegate
private typealias MessageColors = UserContentExtensionsColors.Message
private typealias StringFloats = UserContentExtensionsFloats.AnnotatedString

/**
 * The red indicator shown beside a failed outbox message. Tapping it
 * presents an action sheet offering to retry or delete the message.
 *
 * @param onTap The action performed when the indicator is tapped.
 * @param modifier The modifier for this indicator.
 */
@Composable
internal fun FailedOutboxIndicator(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(end = ChatPageViewFloats.FailedOutboxIndicator.indicatorButtonSpacing)
                .size(ChatPageViewFloats.FailedOutboxIndicator.indicatorButtonSize)
                .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Components.Symbol(
            ChatPageViewStrings.FailedOutboxIndicator.BUTTON_IMAGE_SYSTEM_NAME,
            foregroundColor = ChatPageViewColors.MessagesDataSource.cellBottomLabelAttributedTextNotDeliveredForeground,
            modifier = Modifier.size(ChatPageViewFloats.FailedOutboxIndicator.indicatorButtonSize),
        )
    }
}

/**
 * The sender's name shown above a received group message, or nothing
 * when the row shows none.
 *
 * @param row The row whose sender to name.
 */
@Composable
internal fun SenderNameLabel(row: ChatMessageRowData) {
    val label = messageTopLabelAttributedText(row) ?: return
    Text(
        text = label,
        modifier = Modifier.padding(bottom = ChatPageViewFloats.messageLabelInset),
    )
}

/**
 * The sender's avatar shown to the leading edge of every received group
 * message, aligned to the bubble's bottom: the sender's contact image,
 * or the generic person glyph.
 *
 * @param row The row whose sender to render.
 */
@Composable
internal fun SenderAvatar(row: ChatMessageRowData) {
    Box(modifier = Modifier.padding(end = DisplayFloats.avatarSpacing).size(DisplayFloats.avatarSize)) {
        if (row.senderImage != null) {
            AvatarImageView(row.senderImage, size = DpSize(DisplayFloats.avatarSize, DisplayFloats.avatarSize))
        } else {
            Components.Symbol(
                DisplayStrings.AVATAR_VIEW_IMAGE_SYSTEM_NAME,
                foregroundColor = DisplayColors.genericAvatarViewTint,
                usesIntrinsicSize = false,
                modifier = Modifier.size(DisplayFloats.avatarSize).background(DisplayColors.genericAvatarViewBackground),
            )
        }
    }
}

/**
 * The bubble shape for the given side, with a curved tail at the
 * bottom corner nearest the sender.
 *
 * @param isOwn Whether the message was sent by the current user.
 */
internal fun bubbleShape(isOwn: Boolean): Shape =
    RoundedCornerShape(
        topStart = DisplayFloats.bubbleRadius,
        topEnd = DisplayFloats.bubbleRadius,
        bottomStart = if (isOwn) DisplayFloats.bubbleRadius else DisplayFloats.bubbleTailRadius,
        bottomEnd = if (isOwn) DisplayFloats.bubbleTailRadius else DisplayFloats.bubbleRadius,
    )

/**
 * A text message bubble.
 *
 * Renders the text at the message cell size with its paragraph line
 * spacing, italicized for alternate content, underlining detected
 * dates, links, and phone numbers, and highlighting the range being
 * spoken aloud in red.
 *
 * @param text The text to render.
 * @param isOwn Whether the message was sent by the current user.
 * @param isItalicized Whether the text renders italicized.
 * @param isSpeaking Whether the message is being spoken aloud.
 * @param highlightRange The character range being enunciated, or
 *   `null`.
 */
@Composable
@Suppress("LongParameterList")
internal fun MessageBubble(
    text: String,
    isOwn: Boolean,
    isItalicized: Boolean,
    isSpeaking: Boolean = false,
    highlightRange: IntRange? = null,
) {
    val colors = LocalPantherColors.current
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val nonCurrentUserForegroundColor =
        if (isDarkModeActive) MessageColors.kindAttributedTextDarkForeground else MessageColors.kindAttributedTextLightForeground
    val foregroundColor = if (isOwn) MessageColors.kindAttributedTextCurrentUserForeground else nonCurrentUserForegroundColor
    val detectorColor =
        if (isOwn || isDarkModeActive) {
            DisplayColors.detectorAttributesPrimaryForeground
        } else {
            DisplayColors.detectorAttributesAlternateForeground
        }

    Box(
        modifier =
            Modifier
                .widthIn(max = DisplayFloats.bubbleMaxWidth)
                .clip(bubbleShape(isOwn))
                .background(if (isOwn) colors.senderBubble else colors.receiverBubble)
                .padding(
                    horizontal = DisplayFloats.bubbleHorizontalPadding,
                    vertical = DisplayFloats.bubbleVerticalPadding,
                ),
    ) {
        Text(
            text = messageCellString(text.ifBlank { " " }, foregroundColor, detectorColor, isSpeaking, highlightRange),
            style =
                TextStyle(
                    fontSize = StringFloats.MESSAGE_CELL_STRING_SYSTEM_FONT_SIZE.sp,
                    lineHeight = messageCellLineHeight().sp,
                    fontStyle = if (isItalicized) FontStyle.Italic else FontStyle.Normal,
                ),
        )
    }
}

// MARK: - Auxiliary

private fun messageCellLineHeight(): Float =
    StringFloats.MESSAGE_CELL_STRING_SYSTEM_FONT_SIZE * StringFloats.MESSAGE_CELL_STRING_PARAGRAPH_LINE_SPACING

// The message cell string: the base foreground, detected attributes as
// underlined links, and the spoken range highlighted.
private fun messageCellString(
    text: String,
    foregroundColor: Color,
    detectorColor: Color,
    isSpeaking: Boolean,
    highlightRange: IntRange?,
): AnnotatedString =
    buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = foregroundColor), 0, text.length)

        AttributeDetectionService.detectAttributes(text).forEach { attribute ->
            val end = (attribute.range.last + 1).coerceAtMost(text.length)
            addLink(
                LinkAnnotation.Clickable(
                    tag = attribute.range.toString(),
                    styles = TextLinkStyles(style = SpanStyle(color = detectorColor, textDecoration = TextDecoration.Underline)),
                ) { AttributeDetectionService.handleSelection(attribute, text) },
                attribute.range.first,
                end,
            )
        }

        if (!isSpeaking) return@buildAnnotatedString
        highlightRange?.let { range ->
            val start = range.first.coerceIn(0, text.length)
            val end = (range.last + 1).coerceIn(start, text.length)
            if (start < end) {
                val highlight = ChatPageViewColors.AVSpeechSynthesizerDelegate.willSpeakRangeOfSpeechStringHighlight
                addStyle(SpanStyle(color = highlight), start, end)
            }
        }
    }
