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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellColors
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewStrings
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import androidx.compose.material3.Text as Material3Text

/**
 * The red indicator shown beside a failed outbox message. Tapping it
 * presents an action sheet offering to retry or delete the message.
 */
@Composable
internal fun FailedOutboxIndicator(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(end = ChatPageViewFloats.failedOutboxIndicatorButtonSpacing)
                .size(ChatPageViewFloats.failedOutboxIndicatorButtonSize)
                .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Components.Symbol(
            ChatPageViewStrings.FAILED_OUTBOX_INDICATOR_BUTTON_IMAGE_SYSTEM_NAME,
            color = ChatMessageCellColors.error,
            modifier = Modifier.size(ChatPageViewFloats.failedOutboxIndicatorButtonSize),
        )
    }
}

/**
 * The sender's display name shown above the first message in a run from
 * a group participant, or nothing when it should be hidden.
 */
@Composable
internal fun SenderNameLabel(row: ChatMessageRowData) {
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
internal fun SenderAvatar(
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

@Composable
@Suppress("LongParameterList")
internal fun MessageBubble(
    text: String,
    isOwn: Boolean,
    senderBubble: Color,
    receiverBubble: Color,
    receivedTextColor: Color,
    isAlternate: Boolean,
    isSpeaking: Boolean = false,
    highlightRange: IntRange? = null,
) {
    val shape =
        RoundedCornerShape(
            topStart = ChatMessageCellFloats.bubbleRadius,
            topEnd = ChatMessageCellFloats.bubbleRadius,
            bottomStart = if (isOwn) ChatMessageCellFloats.bubbleRadius else ChatMessageCellFloats.bubbleTailRadius,
            bottomEnd = if (isOwn) ChatMessageCellFloats.bubbleTailRadius else ChatMessageCellFloats.bubbleRadius,
        )

    val font = if (isAlternate) Font.systemItalic() else Font.system

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
        if (isSpeaking) {
            Material3Text(
                text = spokenAnnotatedString(text.ifBlank { " " }, isOwn, highlightRange),
                style = font.textStyle,
            )
        } else {
            Components.Text(
                text.ifBlank { " " },
                color = if (isOwn) Color.White else receivedTextColor,
                font = font,
            )
        }
    }
}
