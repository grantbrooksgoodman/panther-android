//
//  FadeInModifier.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.modifiers

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Fades this element in from transparent when it first appears.
 *
 * @param duration The duration of the fade. The default is 500
 *   milliseconds.
 * @param delay The duration to wait before the fade begins. The
 *   default is zero.
 *
 * @return A modifier that fades the element in on appearance.
 */
fun Modifier.fadeIn(
    duration: Duration = 500.milliseconds,
    delay: Duration = Duration.ZERO,
): Modifier =
    composed {
        var isVisible by remember { mutableStateOf(false) }
        val opacity by animateFloatAsState(
            animationSpec = tween(duration.inWholeMilliseconds.toInt(), easing = EaseIn),
            label = "fadeIn",
            targetValue = if (isVisible) 1f else 0f,
        )

        LaunchedEffect(Unit) {
            delay(delay)
            isVisible = true
        }

        graphicsLayer { alpha = opacity }
    }
