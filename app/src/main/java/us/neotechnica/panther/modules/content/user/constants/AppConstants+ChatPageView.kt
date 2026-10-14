//
//  AppConstants+ChatPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

/** The layout and timing values of the chat page view. */
object ChatPageViewFloats {
    /** The inset of the message label. */
    val messageLabelInset: Dp = 4.dp

    /** The top text inset of an outgoing cell's bottom label. */
    val messageOutgoingCellBottomLabelAlignmentTopTextInset: Dp = 2.dp

    /** The left text inset of a text cell's message label. */
    val textCellMessageLabelLeftTextInset: Dp = 15.dp

    /** The right text inset of a text cell's message label. */
    val textCellMessageLabelRightTextInset: Dp = 1.dp

    /** The values of the failed outbox indicator. */
    object FailedOutboxIndicator {
        /** The size of the indicator button. */
        val indicatorButtonSize: Dp = 22.dp

        /** The spacing of the indicator button. */
        val indicatorButtonSpacing: Dp = 6.dp
    }

    /** The values of the messages data source. */
    object MessagesDataSource {
        /** The font size of the cell bottom label's bold text. */
        const val CELL_BOTTOM_LABEL_ATTRIBUTED_TEXT_BOLD_ATTRIBUTES_SYSTEM_FONT_SIZE = 12f

        /** The font size of the cell bottom label's emoji text. */
        const val CELL_BOTTOM_LABEL_ATTRIBUTED_TEXT_EMOJI_ATTRIBUTES_SYSTEM_FONT_SIZE = 14f

        /** The font size of the cell bottom label's standard text. */
        const val CELL_BOTTOM_LABEL_ATTRIBUTED_TEXT_STANDARD_ATTRIBUTES_SYSTEM_FONT_SIZE = 12f

        /** The font size of the message timestamp label's text. */
        const val MESSAGE_TIMESTAMP_LABEL_ATTRIBUTED_TEXT_ATTRIBUTES_SYSTEM_FONT_SIZE = 12f

        /** The baseline offset of the message top label's text. */
        const val MESSAGE_TOP_LABEL_ATTRIBUTED_TEXT_ATTRIBUTES_BASELINE_OFFSET = 3f

        /** The font size of the message top label's text. */
        const val MESSAGE_TOP_LABEL_ATTRIBUTED_TEXT_ATTRIBUTES_FONT_SIZE = 10.5f
    }

    /** The values of the messages display delegate. */
    object MessagesDisplayDelegate {
        /** The opacity of the audio cell progress view's track. */
        const val AUDIO_CELL_PROGRESS_VIEW_TRACK_TINT_COLOR_ALPHA_COMPONENT = 0.8f

        /** The percentage by which the audio cell progress view's track is darkened. */
        const val AUDIO_CELL_PROGRESS_VIEW_TRACK_TINT_COLOR_DARKENING_PERCENTAGE = 6f

        /** The opacity of the message bubble's shadow. */
        const val MESSAGE_STYLE_CUSTOM_LAYER_SHADOW_OPACITY = 0.1f

        /** The size of the avatar. */
        val avatarSize: Dp = 30.dp

        /** The spacing between the avatar and the bubble. */
        val avatarSpacing: Dp = 6.dp

        /** The corner radius of the message bubble. */
        val messageStyleCustomLayerCornerRadius: Dp = 10.dp

        /** The vertical offset of the message bubble's shadow. */
        val messageStyleCustomLayerShadowOffsetHeight: Dp = 2.dp

        /** The blur radius of the message bubble's shadow. */
        val messageStyleCustomLayerShadowRadius: Dp = 4.dp

        // Bubble metrics: the corner radius, tail radius, maximum width,
        // and content insets of a message bubble.

        /** The horizontal padding of the bubble. */
        val bubbleHorizontalPadding: Dp = 14.dp

        /** The maximum width of the bubble. */
        val bubbleMaxWidth: Dp = 280.dp

        /** The width of the bubble outline. */
        val bubbleOutlineWidth: Dp = 1.dp

        /** The corner radius of the bubble. */
        val bubbleRadius: Dp = 18.dp

        /** The corner radius at the bubble's tail. */
        val bubbleTailRadius: Dp = 4.dp

        /** The vertical padding of the bubble. */
        val bubbleVerticalPadding: Dp = 9.dp

        // Reaction chip metrics.

        /** The font size of the reaction chip. */
        const val REACTION_CHIP_FONT_SIZE = 14f

        /** The opacity of the highlight on the current user's reaction chip. */
        const val REACTION_CHIP_OWN_HIGHLIGHT_ALPHA = 0.22f

        /** The corner radius of the reaction chip. */
        val reactionChipCornerRadius: Dp = 10.dp

        /** The start padding of the reaction chip's count. */
        val reactionChipCountStartPadding: Dp = 2.dp

        /** The horizontal padding of the reaction chip. */
        val reactionChipHorizontalPadding: Dp = 6.dp

        /** The spacing of the reaction chip. */
        val reactionChipSpacing: Dp = 4.dp

