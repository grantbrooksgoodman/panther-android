//
//  DialogWindowEffect.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

// Hands dimming and system bar styling to the content of the dialog that
// calls it: the window's own dim gives way to the content's animated
// dimming, and the system bars keep the appearance of the content
// behind the dialog.
@Composable
internal fun DialogWindowEffect(isDarkModeActive: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.setDimAmount(0f)
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightNavigationBars = !isDarkModeActive
            isAppearanceLightStatusBars = !isDarkModeActive
        }
    }
}
