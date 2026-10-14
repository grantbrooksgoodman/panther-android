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

/** The layout and timing values of the chat page view service. */
object ChatPageViewServiceFloats {
    /** The duration, in seconds, of the input bar's appearance animation. */
    const val INPUT_BAR_APPEARANCE_ANIMATION_DURATION = 0.2f

    /** The delay, in milliseconds, before older messages load. */
    const val LOAD_MORE_MESSAGES_DELAY_MILLISECONDS = 200L

    /** The debounce interval, in milliseconds, for outbox changes. */
    const val OUTBOX_CHANGE_DEBOUNCE_MILLISECONDS = 250L

    /** The delay, in milliseconds, before scrolling. */
    const val SCROLL_DELAY_MILLISECONDS = 10L

    /** The debounce interval, in milliseconds, for session store changes. */
    const val SESSION_STORE_CHANGE_DEBOUNCE_MILLISECONDS = 250L

    /** The delay, in milliseconds, before the focused message cell's interaction triggers. */
    const val TRIGGER_FOCUSED_MESSAGE_CELL_INTERACTION_DELAY_MILLISECONDS = 500L

    /** The bottom inset of the preview configuration. */
    val previewConfigBottomInset: Dp = 8.dp

    /** The values of audio message playback. */
    object AudioMessagePlayback {
        /** The interval, in seconds, of the playback progress timer. */
        const val PLAYBACK_TIMER_TIME_INTERVAL = 0.1f

        /** The delay, in milliseconds, before the next message plays. */
        const val PLAY_NEXT_MESSAGE_DELAY_MILLISECONDS = 100L
    }

    /** The values of the context menu. */
    object ContextMenu {
        /** The number of taps that make up a double tap. */
        const val DOUBLE_TAP_GESTURE_NUMBER_OF_TAPS_REQUIRED = 2

        /** The interval, in seconds, of the interaction timer. */
        const val INTERACTION_TIMER_TIME_INTERVAL = 0.1f

        /** The delay, in milliseconds, before scrolling to the last item after an interaction. */
        const val INTERACTION_SCROLL_TO_LAST_ITEM_DELAY_MILLISECONDS = 60L

        /** The confidence above which language recognition counts as a match. */
        const val LANGUAGE_RECOGNITION_MATCH_CONFIDENCE_THRESHOLD = 0.8f

        /** The minimum duration, in seconds, of a long press. */
        const val LONG_PRESS_GESTURE_MINIMUM_PRESS_DURATION = 0.5f

        /** The horizontal scale factor of the lifted content. */
        const val MENU_STYLE_TRANSFORM_SCALE_X = 1.08f

        /** The vertical scale factor of the lifted content. */
        const val MENU_STYLE_TRANSFORM_SCALE_Y = 1.08f

        /** The delay, in milliseconds, before scrolling to the last item after a reaction. */
        const val REACTION_SCROLL_TO_LAST_ITEM_DELAY_MILLISECONDS = 200L

        /** The delay, in milliseconds, before an existing selection triggers. */
        const val TRIGGER_EXISTING_SELECTION_DELAY_MILLISECONDS = 10L

        /** The bottom margin of the menu. */
        val menuStyleBottomMargin: Dp = 8.dp

        /** The top margin of the menu. */
        val menuStyleTopMargin: Dp = 8.dp
    }

    /** The values of the delivery progress indicator. */
    object DeliveryProgressIndicator {
        /** The delay, in seconds, before the indicator's animation. */
        const val ANIMATION_DELAY = 1f

        /** The duration, in seconds, of the indicator's animation. */
        const val ANIMATION_DURATION = 0.2f

        /** The interval, in seconds, after which the indicator appears. */
        const val APPEARANCE_TIMER_TIME_INTERVAL = 5f

        /** The interval, in seconds, of the progress timer while the indicator is hidden. */
        const val HIDDEN_TIMER_TIME_INTERVAL = 0.05f

        /** The progress added on each timer tick. */
        const val TIMER_PROGRESS_INCREMENT = 0.001f

        /** The progress beyond which the timer stops adding progress. */
        const val TIMER_PROGRESS_INCREMENT_THRESHOLD = 0.9f

        /** The interval, in seconds, of the progress timer while the indicator is visible. */
        const val VISIBLE_TIMER_TIME_INTERVAL = 0.01f

        /** The height of the indicator. */
        val viewFrameHeight: Dp = 2.dp
    }

