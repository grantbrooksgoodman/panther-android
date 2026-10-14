//
//  AppConstants+ContactPairCellView.kt
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

/** The layout and timing values of the contact pair cell view. */
object ContactPairCellViewFloats {
    /** The spacing of the row. */
    val hStackSpacing: Dp = 3.5.dp
}

// MARK: - Color

// NIT: Unused on Android.

/** The colors of the contact pair cell view. */
object ContactPairCellViewColors {
    /** The background color while prevarication mode is on. */
    val prevaricationModeBackground = Color(0xFFF3EDE6)
}