        /** The vertical padding of the reaction chip. */
        val reactionChipVerticalPadding: Dp = 2.dp
    }

    /** The values of the messages layout delegate. */
    object MessagesLayoutDelegate {
        /**
         * The interval, in seconds, since the previous message within which
         * a cell omits its date label.
         */
        const val CELL_TOP_LABEL_HEIGHT_SENT_DATE_SECONDS_COMPARATOR = 5400L

        /** The height of the cell bottom label. */
        val cellBottomLabelHeight: Dp = 20.dp

        /** The height of the cell top label. */
        val cellTopLabelHeight: Dp = 25.dp

        /** The height of the message top label. */
        val messageTopLabelHeight: Dp = 15.dp

        /** The horizontal padding of the row. */
        val rowHorizontalPadding: Dp = 12.dp

        /** The vertical padding of the row. */
        val rowVerticalPadding: Dp = 2.dp
    }

    /** The values of the message timestamps. */
    object MessageTimestamp {
        /** The width of the timestamp label. */
        val labelWidth: Dp = 72.dp

        /** The drag distance that fully reveals the timestamps. */
        val revealDistance: Dp = 80.dp
    }

    /** The values of the text view delegate. */
    object UITextViewDelegate {
        /** The delay, in milliseconds, before the buttons' enabled state updates. */
        const val SET_BUTTONS_IS_ENABLED_DELAY_MILLISECONDS = 100L

        /** The delay, in milliseconds, before the label representation toggles. */
        const val TOGGLE_LABEL_REPRESENTATION_DELAY_MILLISECONDS = 10L
    }
}

// MARK: - Color

/** The colors of the chat page view. */
object ChatPageViewColors {
    /** The colors of the speech synthesizer delegate. */
    object AVSpeechSynthesizerDelegate {
        /** The highlight color of the range being spoken. */
        val willSpeakRangeOfSpeechStringHighlight = Color.Red

        /** The color of spoken text that is not white. */
        val willSpeakRangeOfSpeechStringNotWhite = Color.Black

        /** The color of spoken text that is white. */
        val willSpeakRangeOfSpeechStringWhite = Color.White
    }

    /** The values of the messages data source. */
    object MessagesDataSource {
        /** The foreground color of the cell bottom label's bold text. */
        val cellBottomLabelAttributedTextBoldAttributesForeground = Color.Gray

        /** The foreground color of the cell bottom label's not-delivered text. */
        val cellBottomLabelAttributedTextNotDeliveredForeground = Color(0xFFFF3B30)

        /** The foreground color of the cell bottom label's standard text. */
        val cellBottomLabelAttributedTextStandardAttributesForeground = Color.LightGray

        /** The tint color of the current user's audio cell. */
        val currentUserAudioTintColor = Color.White

        /** The foreground color of the message timestamp label. */
        val messageTimestampLabelAttributedTextAttributesForeground = Color.LightGray

        /** The foreground color of the message top label. */
        val messageTopLabelAttributedTextAttributesForeground = Color(0xFF8E8E93)
    }

    /** The values of the messages display delegate. */
    object MessagesDisplayDelegate {
        /** The accent color of the current user's audio cell progress view. */
        val audioCellProgressViewCurrentUserAccent = Color.White

        /** The alternate foreground color of detected attributes. */
        val detectorAttributesAlternateForeground = Color.Black

        /** The primary foreground color of detected attributes. */
        val detectorAttributesPrimaryForeground = Color.White

        /** The outline color of a document bubble. */
        val documentBubbleOutline = Color.Gray

        /** The background color of the generic avatar view. */
        val genericAvatarViewBackground = Color.Transparent

        /** The tint color of the generic avatar view. */
        val genericAvatarViewTint = Color.Gray

        /** The shadow color of the message bubble. */
        val messageStyleCustomLayerShadowColor = Color.Black
    }
}

// MARK: - String

/** The string constants of the chat page view. */
object ChatPageViewStrings {
    /** The values of the failed outbox indicator. */
    object FailedOutboxIndicator {
        /** The system name of the button image. */
        const val BUTTON_IMAGE_SYSTEM_NAME = "exclamationmark.circle"

        /** The semantic tag of the indicator button. */
        const val INDICATOR_BUTTON_SEMANTIC_TAG = "INDICATOR_BUTTON"
    }

    /** The values of the messages data source. */
    object MessagesDataSource {
        /** The separator between the bottom label's components. */
        const val BOTTOM_LABEL_COMPONENT_SEPARATOR = " | "

        /** The prefix of the message top label's text. */
        const val MESSAGE_TOP_LABEL_ATTRIBUTED_TEXT_PREFIX = "   "

        /** The text shown in place of a missing translation. */
        const val MISSING_TRANSLATION_TEXT = "�"
    }

    /** The values of the messages display delegate. */
    object MessagesDisplayDelegate {
        /** The system name of the avatar view image. */
        const val AVATAR_VIEW_IMAGE_SYSTEM_NAME = "person.crop.circle.fill"
    }
}
