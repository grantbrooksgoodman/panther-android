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

object ChatPageViewFloats {
    val messageLabelInset: Dp = 4.dp

    val messageOutgoingCellBottomLabelAlignmentTopTextInset: Dp = 2.dp

    val textCellMessageLabelLeftTextInset: Dp = 15.dp
    val textCellMessageLabelRightTextInset: Dp = 1.dp

    object FailedOutboxIndicator {
        val indicatorButtonSize: Dp = 22.dp
        val indicatorButtonSpacing: Dp = 6.dp
    }

    object MessagesDataSource {
        const val CELL_BOTTOM_LABEL_ATTRIBUTED_TEXT_BOLD_ATTRIBUTES_SYSTEM_FONT_SIZE = 12f
        const val CELL_BOTTOM_LABEL_ATTRIBUTED_TEXT_EMOJI_ATTRIBUTES_SYSTEM_FONT_SIZE = 14f
        const val CELL_BOTTOM_LABEL_ATTRIBUTED_TEXT_STANDARD_ATTRIBUTES_SYSTEM_FONT_SIZE = 12f

        const val MESSAGE_TIMESTAMP_LABEL_ATTRIBUTED_TEXT_ATTRIBUTES_SYSTEM_FONT_SIZE = 12f
        const val MESSAGE_TOP_LABEL_ATTRIBUTED_TEXT_ATTRIBUTES_BASELINE_OFFSET = 3f
        const val MESSAGE_TOP_LABEL_ATTRIBUTED_TEXT_ATTRIBUTES_FONT_SIZE = 10.5f
    }

    object MessagesDisplayDelegate {
        const val AUDIO_CELL_PROGRESS_VIEW_TRACK_TINT_COLOR_ALPHA_COMPONENT = 0.8f
        const val AUDIO_CELL_PROGRESS_VIEW_TRACK_TINT_COLOR_DARKENING_PERCENTAGE = 6f

        const val MESSAGE_STYLE_CUSTOM_LAYER_SHADOW_OPACITY = 0.1f

        val avatarSize: Dp = 30.dp
        val avatarSpacing: Dp = 6.dp
        val messageStyleCustomLayerCornerRadius: Dp = 10.dp
        val messageStyleCustomLayerShadowOffsetHeight: Dp = 2.dp
        val messageStyleCustomLayerShadowRadius: Dp = 4.dp

        // Bubble metrics: the corner radius, tail radius, maximum width,
        // and content insets of a message bubble.
        val bubbleHorizontalPadding: Dp = 14.dp
        val bubbleMaxWidth: Dp = 280.dp
        val bubbleOutlineWidth: Dp = 1.dp
        val bubbleRadius: Dp = 18.dp
        val bubbleTailRadius: Dp = 4.dp
        val bubbleVerticalPadding: Dp = 9.dp

        // Reaction chip metrics.
        const val REACTION_CHIP_FONT_SIZE = 14f
        const val REACTION_CHIP_OWN_HIGHLIGHT_ALPHA = 0.22f

        val reactionChipCornerRadius: Dp = 10.dp
        val reactionChipCountStartPadding: Dp = 2.dp
        val reactionChipHorizontalPadding: Dp = 6.dp
        val reactionChipSpacing: Dp = 4.dp
        val reactionChipVerticalPadding: Dp = 2.dp
    }

    object MessagesLayoutDelegate {
        const val CELL_TOP_LABEL_HEIGHT_SENT_DATE_SECONDS_COMPARATOR = 5400L

        val cellBottomLabelHeight: Dp = 20.dp
        val cellTopLabelHeight: Dp = 25.dp
        val messageTopLabelHeight: Dp = 15.dp
        val rowHorizontalPadding: Dp = 12.dp
        val rowVerticalPadding: Dp = 2.dp
    }

    object MessageTimestamp {
        const val REVEAL_DRAG_THRESHOLD_FRACTION = 0.5f

        val labelWidth: Dp = 72.dp
        val revealDistance: Dp = 80.dp
    }

    object UITextViewDelegate {
        const val SET_BUTTONS_IS_ENABLED_DELAY_MILLISECONDS = 100L
        const val TOGGLE_LABEL_REPRESENTATION_DELAY_MILLISECONDS = 10L
    }
}

// MARK: - Color

object ChatPageViewColors {
    object AVSpeechSynthesizerDelegate {
        val willSpeakRangeOfSpeechStringHighlight = Color.Red
        val willSpeakRangeOfSpeechStringNotWhite = Color.Black
        val willSpeakRangeOfSpeechStringWhite = Color.White
    }

    object MessagesDataSource {
        val cellBottomLabelAttributedTextBoldAttributesForeground = Color.Gray
        val cellBottomLabelAttributedTextNotDeliveredForeground = Color(0xFFFF3B30)
        val cellBottomLabelAttributedTextStandardAttributesForeground = Color.LightGray

        val currentUserAudioTintColor = Color.White

        val messageTimestampLabelAttributedTextAttributesForeground = Color.LightGray
        val messageTopLabelAttributedTextAttributesForeground = Color(0xFF8E8E93)
    }

    object MessagesDisplayDelegate {
        val audioCellProgressViewCurrentUserAccent = Color.White

        val detectorAttributesAlternateForeground = Color.Black
        val detectorAttributesPrimaryForeground = Color.White

        val documentBubbleOutline = Color.Gray

        val genericAvatarViewBackground = Color.Transparent
        val genericAvatarViewTint = Color.Gray

        val messageStyleCustomLayerShadowColor = Color.Black
    }
}

// MARK: - String

object ChatPageViewStrings {
    object FailedOutboxIndicator {
        const val BUTTON_IMAGE_SYSTEM_NAME = "exclamationmark.circle"
        const val INDICATOR_BUTTON_SEMANTIC_TAG = "INDICATOR_BUTTON"
    }

    object MessagesDataSource {
        const val BOTTOM_LABEL_COMPONENT_SEPARATOR = " | "
        const val MESSAGE_TOP_LABEL_ATTRIBUTED_TEXT_PREFIX = "   "
        const val MISSING_TRANSLATION_TEXT = "�"
    }

    object MessagesDisplayDelegate {
        const val AVATAR_VIEW_IMAGE_SYSTEM_NAME = "person.crop.circle.fill"
    }
}
