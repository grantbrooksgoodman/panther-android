//
//  OverlayHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

/**
 * Renders the global activity [Overlay] over the current screen.
 *
 * Place a single [OverlayHost] near the root of the composition, above
 * the app's content. While a modal overlay is visible, it dims the
 * screen and consumes input so the underlying UI cannot be interacted
 * with. Removal fades out over 0.2 seconds unless the overlay was
 * removed without animation.
 */
@Composable
fun OverlayHost() {
    val isVisible by Overlay.isVisible.collectAsState()
    val animatesRemoval by Overlay.animatesRemoval.collectAsState()

    AnimatedVisibility(
        enter = fadeIn(tween(FADE_DURATION_MILLIS)),
        exit = fadeOut(tween(if (animatesRemoval) FADE_DURATION_MILLIS else 0)),
        visible = isVisible,
    ) {
        val activityIndicator by Overlay.activityIndicator.collectAsState()
        val alpha by Overlay.alpha.collectAsState()
        val backgroundColor by Overlay.backgroundColor.collectAsState()
        val isModal by Overlay.isModal.collectAsState()

        val interactionSource = remember { MutableInteractionSource() }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(backgroundColor.copy(alpha = alpha))
                    .then(
                        if (isModal) {
                            Modifier.clickable(
                                indication = null,
                                interactionSource = interactionSource,
                            ) {}
                        } else {
                            Modifier
                        },
                    ),
        ) {
            activityIndicator?.let { CircularProgressIndicator(color = it.color) }
        }
    }
}

private const val FADE_DURATION_MILLIS = 200
