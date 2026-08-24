//
//  SystemFontFamily.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.ui.text.font.FontFamily

/**
 * The system font family for **release builds**: Inter, a
 * metric-compatible stand-in for San Francisco (which is licensed for
 * Apple platforms and therefore excluded from shipping builds). The
 * real San Francisco typeface is used only in debug builds via the
 * `src/debug` variant of this declaration.
 */
val SystemFontFamily: FontFamily = InterFontFamily
