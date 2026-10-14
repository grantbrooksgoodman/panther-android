//
//  AppConstants+PermissionPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

/** The layout values of the permission page view. */
object PermissionsViewFloats {
    /** The top padding of the back button. */
    val backButtonTopPadding: Dp = 2.dp

    /** The bottom padding of the button stack. */
    val buttonVStackBottomPadding: Dp = 50.dp

    /** The spacing of the button stack. */
    val buttonVStackSpacing: Dp = 8.dp

    /** The top padding of the finish button. */
    val finishButtonTopPadding: Dp = 5.dp

    /** The bottom padding of the inner stack. */
    val innerVStackBottomPadding: Dp = 50.dp

    /** The font size of the back button label. */
    const val BACK_BUTTON_LABEL_FONT_SIZE = 15f
}
