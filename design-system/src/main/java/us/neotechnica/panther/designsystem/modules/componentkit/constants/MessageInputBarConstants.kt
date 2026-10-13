//
//  MessageInputBarConstants.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object MessageInputBarFloats {
    /** The opacity of a button image while the button is disabled. */
    const val DISABLED_IMAGE_ALPHA = 0.4f
    const val DIVIDER_ALPHA = 0.15f
    const val FIELD_MAX_LINES = 5

    /**
     * The spacing between the bottom of the text field and the bottom of
     * the attach and send buttons, centering the buttons on a single line
     * of text.
     */
    val buttonBottomPadding: Dp = 4.dp

    val buttonSizeHeight: Dp = 30.dp
    val buttonSizeWidth: Dp = 30.dp

    val dividerHeight: Dp = 0.5.dp
    val glyphSize: Dp = 28.dp
    val horizontalPadding: Dp = 12.dp

    val indicatorSize: Dp = 20.dp
    val indicatorStrokeWidth: Dp = 2.dp

    /** The text field's border width: a single-pixel hairline. */
    val layerBorderWidth: Dp = Dp.Hairline
    val layerCornerRadius: Dp = 15.dp

    val leftStackViewFixedSpaceWidth: Dp = 10.dp
    val leftStackViewWidthConstant: Dp = 40.dp

    /** The spacing between the text field and the bar's trailing padding. */
    val middleContentViewPaddingRight: Dp = 8.dp

    val sendButtonTrailingAnchorConstraintConstantDecrement: Dp = 15.dp

    val textContainerInsetLeft: Dp = 12.dp
    val textContainerInsetRightIncrement: Dp = 10.dp
    val textContainerInsetVertical: Dp = 9.dp

    val verticalPadding: Dp = 6.dp
}

// MARK: - Color

object MessageInputBarColors {
    val inputTextViewAlternateTint = Color.Transparent
    val inputTextViewLayerBorder = Color(0xFF8E8E93)
}

// MARK: - String

object MessageInputBarStrings {
    const val ATTACH_MEDIA_BUTTON_CONTENT_DESCRIPTION = "Attach media"
    const val ATTACH_MEDIA_BUTTON_IMAGE_SYSTEM_NAME = "plus"
    const val SEND_BUTTON_CONTENT_DESCRIPTION = "Send"
    const val SEND_BUTTON_IMAGE_SYSTEM_NAME = "arrow.up.circle.fill"
}