    /** The values of the input bar. */
    object InputBar {
        /** The horizontal scale factor of a selected button. */
        const val BUTTON_ON_SELECTED_TRANSFORM_SCALE_X = 1.1f

        /** The vertical scale factor of a selected button. */
        const val BUTTON_ON_SELECTED_TRANSFORM_SCALE_Y = 1.1f

        /** The delay, in milliseconds, before the bar's appearance is forced. */
        const val FORCE_APPEARANCE_DELAY_MILLISECONDS = 200L

        /** The duration, in seconds, of the transition animation. */
        const val TRANSITION_ANIMATION_DURATION = 0.3f

        /** The height of a button. */
        val buttonSizeHeight: Dp = 30.dp

        /** The width of a button. */
        val buttonSizeWidth: Dp = 30.dp

        /** The border width of the text field. */
        val layerBorderWidth: Dp = 0.5.dp

        /** The corner radius of the text field. */
        val layerCornerRadius: Dp = 15.dp

        /** The width of the fixed space in the left stack view. */
        val leftStackViewFixedSpaceWidth: Dp = 10.dp

        /** The width of the left stack view. */
        val leftStackViewWidthConstant: Dp = 40.dp

        /** The amount by which the send button's trailing inset is reduced. */
        val sendButtonTrailingAnchorConstraintConstantDecrement: Dp = 15.dp

        /** The amount added to the text container's right inset. */
        val textContainerInsetRightIncrement: Dp = 10.dp
    }

    /** The values of the media action handler. */
    object MediaActionHandler {
        /** The target size, in kilobytes, of a compressed image. */
        const val IMAGE_COMPRESSION_SIZE_KB = 1000

        /** The time, in microseconds, of the video frame used as a thumbnail. */
        const val THUMBNAIL_FRAME_TIME_MICROSECONDS = 1_000_000L

        /** The scale factor of a thumbnail image. */
        const val THUMBNAIL_IMAGE_SCALE = 2

        /** The height of a thumbnail image. */
        const val THUMBNAIL_IMAGE_SIZE_HEIGHT = 500

        /** The width of a thumbnail image. */
        const val THUMBNAIL_IMAGE_SIZE_WIDTH = 500

        // A medium-quality transcode target: the video height is scaled to
        // 480 px (H.264 video, AAC audio), preserving the aspect ratio,
        // with the encoder's default medium bitrate.

        /** The target height of a compressed video. */
        const val VIDEO_TARGET_HEIGHT = 480
    }

    /** The values of the recipient bar service. */
    object RecipientBarService {
        /** The values of the action handler. */
        object ActionHandler {
            /** The delay, in milliseconds, before the field takes focus. */
            const val BECOME_FIRST_RESPONDER_DELAY_MILLISECONDS = 10L

            /** The delay, in milliseconds, before the contact access call to action presents. */
            const val PRESENT_CONTACT_CTA_DELAY_MILLISECONDS = 500L

            /** The number of sublevels. */
            const val SUBLEVEL_COUNT = 10
        }

        /** The values of the contact selection UI. */
        object ContactSelectionUI {
            /** The spacing between adjacent contact views. */
            val adjacentViewSpacing: Dp = 2.dp

            /** The corner radius of the contact view. */
            val contactViewCornerRadius: Dp = 6.dp

            /** The frame height of the contact view. */
            val contactViewFrameHeight: Dp = 30.dp

            /** The amount added to a contact view's width. */
            val contactViewWidthIncrement: Dp = 10.dp

            /** The amount added to a contact view's horizontal origin. */
            val contactViewXOriginIncrement: Dp = 5.dp

            /** The font size of a contact label. */
            const val CONTACT_LABEL_SYSTEM_FONT_SIZE = 16f

            /** The divisor applied to the screen width to cap a contact view's width. */
            const val CONTACT_VIEW_MAXIMUM_WIDTH_DIVISOR = 2

            /** The maximum number of selected contact pairs. */
            const val SELECTED_CONTACT_PAIRS_MAXIMUM = 10

            /** The number of sublevels. */
            const val SUBLEVEL_COUNT = 10
        }

        /** The values of the layout. */
        object Layout {
            /** The height of the border. */
            val borderHeight: Dp = 0.3.dp

            /** The height of the bar. */
            val frameHeight: Dp = 54.dp

            /** The frame height of the select contact button. */
            val selectContactButtonFrameHeight: Dp = 26.dp

            /** The frame width of the select contact button. */
            val selectContactButtonFrameWidth: Dp = 26.dp

            /**
             * The amount subtracted from the select contact button's minimum
             * horizontal position.
             */
            val selectContactButtonMinXDecrement: Dp = 5.dp

