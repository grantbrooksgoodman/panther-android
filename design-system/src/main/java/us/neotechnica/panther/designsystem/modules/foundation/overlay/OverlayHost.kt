//
//  OverlayHost.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Renders the global activity [Overlay] over the current screen.
 *
 * Place a single [OverlayHost] near the root of the composition, above
 * the app's content. While visible, it dims the screen and consumes
 * input so the underlying UI cannot be interacted with.
 */
@Composable
fun OverlayHost() {
    val isVisible by Overlay.isVisible.collectAsState()
    if (!isVisible) return

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(
                    indication = null,
                    interactionSource = interactionSource,
                ) {},
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

private const val SCRIM_ALPHA = 0.5f
