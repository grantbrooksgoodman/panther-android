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

/** The layout and timing values of the message input bar. */
object MessageInputBarFloats {
    /** The opacity of a button image while the button is disabled. */
    const val DISABLED_IMAGE_ALPHA = 0.4f

    /** The opacity of the divider. */
    const val DIVIDER_ALPHA = 0.15f

    /** The maximum number of lines of the field. */
    const val FIELD_MAX_LINES = 5

    /**
     * The spacing between the bottom of the text field and the bottom of
     * the attach and send buttons, centering the buttons on a single line
     * of text.
     */
    val buttonBottomPadding: Dp = 4.dp

    /** The height of a button. */
    val buttonSizeHeight: Dp = 30.dp

    /** The width of a button. */
    val buttonSizeWidth: Dp = 30.dp

    /** The height of the divider. */
    val dividerHeight: Dp = 0.5.dp

    /** The size of a button glyph. */
    val glyphSize: Dp = 28.dp

    /** The horizontal padding of the bar. */
    val horizontalPadding: Dp = 12.dp

    /** The size of the sending indicator. */
    val indicatorSize: Dp = 20.dp

    /** The stroke width of the sending indicator. */
    val indicatorStrokeWidth: Dp = 2.dp

    /** The text field's border width: a single-pixel hairline. */
    val layerBorderWidth: Dp = Dp.Hairline

    /** The corner radius of the text field. */
    val layerCornerRadius: Dp = 15.dp

    /** The width of the fixed space in the left stack view. */
    val leftStackViewFixedSpaceWidth: Dp = 10.dp

    /** The width of the left stack view. */
    val leftStackViewWidthConstant: Dp = 40.dp

    /** The spacing between the text field and the bar's trailing padding. */
    val middleContentViewPaddingRight: Dp = 8.dp

    /** The amount by which the send button's trailing inset is reduced. */
    @Suppress("VariableMaxLength")
    val sendButtonTrailingAnchorConstraintConstantDecrement: Dp = 15.dp

    /** The left inset of the text container. */
    val textContainerInsetLeft: Dp = 12.dp

    /** The amount added to the text container's right inset. */
    val textContainerInsetRightIncrement: Dp = 10.dp

    /** The vertical inset of the text container. */
    val textContainerInsetVertical: Dp = 9.dp

    /** The vertical padding of the bar. */
    val verticalPadding: Dp = 6.dp
}

// MARK: - Color

/** The colors of the message input bar. */
object MessageInputBarColors {
    /** The alternate tint color of the input text view. */
    val inputTextViewAlternateTint = Color.Transparent

    /** The border color of the input text view. */
    val inputTextViewLayerBorder = Color(0xFF8E8E93)
}

// MARK: - String

/** The string constants of the message input bar. */
object MessageInputBarStrings {
    /** The accessibility description of the attach media button. */
    const val ATTACH_MEDIA_BUTTON_CONTENT_DESCRIPTION = "Attach media"

    /** The accessibility description of the send button. */
    const val SEND_BUTTON_CONTENT_DESCRIPTION = "Send"
}
