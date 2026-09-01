//
//  ToastHost.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle

/**
 * Renders the toast currently requested through [ToastPresenter].
 *
 * Place a single [ToastHost] near the root of the composition,
 * above the app's content, so that toasts presented from anywhere
 * appear over the current screen. Toasts appear from the top edge
 * and, unless persistent, dismiss themselves after their duration.
 */
@Composable
fun ToastHost() {
    val presented by ToastPresenter.current.collectAsState()

    val ephemeralDuration =
        (presented?.toast?.perpetuation as? Toast.Perpetuation.Ephemeral)?.duration
    LaunchedEffect(presented) {
        val duration = ephemeralDuration ?: return@LaunchedEffect
        delay(duration)
        ToastPresenter.hide()
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        AnimatedVisibility(
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            visible = presented != null,
        ) {
            presented?.let { current ->
                val onDismiss = { ToastPresenter.hide() }
                val onTap =
                    current.onTap?.let { tap ->
                        {
                            tap()
                            ToastPresenter.hide()
                        }
                    }

                when (val type = current.toast.type) {
                    is Toast.Type.Banner ->
                        BannerToast(current.toast, type, onTap, onDismiss)

                    is Toast.Type.Capsule ->
                        CapsuleToast(current.toast, type, onTap, onDismiss)
                }
            }
        }
    }
}

// MARK: - Banner

@Composable
private fun BannerToast(
    toast: Toast,
    type: Toast.Type.Banner,
    onTap: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val accentColor = type.style.accentColor

    Surface(
        color = colors.navigationBarBackground,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BANNER_HORIZONTAL_PADDING.dp),
        shadowElevation = SHADOW_ELEVATION.dp,
        shape = RoundedCornerShape(BANNER_CORNER_RADIUS.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            accentColor?.let { color ->
                Box(
                    modifier =
                        Modifier
                            .width(ACCENT_STRIP_WIDTH.dp)
                            .fillMaxHeight()
                            .background(color),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(CONTENT_SPACING.dp),
                modifier =
                    Modifier
                        .weight(1f)
                        .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
                        .padding(CONTENT_PADDING.dp),
                verticalAlignment = if (toast.title == null) Alignment.CenterVertically else Alignment.Top,
            ) {
                accentColor?.let { color ->
                    type.style.icon?.let { icon ->
                        Icon(
                            contentDescription = null,
                            imageVector = icon,
                            tint = color,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(TITLE_MESSAGE_SPACING.dp)) {
                    toast.title?.let { title ->
                        Text(
                            color = colors.titleText,
                            fontWeight = FontWeight.SemiBold,
                            text = title,
                        )
                    }

                    Text(
                        color = colors.titleText.copy(alpha = MESSAGE_ALPHA),
                        fontWeight = if (toast.title == null) FontWeight.SemiBold else FontWeight.Normal,
                        text = toast.message,
                    )
                }
            }

            if (type.showsDismissButton) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        contentDescription = "Dismiss",
                        imageVector = Icons.Filled.Close,
                        tint = colors.titleText.copy(alpha = DISMISS_BUTTON_ALPHA),
                    )
                }
            }
        }
    }
}

// MARK: - Capsule

@Composable
private fun CapsuleToast(
    toast: Toast,
    type: Toast.Type.Capsule,
    onTap: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = LocalPantherColors.current

    Surface(
        color = colors.navigationBarBackground,
        modifier =
            Modifier
                .padding(top = CAPSULE_TOP_PADDING.dp)
                .clip(CircleShape)
                .clickable(onClick = onTap ?: onDismiss),
        shadowElevation = SHADOW_ELEVATION.dp,
        shape = CircleShape,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(CONTENT_SPACING.dp),
            modifier =
                Modifier.padding(
                    horizontal = CAPSULE_HORIZONTAL_PADDING.dp,
                    vertical = CAPSULE_VERTICAL_PADDING.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            type.style.accentColor?.let { color ->
                type.style.icon?.let { icon ->
                    Icon(
                        contentDescription = null,
                        imageVector = icon,
                        modifier = Modifier.size(CAPSULE_ICON_SIZE.dp),
                        tint = color,
                    )
                }
            }

            Text(
                color = colors.titleText,
                fontWeight = FontWeight.SemiBold,
                text = toast.title ?: toast.message,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// MARK: - Style Mapping

private val ToastStyle.accentColor: Color?
    get() =
        when (this) {
            ToastStyle.ERROR -> Color(0xFFFF3B30)
            ToastStyle.INFO -> Color(0xFF007AFF)
            ToastStyle.SUCCESS -> Color(0xFF34C759)
            ToastStyle.WARNING -> Color(0xFFFF9500)
            ToastStyle.NONE -> null
        }

private val ToastStyle.icon: ImageVector?
    get() =
        when (this) {
            ToastStyle.ERROR -> Icons.Filled.Error
            ToastStyle.INFO -> Icons.Filled.Info
            ToastStyle.SUCCESS -> Icons.Filled.CheckCircle
            ToastStyle.WARNING -> Icons.Filled.Warning
            ToastStyle.NONE -> null
        }

private const val ACCENT_STRIP_WIDTH = 4
private const val BANNER_CORNER_RADIUS = 16
private const val BANNER_HORIZONTAL_PADDING = 12
private const val CAPSULE_HORIZONTAL_PADDING = 20
private const val CAPSULE_ICON_SIZE = 18
private const val CAPSULE_TOP_PADDING = 8
private const val CAPSULE_VERTICAL_PADDING = 12
private const val CONTENT_PADDING = 16
private const val CONTENT_SPACING = 12
private const val DISMISS_BUTTON_ALPHA = 0.5f
private const val MESSAGE_ALPHA = 0.8f
private const val SHADOW_ELEVATION = 8
private const val TITLE_MESSAGE_SPACING = 2
