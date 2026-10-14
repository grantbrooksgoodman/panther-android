//
//  AppConstants+ChatPageViewService.kt
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

object ChatPageViewServiceFloats {
    const val INPUT_BAR_APPEARANCE_ANIMATION_DURATION = 0.2f
    const val LOAD_MORE_MESSAGES_DELAY_MILLISECONDS = 200L
    const val OUTBOX_CHANGE_DEBOUNCE_MILLISECONDS = 250L
    const val SCROLL_DELAY_MILLISECONDS = 10L
    const val SESSION_STORE_CHANGE_DEBOUNCE_MILLISECONDS = 250L
    const val TRIGGER_FOCUSED_MESSAGE_CELL_INTERACTION_DELAY_MILLISECONDS = 500L

    val previewConfigBottomInset: Dp = 8.dp

    object AudioMessagePlayback {
        const val PLAYBACK_TIMER_TIME_INTERVAL = 0.1f
        const val PLAY_NEXT_MESSAGE_DELAY_MILLISECONDS = 100L
    }

    object ContextMenu {
        const val DOUBLE_TAP_GESTURE_NUMBER_OF_TAPS_REQUIRED = 2
        const val INTERACTION_TIMER_TIME_INTERVAL = 0.1f
        const val INTERACTION_SCROLL_TO_LAST_ITEM_DELAY_MILLISECONDS = 60L
        const val LANGUAGE_RECOGNITION_MATCH_CONFIDENCE_THRESHOLD = 0.8f
        const val LONG_PRESS_GESTURE_MINIMUM_PRESS_DURATION = 0.5f
        const val MENU_STYLE_TRANSFORM_SCALE_X = 1.08f
        const val MENU_STYLE_TRANSFORM_SCALE_Y = 1.08f
        const val REACTION_SCROLL_TO_LAST_ITEM_DELAY_MILLISECONDS = 200L
        const val TRIGGER_EXISTING_SELECTION_DELAY_MILLISECONDS = 10L

        val menuStyleBottomMargin: Dp = 8.dp
        val menuStyleTopMargin: Dp = 8.dp
    }

    object DeliveryProgressIndicator {
        const val ANIMATION_DELAY = 1f
        const val ANIMATION_DURATION = 0.2f
        const val APPEARANCE_TIMER_TIME_INTERVAL = 5f
        const val HIDDEN_TIMER_TIME_INTERVAL = 0.05f
        const val TIMER_PROGRESS_INCREMENT = 0.001f
        const val TIMER_PROGRESS_INCREMENT_THRESHOLD = 0.9f
        const val VISIBLE_TIMER_TIME_INTERVAL = 0.01f

        val viewFrameHeight: Dp = 2.dp
    }

    object InputBar {
        const val BUTTON_ON_SELECTED_TRANSFORM_SCALE_X = 1.1f
        const val BUTTON_ON_SELECTED_TRANSFORM_SCALE_Y = 1.1f
        const val FORCE_APPEARANCE_DELAY_MILLISECONDS = 200L
        const val TRANSITION_ANIMATION_DURATION = 0.3f

        val buttonSizeHeight: Dp = 30.dp
        val buttonSizeWidth: Dp = 30.dp
        val layerBorderWidth: Dp = 0.5.dp
        val layerCornerRadius: Dp = 15.dp
        val leftStackViewFixedSpaceWidth: Dp = 10.dp
        val leftStackViewWidthConstant: Dp = 40.dp
        val sendButtonTrailingAnchorConstraintConstantDecrement: Dp = 15.dp
        val textContainerInsetRightIncrement: Dp = 10.dp
    }

    object MediaActionHandler {
        const val IMAGE_COMPRESSION_SIZE_KB = 1000
        const val THUMBNAIL_FRAME_TIME_MICROSECONDS = 1_000_000L
        const val THUMBNAIL_IMAGE_SCALE = 2
        const val THUMBNAIL_IMAGE_SIZE_HEIGHT = 500
        const val THUMBNAIL_IMAGE_SIZE_WIDTH = 500

        // A medium-quality transcode target: the video height is scaled to
        // 480 px (H.264 video, AAC audio), preserving the aspect ratio,
        // with the encoder's default medium bitrate.
        const val VIDEO_TARGET_HEIGHT = 480
    }

    object RecipientBarService {
        object ActionHandler {
            const val BECOME_FIRST_RESPONDER_DELAY_MILLISECONDS = 10L
            const val PRESENT_CONTACT_CTA_DELAY_MILLISECONDS = 500L
            const val SUBLEVEL_COUNT = 10
        }

        object ContactSelectionUI {
            val adjacentViewSpacing: Dp = 2.dp

            val contactViewCornerRadius: Dp = 6.dp
            val contactViewFrameHeight: Dp = 30.dp
            val contactViewWidthIncrement: Dp = 10.dp
            val contactViewXOriginIncrement: Dp = 5.dp

            const val CONTACT_LABEL_SYSTEM_FONT_SIZE = 16f
            const val CONTACT_VIEW_MAXIMUM_WIDTH_DIVISOR = 2
            const val SELECTED_CONTACT_PAIRS_MAXIMUM = 10
            const val SUBLEVEL_COUNT = 10
        }

        object Layout {
            val borderHeight: Dp = 0.3.dp

            val frameHeight: Dp = 54.dp

            val selectContactButtonFrameHeight: Dp = 26.dp
            val selectContactButtonFrameWidth: Dp = 26.dp
            val selectContactButtonMinXDecrement: Dp = 5.dp
            val selectContactButtonXOriginDecrement: Dp = 10.dp

            val textFieldXOriginIncrement: Dp = 5.dp
            val toLabelXOrigin: Dp = 15.dp

