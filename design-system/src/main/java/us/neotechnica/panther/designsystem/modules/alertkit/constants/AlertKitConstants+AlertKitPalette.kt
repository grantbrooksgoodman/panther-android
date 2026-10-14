//
//  AlertKitConstants+AlertKitPalette.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

/** The opacities of the AlertKit palette. */
object AlertKitPaletteFloats {
    /** The opacity of the dimming behind an alert. */
    const val DIMMING_OPACITY = 0.2f

    /** The opacity of a pressed button's highlight. */
    const val HIGHLIGHT_OPACITY = 0.1f
}

// MARK: - Color

/** The colors of the AlertKit palette. */
object AlertKitPaletteColors {
    /** The color of an action label in dark mode. */
    val ACTION_LABEL_DARK = Color(0xFF4693FF)

    /** The color of a destructive action label in dark mode. */
    val ACTION_LABEL_DESTRUCTIVE_DARK = Color(0xFFFF4245)

    /** The color of a destructive action label in light mode. */
    val ACTION_LABEL_DESTRUCTIVE_LIGHT = Color(0xFFFF383C)

    /** The color of a disabled action label in dark mode. */
    val ACTION_LABEL_DISABLED_DARK = Color(0xFF626265)

    /** The color of a disabled action label in light mode. */
    val ACTION_LABEL_DISABLED_LIGHT = Color(0xFFBABABC)

    /** The color of an action label in light mode. */
    val ACTION_LABEL_LIGHT = Color(0xFF007AFF)

    /** The background color in dark mode. */
    val BACKGROUND_DARK = Color(0xFF272727)

    /** The background color in light mode. */
    val BACKGROUND_LIGHT = Color(0xFFF0F0F0)

    /** The background color of the cancel button in dark mode. */
    val CANCEL_BUTTON_BACKGROUND_DARK = Color(0xFF2C2C2E)

    /** The background color of the cancel button in light mode. */
    val CANCEL_BUTTON_BACKGROUND_LIGHT = Color.White

    /** The color of the clear button in dark mode. */
    val CLEAR_BUTTON_DARK = Color(0xFF333333)

    /** The color of the clear button in light mode. */
    val CLEAR_BUTTON_LIGHT = Color(0xFFCCCCCC)

    /** The color of the text field's cursor. */
    val CURSOR = Color(0xFF426BF2)

    /** The color of the dimming behind an alert. */
    val DIMMING = Color.Black

    /** The color of a pressed button's highlight in dark mode. */
    val HIGHLIGHT_DARK = Color.White

    /** The color of a pressed button's highlight in light mode. */
    val HIGHLIGHT_LIGHT = Color.Black

    /** The color of a label in dark mode. */
    val LABEL_DARK = Color.White

    /** The color of a label in light mode. */
    val LABEL_LIGHT = Color.Black

    /** The color of the text field's placeholder in dark mode. */
    val PLACEHOLDER_DARK = Color(0xFF464649)

    /** The color of the text field's placeholder in light mode. */
    val PLACEHOLDER_LIGHT = Color(0xFFC4C4C7)

    /** The fill color of the progress bar in dark mode. */
    val PROGRESS_FILL_DARK = Color(0xFF0090FD)

    /** The fill color of the progress bar in light mode. */
    val PROGRESS_FILL_LIGHT = Color(0xFF0087FD)

    /** The track color of the progress bar in dark mode. */
    val PROGRESS_TRACK_DARK = Color(0xFF404042)

    /** The track color of the progress bar in light mode. */
    val PROGRESS_TRACK_LIGHT = Color(0xFFD6D7D7)

    /** The color of a secondary label in dark mode. */
    val SECONDARY_LABEL_DARK = Color(0xFF9E9E9E)

    /** The color of a secondary label in light mode. */
    val SECONDARY_LABEL_LIGHT = Color(0xFF828484)

    /** The color of a separator in dark mode. */
    val SEPARATOR_DARK = Color(0xFF666868)

    /** The color of a separator in light mode. */
    val SEPARATOR_LIGHT = Color(0xFFB2B2B4)

    /** The background color of the text field in dark mode. */
    val TEXT_FIELD_BACKGROUND_DARK = Color.Black

    /** The background color of the text field in light mode. */
    val TEXT_FIELD_BACKGROUND_LIGHT = Color.White
}
