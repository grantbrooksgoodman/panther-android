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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

/**
 * Renders the heads-up display requested through [HUDPresenter].
 *
 * Place a single [HUDHost] near the root of the composition, above the
 * app's content, so the display appears centered over the current
 * screen. A success display dismisses itself after a short interval; a
 * progress display remains until dismissed and, when modal, blocks
 * interaction with the underlying content.
 */
@Composable
fun HUDHost() {
    val presentation by HUDPresenter.presentation.collectAsState()

    LaunchedEffect(presentation) {
        val success = presentation as? HUDPresenter.Presentation.Success ?: return@LaunchedEffect
        delay(SUCCESS_DISPLAY_DURATION_MILLIS)
        if (HUDPresenter.presentation.value == success) HUDPresenter.hide()
    }

    // Retain the last display so the exit animation renders the
    // outgoing content rather than empty space.
    var lastPresentation by remember { mutableStateOf<HUDPresenter.Presentation?>(null) }
    presentation?.let { lastPresentation = it }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if ((presentation as? HUDPresenter.Presentation.Progress)?.isModal == true) {
            ModalScrim()
        }

        AnimatedVisibility(
            enter = fadeIn() + scaleIn(initialScale = ENTER_INITIAL_SCALE),
            exit = fadeOut() + scaleOut(targetScale = ENTER_INITIAL_SCALE),
            visible = presentation != null,
        ) {
            when (lastPresentation) {
                is HUDPresenter.Presentation.Progress -> ProgressCard()
                is HUDPresenter.Presentation.Success -> SuccessCard()
                null -> Unit
            }
        }
    }
}

// MARK: - Modal Scrim

@Composable
private fun ModalScrim() {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(
                    indication = null,
                    interactionSource = interactionSource,
                ) {},
    )
}

// MARK: - Progress Card

@Composable
private fun ProgressCard() {
    val colors = LocalPantherColors.current
    Surface(
        color = colors.navigationBarBackground,
        shadowElevation = SHADOW_ELEVATION.dp,
        shape = RoundedCornerShape(CARD_CORNER_RADIUS.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(CARD_SIZE.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(ICON_SIZE.dp))
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
private const val SCRIM_ALPHA = 0.5f
private val SUCCESS_COLOR = Color(0xFF34C759)
