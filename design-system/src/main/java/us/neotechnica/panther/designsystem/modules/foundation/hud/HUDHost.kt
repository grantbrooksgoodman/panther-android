//
//  HUDHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

/**
 * Renders the success heads-up display requested through
 * [HUDPresenter].
 *
 * Place a single [HUDHost] near the root of the composition, above the
 * app's content, so the display appears centered over the current
 * screen and dismisses itself after a short interval.
 */
@Composable
fun HUDHost() {
    val token by HUDPresenter.token.collectAsState()

    LaunchedEffect(token) {
        if (token == null) return@LaunchedEffect
        delay(SUCCESS_DISPLAY_DURATION_MILLIS)
        HUDPresenter.hide()
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            enter = fadeIn() + scaleIn(initialScale = ENTER_INITIAL_SCALE),
            exit = fadeOut() + scaleOut(targetScale = ENTER_INITIAL_SCALE),
            visible = token != null,
        ) {
            SuccessCard()
        }
    }
}

// MARK: - Success Card

@Composable
private fun SuccessCard() {
    val colors = LocalPantherColors.current
    Surface(
        color = colors.navigationBarBackground,
        shadowElevation = SHADOW_ELEVATION.dp,
        shape = RoundedCornerShape(CARD_CORNER_RADIUS.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(CARD_SIZE.dp)) {
            Icon(
                contentDescription = null,
                imageVector = Icons.Filled.CheckCircle,
                modifier = Modifier.size(ICON_SIZE.dp),
                tint = SUCCESS_COLOR,
            )
        }
    }
}

private const val SUCCESS_DISPLAY_DURATION_MILLIS = 2000L
private const val ENTER_INITIAL_SCALE = 0.8f
private const val CARD_CORNER_RADIUS = 16
private const val CARD_SIZE = 100
private const val ICON_SIZE = 48
private const val SHADOW_ELEVATION = 8
private val SUCCESS_COLOR = Color(0xFF34C759)