            /** The amount subtracted from the select contact button's horizontal origin. */
            val selectContactButtonXOriginDecrement: Dp = 10.dp

            /** The amount added to the text field's horizontal origin. */
            val textFieldXOriginIncrement: Dp = 5.dp

            /** The horizontal origin of the To label. */
            val toLabelXOrigin: Dp = 15.dp

            /** The opacity of the background color in light mode. */
            const val LIGHT_BACKGROUND_COLOR_ALPHA_COMPONENT = 0.98f

            /** The font size of the To label. */
            const val TO_LABEL_FONT_SIZE = 14f
        }

        /** The values of the text field delegate. */
        object UITextFieldDelegate {
            /** The delay, in milliseconds, before the label representation toggles. */
            const val TOGGLE_LABEL_REPRESENTATION_DELAY_MILLISECONDS = 10L
        }
    }
}

// MARK: - Color

/** The colors of the chat page view service. */
object ChatPageViewServiceColors {
    /** The primary background color of the messages list in dark mode. */
    val messagesCollectionViewPrimaryDarkBackground = Color.Black

    /** The secondary background color of the messages list in dark mode. */
    val messagesCollectionViewSecondaryDarkBackground = Color(0xFF1C1C1E)

    /** The values of audio message playback. */
    object AudioMessagePlayback {
        /** The tint color of the current user's cell progress view. */
        val cellCurrentUserProgressViewTint = Color.White
    }

    /** The values of the delivery progress indicator. */
    object DeliveryProgressIndicator {
        /** The tint color of the progress bar. */
        val progressBarTint = Color(0xFF007AFF)
    }

    /** The values of the input bar. */
    object InputBar {
        /** The alternate tint color of the input text view. */
        val inputTextViewAlternateTint = Color.Transparent

        /** The border color of the input text view. */
        val inputTextViewLayerBorder = Color(0xFF8E8E93)

        /** The tint color of the send button's text. */
        val sendButtonTextTint = Color(0xFF007AFF)
    }

    /** The values of the recipient bar service. */
    object RecipientBarService {
        /** The values of the contact selection UI. */
        object ContactSelectionUI {
            /** The accent color. */
            val accent = Color(0xFF007AFF)

            /** The selection color of a contact view in dark mode. */
            val contactViewDarkSelection = Color(0xFF2A2A2C)

            /** The text color of a highlighted contact view. */
            val contactViewHighlightedText = Color.White

            /** The selection color of a contact view in light mode. */
            val contactViewLightSelection = Color(0xFFECF0F1)

            /** The text color of a contact view for a mock recipient. */
            val contactViewRedText = Color(0xFF34C759)

            /** The color of a contact view in its label representation. */
            val labelRepresentationColor = Color.Transparent
        }

        /** The values of the layout. */
        object Layout {
            /** The border color in dark mode. */
            val darkBorder = Color(0x4A3C3C43)

            /** The border color in light mode. */
            val lightBorder = Color(0xFFDCDCDD)

            /** The background color in light mode. */
            val lightBackground = Color.White

            /** The text color of the To label. */
            val toLabelText = Color.Gray
        }
    }
}

// MARK: - String

/** The string constants of the chat page view service. */
object ChatPageViewServiceStrings {
    /** The values of audio message playback. */
    object AudioMessagePlayback {
        /** The default text of an audio cell's duration label. */
        const val CELL_DEFAULT_DURATION_LABEL_TEXT = "0:00"
    }

    /** The values of the context menu. */
    object ContextMenu {
        /** The system name of the audio message action's alternate image. */
        const val AUDIO_MESSAGE_ACTION_ALTERNATE_IMAGE_SYSTEM_NAME = "text.bubble"

        /** The identifier of the audio message action. */
        const val AUDIO_MESSAGE_ACTION_IDENTIFIER_RAW_VALUE = "audio_message"

        /** The system name of the audio message action image. */
        const val AUDIO_MESSAGE_ACTION_IMAGE_SYSTEM_NAME = "speaker.wave.2.bubble"

        /** The identifier of the copy action. */
        const val COPY_ACTION_IDENTIFIER_RAW_VALUE = "copy"

        /** The system name of the copy action image. */
        const val COPY_ACTION_IMAGE_SYSTEM_NAME = "doc.on.doc"

        /** The identifier of the reaction details action. */
        const val REACTION_DETAILS_ACTION_IDENTIFIER_RAW_VALUE = "reaction_details"

