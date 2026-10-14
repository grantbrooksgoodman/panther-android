//
//  AppConstants+GenericTextField.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

/** The layout and timing values of the generic text field. */
object GenericTextFieldFloats {
    /** The horizontal offset of the default divider. */
    val defaultDividerXOffset: Dp = 0.dp

    /** The vertical offset of the default divider. */
    val defaultDividerYOffset: Dp = 18.dp

    /** The minimum height of the field. */
    val fieldMinHeight: Dp = 40.dp

    /** The opacity of the divider. */
    const val DIVIDER_ALPHA = 0.3f
}
