//
//  InterFontFamily.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import us.neotechnica.panther.designsystem.R

/**
 * The Inter typeface, used as the system font on Android in place of
 * San Francisco (which is licensed for Apple platforms only). Inter is
 * chosen for its close metric compatibility with San Francisco.
 *
 * The family is backed by two variable fonts (upright and italic); each
 * declared weight selects the corresponding weight axis. On API levels
 * below 26, where variable weight axes are not honored, the closest
 * static rendering is used.
 */
val InterFontFamily: FontFamily by lazy {
    FontFamily(
        weightFont(FontWeight.Light, italic = false),
        weightFont(FontWeight.Normal, italic = false),
        weightFont(FontWeight.Medium, italic = false),
        weightFont(FontWeight.SemiBold, italic = false),
        weightFont(FontWeight.Bold, italic = false),
        weightFont(FontWeight.Light, italic = true),
        weightFont(FontWeight.Normal, italic = true),
        weightFont(FontWeight.Medium, italic = true),
        weightFont(FontWeight.SemiBold, italic = true),
        weightFont(FontWeight.Bold, italic = true),
    )
}

@OptIn(ExperimentalTextApi::class)
private fun weightFont(
    weight: FontWeight,
    italic: Boolean,
): Font =
    Font(
        resId = if (italic) R.font.inter_variable_italic else R.font.inter_variable,
        weight = weight,
        style = if (italic) FontStyle.Italic else FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )
