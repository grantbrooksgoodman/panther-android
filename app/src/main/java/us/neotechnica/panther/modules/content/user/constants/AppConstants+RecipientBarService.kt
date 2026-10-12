//
//  AppConstants+RecipientBarService.kt
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
            const val LABEL_REPRESENTATION_ANIMATION_DURATION_MILLISECONDS = 300
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
    object RecipientBarService {
        object ContactSelectionUI {
            const val CONTACT_LABEL_SEMANTIC_TAG = "CONTACT_LABEL"
            const val CONTACT_VIEW_SEMANTIC_TAG = "CONTACT_VIEW"
        }

        object Layout {
            const val RECIPIENT_BAR_SEMANTIC_TAG = "RECIPIENT_BAR"
            const val SELECT_CONTACT_BUTTON_IMAGE_SYSTEM_NAME = "plus.circle"
            const val SELECT_CONTACT_BUTTON_SEMANTIC_TAG = "SELECT_CONTACT_BUTTON"

            const val TABLE_VIEW_SEMANTIC_TAG = "TABLE_VIEW"

            const val TEXT_FIELD_SEMANTIC_TAG = "TEXT_FIELD"

            const val TO_LABEL_SEMANTIC_TAG = "TO_LABEL"
        }
    }
}
