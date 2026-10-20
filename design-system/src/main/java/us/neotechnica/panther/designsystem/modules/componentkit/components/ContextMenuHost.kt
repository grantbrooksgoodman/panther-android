//
//  ContextMenuHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import us.neotechnica.panther.designsystem.modules.componentkit.constants.MessageContextMenuFloats
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation

/** Provides the [ContextMenuController] to descendant [MessageContextMenu]s. */
val LocalContextMenuController = compositionLocalOf<ContextMenuController?> { null }

/**
 * Hosts message context menus in the same composition as [content], so
 * the lifted bubble aligns exactly with its origin over a dimmed,
 * tap-to-dismiss backdrop.
 *
 * While a menu is on screen, the host ignores the system back gesture.
 * A menu that presents while the keyboard is showing hides the
 * keyboard, which shows again once the menu finishes animating away.
 *
 * @param modifier The modifier for the host.
 * @param canBegin Whether a long press may present a menu; when
 *   `false`, long presses are ignored but double-taps still fire.
 * @param content The hosted content, containing [MessageContextMenu]s.
 */
@Composable
fun ContextMenuHost(
    modifier: Modifier = Modifier,
    canBegin: Boolean = true,
    content: @Composable () -> Unit,
) {
    val controller = remember { ContextMenuController() }
    controller.canBegin = canBegin
    controller.hapticFeedback = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }

    // The menu appears and disappears on a spring whose initial velocity
    // is relative to the distance left to travel, so a dismissal starts
    // out heading back toward rest. While system-style animations are
    // disabled, the menu moves on Compose's default spring from rest.
    LaunchedEffect(controller.active) {
        val usesAppleAnimations = SystemAnimation.usesAppleAnimations
        val stiffness = if (usesAppleAnimations) PRESENTATION_SPRING_STIFFNESS else Spring.StiffnessMediumLow
        val initialVelocity = if (usesAppleAnimations) PRESENTATION_SPRING_INITIAL_VELOCITY else 0f
        if (controller.active != null) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness),
                initialVelocity = initialVelocity,
            )
            return@LaunchedEffect
        }

        if (controller.lastPresented == null) return@LaunchedEffect
        progress.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness),
            initialVelocity = -initialVelocity * progress.value,
        )
        controller.lastPresented = null
    }

    // While a menu is on screen, the system back gesture is ignored.
    BackHandler(enabled = controller.isDisplayed) {}
    KeyboardDisplacementEffect(controller.isDisplayed)

    // Modifier.blur is a no-op below API 31, so the pre-31 path captures
    // the host into a graphics layer and draws a software-blurred copy
    // behind the overlay instead.
    val usesSoftwareBlur = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    val hostLayer = rememberGraphicsLayer()
    var softwareBlurredBackdrop by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(controller.lastPresented) {
        softwareBlurredBackdrop =
            if (usesSoftwareBlur && controller.lastPresented != null) {
                softwareBlurred(hostLayer.toImageBitmap())
            } else {
                null
            }
    }

    Box(modifier.fillMaxSize().onPlaced { controller.hostCoordinates = it }) {
        Box(
            if (usesSoftwareBlur) {
                Modifier.drawWithContent {
                    hostLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(hostLayer)
                }
            } else {
                Modifier.blur(MessageContextMenuFloats.BLUR_RADIUS * progress.value)
            },
        ) {
            CompositionLocalProvider(LocalContextMenuController provides controller) {
                content()
            }
        }

        controller.displayed?.let { active ->
            softwareBlurredBackdrop?.let { backdrop ->
                Image(
                    bitmap = backdrop,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            ContextMenuOverlay(active = active, progress = progress.value, controller = controller)
        }
    }
}

// Hides the keyboard as a menu presents, and shows it again once the
// menu finishes animating away if the menu hid it. The focused text
// field keeps its focus throughout, so the keyboard returns to it.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyboardDisplacementEffect(isMenuDisplayed: Boolean) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible by rememberUpdatedState(WindowInsets.isImeVisible)
    var didHideKeyboard by remember { mutableStateOf(false) }

    LaunchedEffect(isMenuDisplayed) {
        if (isMenuDisplayed) {
            didHideKeyboard = isImeVisible
            if (didHideKeyboard) keyboardController?.hide()
            return@LaunchedEffect
        }

        if (didHideKeyboard) keyboardController?.show()
        didHideKeyboard = false
    }
}

// Approximates a blur by downscaling the capture and re-expanding it
// with bilinear filtering.
private fun softwareBlurred(image: ImageBitmap): ImageBitmap {
    val bitmap = image.asAndroidBitmap()
    val scaledWidth = (bitmap.width / SOFTWARE_BLUR_DOWNSCALE_FACTOR).coerceAtLeast(1)
    val scaledHeight = (bitmap.height / SOFTWARE_BLUR_DOWNSCALE_FACTOR).coerceAtLeast(1)
    val downscaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    return Bitmap.createScaledBitmap(downscaled, bitmap.width, bitmap.height, true).asImageBitmap()
}

private const val PRESENTATION_SPRING_INITIAL_VELOCITY = 4f

// The stiffness of the menu's spring: a 0.3-second critically damped
// spring.
private const val PRESENTATION_SPRING_STIFFNESS = 918.701f
private const val SOFTWARE_BLUR_DOWNSCALE_FACTOR = 16
