//
//  SanFranciscoFontFamily.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import us.neotechnica.panther.designsystem.R
import androidx.compose.ui.text.font.Font as ComposeFont

/**
 * The San Francisco typeface.
 *
 * [SystemFontFamily] uses this family while [Font.usesSanFrancisco]
 * is `true`.
 */
val SanFranciscoFontFamily: FontFamily =
    FontFamily(
        ComposeFont(R.font.sf_light, FontWeight.Light),
        ComposeFont(R.font.sf_regular, FontWeight.Normal),
        ComposeFont(R.font.sf_medium, FontWeight.Medium),
        ComposeFont(R.font.sf_semibold, FontWeight.SemiBold),
        ComposeFont(R.font.sf_bold, FontWeight.Bold),
        ComposeFont(R.font.sf_heavy, FontWeight.Black),
        ComposeFont(R.font.sf_light_italic, FontWeight.Light, FontStyle.Italic),
        ComposeFont(R.font.sf_regular_italic, FontWeight.Normal, FontStyle.Italic),
        ComposeFont(R.font.sf_medium_italic, FontWeight.Medium, FontStyle.Italic),
        ComposeFont(R.font.sf_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
        ComposeFont(R.font.sf_bold_italic, FontWeight.Bold, FontStyle.Italic),
        ComposeFont(R.font.sf_heavy_italic, FontWeight.Black, FontStyle.Italic),
    )