        /** The system name of the reaction details action image. */
        const val REACTION_DETAILS_ACTION_IMAGE_SYSTEM_NAME = "info.circle"

        /** The identifier of the report mistranslation action. */
        const val REPORT_MISTRANSLATION_ACTION_IDENTIFIER_RAW_VALUE = "report_mistranslation"

        /** The system name of the report mistranslation action image. */
        const val REPORT_MISTRANSLATION_ACTION_IMAGE_SYSTEM_NAME = "flag"

        /** The identifier of the retry translation action. */
        const val RETRY_TRANSLATION_ACTION_IDENTIFIER_RAW_VALUE = "retry_translation"

        /** The system name of the retry translation action image. */
        const val RETRY_TRANSLATION_ACTION_IMAGE_SYSTEM_NAME = "arrow.counterclockwise"

        /** The identifier of the save action. */
        const val SAVE_ACTION_IDENTIFIER_RAW_VALUE = "save"

        /** The system name of the save action image. */
        const val SAVE_ACTION_IMAGE_SYSTEM_NAME = "square.and.arrow.down"

        /** The system name of the speak action's alternate image. */
        const val SPEAK_ACTION_ALTERNATE_IMAGE_SYSTEM_NAME = "speaker.slash.circle"

        /** The identifier of the speak action. */
        const val SPEAK_ACTION_IDENTIFIER_RAW_VALUE = "speak"

        /** The system name of the speak action image. */
        const val SPEAK_ACTION_IMAGE_SYSTEM_NAME = "speaker.wave.2.circle"

        /** The identifier of the view alternate action. */
        const val VIEW_ALTERNATE_ACTION_IDENTIFIER_RAW_VALUE = "view_alternate"

        /** The system name of the view alternate action image. */
        const val VIEW_ALTERNATE_ACTION_IMAGE_SYSTEM_NAME = "globe"
    }

    /** The values of the delivery progress indicator. */
    object DeliveryProgressIndicator {
        /** The semantic tag of the view. */
        const val VIEW_SEMANTIC_TAG = "DELIVERY_PROGRESS_VIEW"
    }

    /** The values of the input bar. */
    object InputBar {
        /** The system name of the attach media button image. */
        const val ATTACH_MEDIA_BUTTON_IMAGE_SYSTEM_NAME = "plus"

        /** The semantic tag of the attach media button. */
        const val ATTACH_MEDIA_BUTTON_SEMANTIC_TAG = "ATTACH_MEDIA_BUTTON"

        /** The system name of the send button image. */
        const val SEND_BUTTON_IMAGE_SYSTEM_NAME = "arrow.up.circle.fill"

        /** The system name of the send button offline image. */
        const val SEND_BUTTON_OFFLINE_IMAGE_SYSTEM_NAME = "wifi.slash"

        /** The semantic tag of the send button. */
        const val SEND_BUTTON_SEMANTIC_TAG = "SEND_BUTTON"
    }

    /** The values of the media action handler. */
    object MediaActionHandler {
        /** The default file name of a document. */
        const val DEFAULT_DOCUMENT_NAME = "document"

        /** The default file name of an image. */
        const val DEFAULT_IMAGE_NAME = "image"

        /** The default file name of a video. */
        const val DEFAULT_VIDEO_NAME = "video"
    }

    /** The values of the recipient bar service. */
    object RecipientBarService {
        /** The values of the contact selection UI. */
        object ContactSelectionUI {
            /** The semantic tag of the contact label. */
            const val CONTACT_LABEL_SEMANTIC_TAG = "CONTACT_LABEL"

            /** The semantic tag of the contact view. */
            const val CONTACT_VIEW_SEMANTIC_TAG = "CONTACT_VIEW"
        }

        /** The values of the layout. */
        object Layout {
            /** The semantic tag of the recipient bar. */
            const val RECIPIENT_BAR_SEMANTIC_TAG = "RECIPIENT_BAR"

            /** The semantic tag of the select contact button. */
            const val SELECT_CONTACT_BUTTON_SEMANTIC_TAG = "SELECT_CONTACT_BUTTON"

            /** The semantic tag of the suggestions list. */
            const val TABLE_VIEW_SEMANTIC_TAG = "TABLE_VIEW"

            /** The semantic tag of the text field. */
            const val TEXT_FIELD_SEMANTIC_TAG = "TEXT_FIELD"

            /** The semantic tag of the to label. */
            const val TO_LABEL_SEMANTIC_TAG = "TO_LABEL"
        }
    }
}
