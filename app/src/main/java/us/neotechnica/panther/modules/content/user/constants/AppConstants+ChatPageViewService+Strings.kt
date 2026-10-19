//
//  AppConstants+ChatPageViewService+Strings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

@file:Suppress("VariableMaxLength")

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
