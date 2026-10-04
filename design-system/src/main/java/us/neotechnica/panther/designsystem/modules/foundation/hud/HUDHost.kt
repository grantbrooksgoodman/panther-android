//
//  HUDHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Renders the heads-up display requested through [HUDPresenter].
 *
 * Place a single [HUDHost] near the root of the composition, above the
 * app's content, so the display appears centered over the current
 * screen. A success or flash display dismisses itself after a short
 * interval scaled to its text; a progress display remains until
 * dismissed and, when modal, blocks interaction with the underlying
 * content.
 */
@Composable
fun HUDHost() {
    val presentation by HUDPresenter.presentation.collectAsState()

    LaunchedEffect(presentation) {
        val current = presentation
        val text =
            when (current) {
                is HUDPresenter.Presentation.Success -> current.text
                is HUDPresenter.Presentation.Flash -> current.text
                else -> return@LaunchedEffect
            }

        delay(AUTO_DISMISS_PER_CHARACTER_MILLIS * (text?.length ?: 0) + AUTO_DISMISS_BASE_MILLIS)
        if (HUDPresenter.presentation.value === current) HUDPresenter.hide()
    }

    // Retain the last display so the exit animation renders the
    // outgoing content rather than empty space.
    var lastPresentation by remember { mutableStateOf<HUDPresenter.Presentation?>(null) }
    presentation?.let { lastPresentation = it }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if ((presentation as? HUDPresenter.Presentation.Progress)?.isModal == true) {
            ModalBlocker()
        }

        AnimatedVisibility(
            enter = fadeIn(tween(ANIMATION_DURATION_MILLIS)) + scaleIn(tween(ANIMATION_DURATION_MILLIS), ENTER_INITIAL_SCALE),
            exit = fadeOut(tween(ANIMATION_DURATION_MILLIS)) + scaleOut(tween(ANIMATION_DURATION_MILLIS), EXIT_TARGET_SCALE),
            visible = presentation != null,
        ) {
            lastPresentation?.let { HUDCard(it) }
        }
    }
}

// MARK: - Modal Blocker

@Composable
private fun ModalBlocker() {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = interactionSource,
                ) {},
    )
}

// MARK: - Card

@Composable
private fun HUDCard(presentation: HUDPresenter.Presentation) {
    Surface(
        color = (if (isSystemInDarkTheme()) DARK_BACKGROUND else LIGHT_BACKGROUND).copy(alpha = BACKGROUND_ALPHA),
        shadowElevation = SHADOW_ELEVATION.dp,
        shape = RoundedCornerShape(CARD_CORNER_RADIUS.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.sizeIn(minWidth = CARD_SIZE.dp, minHeight = CARD_SIZE.dp).padding(CARD_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(CONTENT_SPACING.dp, Alignment.CenterVertically),
        ) {
            when (presentation) {
                is HUDPresenter.Presentation.Progress -> CircularProgressIndicator(modifier = Modifier.size(ICON_SIZE.dp))
                is HUDPresenter.Presentation.Success -> StatusIcon(Icons.Filled.CheckCircle, SUCCESS_COLOR)
                is HUDPresenter.Presentation.Flash ->
                    when (presentation.image) {
                        HUD.HUDImage.SUCCESS -> StatusIcon(Icons.Filled.CheckCircle, SUCCESS_COLOR)
                        HUD.HUDImage.EXCLAMATION -> StatusIcon(Icons.Filled.Warning, Color.Unspecified)
                    }
            }

            presentation.text?.let { text ->
                Text(
                    fontSize = STATUS_FONT_SIZE.sp,
                    fontWeight = FontWeight.Bold,
                    text = text,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(
    imageVector: ImageVector,
    tint: Color,
) {
    Icon(
        contentDescription = null,
        imageVector = imageVector,
        modifier = Modifier.size(ICON_SIZE.dp),
        tint = tint,
    )
}

private const val ANIMATION_DURATION_MILLIS = 150
private const val AUTO_DISMISS_BASE_MILLIS = 1250L
private const val AUTO_DISMISS_PER_CHARACTER_MILLIS = 30L
private const val BACKGROUND_ALPHA = 0.99f
private const val CARD_CORNER_RADIUS = 10
private const val CARD_PADDING = 16
private const val CARD_SIZE = 130
private const val CONTENT_SPACING = 12
private const val ENTER_INITIAL_SCALE = 1.4f
private const val EXIT_TARGET_SCALE = 0.3f
private const val ICON_SIZE = 48
private const val SHADOW_ELEVATION = 8
private const val STATUS_FONT_SIZE = 24

private val DARK_BACKGROUND = Color(0xFF3A3A3C)
private val LIGHT_BACKGROUND = Color(0xFFE5E5EA)
private val SUCCESS_COLOR = Color(0xFF34C759)
