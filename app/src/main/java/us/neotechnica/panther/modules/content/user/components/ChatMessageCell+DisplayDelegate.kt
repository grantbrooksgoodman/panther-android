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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.DetectedAttribute
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.AttributeDetectionService
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewStrings
import us.neotechnica.panther.modules.content.user.constants.UserContentExtensionsColors
import us.neotechnica.panther.modules.content.user.constants.UserContentExtensionsFloats
import us.neotechnica.panther.modules.content.user.extensions.fromInitials
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

// MARK: - Constants Accessors

private typealias DisplayColors = ChatPageViewColors.MessagesDisplayDelegate
private typealias DisplayFloats = ChatPageViewFloats.MessagesDisplayDelegate
private typealias DisplayStrings = ChatPageViewStrings.MessagesDisplayDelegate
private typealias MessageColors = UserContentExtensionsColors.Message
private typealias StringFloats = UserContentExtensionsFloats.AnnotatedString

/**
 * The red indicator shown to the trailing side of a failed outbox
 * message. Tapping it presents an action sheet offering to retry or
 * delete the message.
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
                .padding(start = ChatPageViewFloats.FailedOutboxIndicator.indicatorButtonSpacing)
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
        style =
            Font
                .system(FontScale.Custom(ChatPageViewFloats.MessagesDataSource.MESSAGE_TOP_LABEL_ATTRIBUTED_TEXT_ATTRIBUTES_FONT_SIZE))
                .textStyle,
        modifier = Modifier.padding(bottom = ChatPageViewFloats.messageLabelInset),
    )
}

/**
 * The sender's avatar shown to the leading edge of every received
 * group message, aligned to the bubble's bottom: the sender's
 * contact image, the contact's initials, or – without a contact –
 * the generic person glyph.
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
 * The initials images rendered for contacts without a photo, keyed by
 * contact identifier.
 */
internal object ContactInitialsImageCache {
    // MARK: - Properties

    private val cachedInitialsImagesForContactIDs = LockIsolated(mapOf<String, ImageBitmap>())

    // MARK: - Methods

    /** Removes every cached initials image. */
    fun clearCache() {
        cachedInitialsImagesForContactIDs.wrappedValue = emptyMap()
    }

    /**
     * Returns the initials image for the given contact, rendering and
     * caching it on first access.
     *
     * @param contact The contact to render.
     */
    fun image(contact: Contact): ImageBitmap {
        cachedInitialsImagesForContactIDs.wrappedValue[contact.id]?.let { return it }
        val image = ImageBitmap.fromInitials(contact.initials)
        cachedInitialsImagesForContactIDs.withValue { it.value = it.value + (contact.id to image) }
        return image
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
 * A text message bubble's label: the label's text layout and position
 * in the root coordinate space, recorded as the label lays out, so a
 * tap on the bubble resolves to the character it falls on.
 */
internal class MessageLabel {
    // MARK: - Dependencies

    private val attributeDetectionService: AttributeDetectionService by Dependency { it.commonServices.attributeDetection }

    // MARK: - Properties

    var layoutResult: TextLayoutResult? = null
    var positionInRoot: Offset = Offset.Zero

    // MARK: - Handle Gesture

    /**
     * Handles a tap at the given location in the root coordinate
     * space, notifying the attribute detection delegate if the touch
     * falls on a detected date, link, or phone number.
     *
     * @param touchLocation The location of the touch in the root
     *   coordinate space.
     */
    fun handleGesture(touchLocation: Offset) {
        val layoutResult = layoutResult ?: return
        val characterIndex = layoutResult.characterIndex(touchLocation - positionInRoot) ?: return
        attributeDetectionService
            .handleGesture(layoutResult.layoutInput.text.text, characterIndex)
    }
}

/**
 * A text message bubble.
 *
 * Renders the text at the message cell size with its paragraph line
 * spacing, italicized for alternate content, underlining detected
 * dates, links, and phone numbers, and highlighting the range being
 * spoken aloud in red. Taps are handled by the enclosing
 * `MessageContextMenu` through [label], so the detected attributes
 * install no gesture handling of their own that would consume a long
 * press.
 *
 * @param text The text to render.
 * @param messageID The identifier of the message, under which the
 *   detected attributes are cached.
 * @param label The bubble's label, which records the text layout for
 *   resolving taps.
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
    messageID: String,
    label: MessageLabel,
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

    // Detection runs off the main thread once per message.
    val attributes by produceState(
        initialValue =
            DependencyValues.current.commonServices.attributeDetection
                .cachedAttributes(text, messageID) ?: emptyList(),
        key1 = text,
        key2 = messageID,
    ) {
        value =
            DependencyValues.current.commonServices.attributeDetection
                .attributes(text, messageID)
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
            text = messageCellString(text.ifBlank { " " }, attributes, foregroundColor, detectorColor, isSpeaking, highlightRange),
            style =
                TextStyle(
                    fontSize = StringFloats.MESSAGE_CELL_STRING_SYSTEM_FONT_SIZE.sp,
                    lineHeight = messageCellLineHeight().sp,
                    fontStyle = if (isItalicized) FontStyle.Italic else FontStyle.Normal,
                ),
            onTextLayout = { label.layoutResult = it },
            modifier = Modifier.onGloballyPositioned { label.positionInRoot = it.positionInRoot() },
        )
    }
}

// MARK: - Auxiliary

// The index of the character nearest the given location in the
// layout, or `null` for empty text.
private fun TextLayoutResult.characterIndex(location: Offset): Int? {
    val lastIndex = layoutInput.text.length - 1
    if (lastIndex < 0) return null

    // The nearest cursor offset sits either before or after the nearest
    // glyph.
    val offset = getOffsetForPosition(location)
    return listOf(offset, offset - 1)
        .filter { it in 0..lastIndex }
        .minByOrNull { getBoundingBox(it).distanceTo(location) }
}

// The distance from the rectangle to the given point: zero when the
// point lies within it.
private fun Rect.distanceTo(point: Offset): Float =
    (point - Offset(point.x.coerceIn(left, right), point.y.coerceIn(top, bottom))).getDistance()

private fun messageCellLineHeight(): Float =
    StringFloats.MESSAGE_CELL_STRING_SYSTEM_FONT_SIZE * StringFloats.MESSAGE_CELL_STRING_PARAGRAPH_LINE_SPACING

// The message cell string: the base foreground, detected attributes
// underlined in the detector color, and the spoken range highlighted.
@Suppress("LongParameterList")
private fun messageCellString(
    text: String,
    attributes: List<DetectedAttribute>,
    foregroundColor: Color,
    detectorColor: Color,
    isSpeaking: Boolean,
    highlightRange: IntRange?,
): AnnotatedString =
    buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = foregroundColor), 0, text.length)

        attributes.forEach { attribute ->
            val end = (attribute.range.last + 1).coerceAtMost(text.length)
            addStyle(SpanStyle(color = detectorColor, textDecoration = TextDecoration.Underline), attribute.range.first, end)
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