            const val LIGHT_BACKGROUND_COLOR_ALPHA_COMPONENT = 0.98f
            const val TO_LABEL_FONT_SIZE = 14f
        }

        object UITextFieldDelegate {
            const val TOGGLE_LABEL_REPRESENTATION_DELAY_MILLISECONDS = 10L
        }
    }
}

// MARK: - Color

object ChatPageViewServiceColors {
    val messagesCollectionViewPrimaryDarkBackground = Color.Black
    val messagesCollectionViewSecondaryDarkBackground = Color(0xFF1C1C1E)

    object AudioMessagePlayback {
        val cellCurrentUserProgressViewTint = Color.White
    }

    object DeliveryProgressIndicator {
        val progressBarTint = Color(0xFF007AFF)
    }

    object InputBar {
        val inputTextViewAlternateTint = Color.Transparent
        val inputTextViewLayerBorder = Color(0xFF8E8E93)
        val sendButtonTextTint = Color(0xFF007AFF)
    }

    object RecipientBarService {
        object ContactSelectionUI {
            val accent = Color(0xFF007AFF)

            val contactViewDarkSelection = Color(0xFF2A2A2C)
            val contactViewHighlightedText = Color.White
            val contactViewLightSelection = Color(0xFFECF0F1)
            val contactViewRedText = Color(0xFF34C759)

            val labelRepresentationColor = Color.Transparent
        }

        object Layout {
            val darkBorder = Color(0x4A3C3C43)
            val lightBorder = Color(0xFFDCDCDD)

            val lightBackground = Color.White

            val toLabelText = Color.Gray
        }
    }
}

// MARK: - String

object ChatPageViewServiceStrings {
    object AudioMessagePlayback {
        const val CELL_DEFAULT_DURATION_LABEL_TEXT = "0:00"
    }

    object ContextMenu {
        const val AUDIO_MESSAGE_ACTION_ALTERNATE_IMAGE_SYSTEM_NAME = "text.bubble"
        const val AUDIO_MESSAGE_ACTION_IDENTIFIER_RAW_VALUE = "audio_message"
        const val AUDIO_MESSAGE_ACTION_IMAGE_SYSTEM_NAME = "speaker.wave.2.bubble"

        const val COPY_ACTION_IDENTIFIER_RAW_VALUE = "copy"
        const val COPY_ACTION_IMAGE_SYSTEM_NAME = "doc.on.doc"

        const val REACTION_DETAILS_ACTION_IDENTIFIER_RAW_VALUE = "reaction_details"
        const val REACTION_DETAILS_ACTION_IMAGE_SYSTEM_NAME = "info.circle"

        const val REPORT_MISTRANSLATION_ACTION_IDENTIFIER_RAW_VALUE = "report_mistranslation"
        const val REPORT_MISTRANSLATION_ACTION_IMAGE_SYSTEM_NAME = "flag"

        const val RETRY_TRANSLATION_ACTION_IDENTIFIER_RAW_VALUE = "retry_translation"
        const val RETRY_TRANSLATION_ACTION_IMAGE_SYSTEM_NAME = "arrow.counterclockwise"

        const val SAVE_ACTION_IDENTIFIER_RAW_VALUE = "save"
        const val SAVE_ACTION_IMAGE_SYSTEM_NAME = "square.and.arrow.down"

        const val SPEAK_ACTION_ALTERNATE_IMAGE_SYSTEM_NAME = "speaker.slash.circle"
        const val SPEAK_ACTION_IDENTIFIER_RAW_VALUE = "speak"
        const val SPEAK_ACTION_IMAGE_SYSTEM_NAME = "speaker.wave.2.circle"

        const val VIEW_ALTERNATE_ACTION_IDENTIFIER_RAW_VALUE = "view_alternate"
        const val VIEW_ALTERNATE_ACTION_IMAGE_SYSTEM_NAME = "globe"
    }

    object DeliveryProgressIndicator {
        const val VIEW_SEMANTIC_TAG = "DELIVERY_PROGRESS_VIEW"
    }

    object InputBar {
        const val ATTACH_MEDIA_BUTTON_IMAGE_SYSTEM_NAME = "plus"
        const val ATTACH_MEDIA_BUTTON_SEMANTIC_TAG = "ATTACH_MEDIA_BUTTON"
        const val SEND_BUTTON_IMAGE_SYSTEM_NAME = "arrow.up.circle.fill"
        const val SEND_BUTTON_OFFLINE_IMAGE_SYSTEM_NAME = "wifi.slash"
        const val SEND_BUTTON_SEMANTIC_TAG = "SEND_BUTTON"
    }

    object MediaActionHandler {
        const val DEFAULT_DOCUMENT_NAME = "document"
        const val DEFAULT_IMAGE_NAME = "image"
        const val DEFAULT_VIDEO_NAME = "video"
    }

    object RecipientBarService {
        object ContactSelectionUI {
            const val CONTACT_LABEL_SEMANTIC_TAG = "CONTACT_LABEL"
            const val CONTACT_VIEW_SEMANTIC_TAG = "CONTACT_VIEW"
        }

        object Layout {
            const val RECIPIENT_BAR_SEMANTIC_TAG = "RECIPIENT_BAR"
            const val SELECT_CONTACT_BUTTON_SEMANTIC_TAG = "SELECT_CONTACT_BUTTON"

            const val TABLE_VIEW_SEMANTIC_TAG = "TABLE_VIEW"

            const val TEXT_FIELD_SEMANTIC_TAG = "TEXT_FIELD"

            const val TO_LABEL_SEMANTIC_TAG = "TO_LABEL"
        }
    }
}
