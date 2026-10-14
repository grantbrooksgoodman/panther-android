//
//  SystemFontFamily.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.ui.text.font.FontFamily

/**
 * The font family of the system font.
 *
 * The family is San Francisco while [Font.usesSanFrancisco] is `true`;
 * otherwise, it is Inter, whose metrics match San Francisco's.
 */
val SystemFontFamily: FontFamily
    get() = if (Font.usesSanFrancisco) SanFranciscoFontFamily else InterFontFamily
