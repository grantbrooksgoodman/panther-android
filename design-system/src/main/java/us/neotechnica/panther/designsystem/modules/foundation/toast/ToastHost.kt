//
//  ToastHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.toast

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.componentkit.models.SFSymbol
import us.neotechnica.panther.designsystem.modules.foundation.constants.ToastViewColors
import us.neotechnica.panther.designsystem.modules.foundation.constants.ToastViewFloats
import us.neotechnica.panther.designsystem.modules.foundation.constants.ToastViewStrings
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle

// MARK: - Constants Accessors

private typealias Colors = ToastViewColors
private typealias Floats = ToastViewFloats
private typealias Strings = ToastViewStrings

/**
 * Renders the toast currently requested through [ToastPresenter].
 *
 * Place a single [ToastHost] near the root of the composition,
 * above the app's content, so that toasts presented from anywhere
 * appear over the current screen. Toasts appear from their
 * appearance edge and, unless persistent, dismiss themselves after
 * their duration. A top-edge banner can also be dismissed with an
 * upward swipe.
 */
@Composable
fun ToastHost() {
    val presented by ToastPresenter.current.collectAsState()
    val view = LocalView.current

    val ephemeralDuration =
        (presented?.toast?.perpetuation as? Toast.PerpetuationStrategy.Ephemeral)?.duration
    LaunchedEffect(presented) {
        if (presented != null) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        val duration = ephemeralDuration ?: return@LaunchedEffect
        delay(duration)
        ToastPresenter.hide()
    }

    val appearanceEdge =
        (presented?.toast?.type as? Toast.ToastType.Banner)?.appearanceEdge
            ?: Toast.AppearanceEdge.TOP
    Box(
        contentAlignment =
            if (appearanceEdge == Toast.AppearanceEdge.BOTTOM) {
                Alignment.BottomCenter
            } else {
                Alignment.TopCenter
            },
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        val fromBottom = appearanceEdge == Toast.AppearanceEdge.BOTTOM
        AnimatedVisibility(
            enter = slideInVertically { if (fromBottom) it else -it } + fadeIn(),
            exit = slideOutVertically { if (fromBottom) it else -it } + fadeOut(),
            visible = presented != null,
        ) {
            presented?.let { current ->
                val onDismiss = { ToastPresenter.hide() }
                val onTap =
                    current.onTap?.let { tap ->
                        {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            tap()
                            ToastPresenter.hide()
                        }
                    }

                when (val type = current.toast.type) {
                    is Toast.ToastType.Banner ->
                        BannerToast(current.toast, type, onTap, onDismiss)

                    is Toast.ToastType.Capsule ->
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
    type: Toast.ToastType.Banner,
    onTap: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val palette = type.colorPalette
    val accentColor = palette?.accent ?: type.style.accentColor
    val textColor = palette?.text ?: colors.titleText

    Surface(
        color = palette?.background ?: colors.navigationBarBackground,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Floats.BANNER_HORIZONTAL_PADDING.dp,
                    vertical = Floats.TOP_APPEARANCE_EDGE_Y_OFFSET.dp,
                ).then(
                    if (type.appearanceEdge == Toast.AppearanceEdge.TOP) {
                        Modifier.pointerInput(toast) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -SWIPE_DISMISS_THRESHOLD) onDismiss()
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        shadowElevation = Floats.BANNER_SHADOW_RADIUS.dp,
        shape = RoundedCornerShape(Floats.BANNER_CORNER_RADIUS.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            accentColor?.let { color ->
                Box(
                    modifier =
                        Modifier
                            .width(Floats.BANNER_OVERLAY_FRAME_WIDTH.dp)
                            .fillMaxHeight()
                            .background(color),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Floats.BANNER_SPACER_MIN_LENGTH.dp),
                modifier =
                    Modifier
                        .weight(1f)
                        .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
                        .padding(Floats.BANNER_HORIZONTAL_PADDING.dp),
                verticalAlignment = if (toast.title == null) Alignment.CenterVertically else Alignment.Top,
            ) {
                accentColor?.let { color ->
                    type.style.bannerIcon?.let { icon ->
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
                            color = textColor.copy(alpha = Floats.BANNER_TITLE_LABEL_FOREGROUND_COLOR_OPACITY),
                            fontSize = Floats.BANNER_TITLE_LABEL_FONT_SIZE.sp,
                            fontWeight = FontWeight.SemiBold,
                            text = title.sanitized,
                        )
                    }

                    Text(
                        color = textColor.copy(alpha = Floats.BANNER_MESSAGE_LABEL_FOREGROUND_COLOR_OPACITY),
                        fontSize = Floats.BANNER_MESSAGE_LABEL_FONT_SIZE.sp,
                        fontWeight = if (toast.title == null) FontWeight.SemiBold else FontWeight.Normal,
                        text = toast.message.sanitized,
                    )
                }
            }

            if (type.showsDismissButton) {
                IconButton(
                    modifier =
                        Modifier.sizeIn(
                            minWidth = Floats.BANNER_DISMISS_BUTTON_MIN_SIZE.dp,
                            minHeight = Floats.BANNER_DISMISS_BUTTON_MIN_SIZE.dp,
                        ),
                    onClick = onDismiss,
                ) {
                    Icon(
                        contentDescription = "Dismiss",
                        imageVector = SFSymbol.imageVector(Strings.BANNER_DISMISS_BUTTON_IMAGE_SYSTEM_NAME),
                        tint =
                            (palette?.dismissButton ?: colors.titleText)
                                .copy(alpha = Floats.BANNER_DISMISS_BUTTON_FOREGROUND_COLOR_OPACITY),
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
    type: Toast.ToastType.Capsule,
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
                .border(
                    width = Floats.CAPSULE_OVERLAY_STROKE_LINE_WIDTH.dp,
                    color = Colors.CAPSULE_OVERLAY_STROKE.copy(alpha = Floats.CAPSULE_OVERLAY_STROKE_COLOR_OPACITY),
                    shape = CircleShape,
                ).clickable(onClick = onTap ?: onDismiss),
        shadowElevation = Floats.CAPSULE_SHADOW_RADIUS.dp,
        shape = CircleShape,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Floats.CAPSULE_MESSAGE_LABEL_HORIZONTAL_PADDING.dp),
            modifier =
                Modifier.padding(
                    horizontal = Floats.CAPSULE_PRIMARY_HORIZONTAL_PADDING.dp,
                    vertical = Floats.CAPSULE_VERTICAL_PADDING.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            type.style.accentColor?.let { color ->
                type.style.capsuleIcon?.let { icon ->
                    Icon(
                        contentDescription = null,
                        imageVector = icon,
                        modifier =
                            Modifier.size(
                                width = Floats.CAPSULE_IMAGE_FRAME_MAX_WIDTH.dp,
                                height = Floats.CAPSULE_IMAGE_FRAME_MAX_HEIGHT.dp,
                            ),
                        tint = color,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                toast.title?.let { title ->
                    Text(
                        color = colors.titleText,
                        fontSize = Floats.CAPSULE_TITLE_LABEL_FONT_SIZE.sp,
                        fontWeight = FontWeight.SemiBold,
                        text = title.sanitized,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    color =
                        if (toast.title == null) {
                            colors.titleText
                        } else {
                            Colors.CAPSULE_MESSAGE_LABEL_FOREGROUND
                        },
                    fontSize = Floats.CAPSULE_MESSAGE_LABEL_FONT_SIZE.sp,
                    fontWeight = if (toast.title == null) FontWeight.SemiBold else FontWeight.Normal,
                    text = toast.message.sanitized,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// MARK: - Style Mapping

private val ToastStyle.accentColor: Color?
    get() =
        when (this) {
            ToastStyle.ERROR -> Colors.DEFAULT_ERROR
            ToastStyle.INFO -> Colors.DEFAULT_INFO
            ToastStyle.SUCCESS -> Colors.DEFAULT_SUCCESS
            ToastStyle.WARNING -> Colors.DEFAULT_WARNING
            ToastStyle.NONE -> null
        }

private val ToastStyle.bannerIcon: ImageVector?
    get() =
        when (this) {
            ToastStyle.ERROR -> SFSymbol.imageVector(Strings.BANNER_ERROR_ICON_IMAGE_SYSTEM_NAME)
            ToastStyle.INFO -> SFSymbol.imageVector(Strings.BANNER_INFO_ICON_IMAGE_SYSTEM_NAME)
            ToastStyle.SUCCESS -> SFSymbol.imageVector(Strings.BANNER_SUCCESS_ICON_IMAGE_SYSTEM_NAME)
            ToastStyle.WARNING -> SFSymbol.imageVector(Strings.BANNER_WARNING_ICON_IMAGE_SYSTEM_NAME)
            ToastStyle.NONE -> null
        }

private val ToastStyle.capsuleIcon: ImageVector?
    get() =
        when (this) {
            ToastStyle.ERROR -> SFSymbol.imageVector(Strings.CAPSULE_ERROR_ICON_IMAGE_SYSTEM_NAME)
            ToastStyle.INFO -> SFSymbol.imageVector(Strings.CAPSULE_INFO_ICON_IMAGE_SYSTEM_NAME)
            ToastStyle.SUCCESS -> SFSymbol.imageVector(Strings.CAPSULE_SUCCESS_ICON_IMAGE_SYSTEM_NAME)
            ToastStyle.WARNING -> SFSymbol.imageVector(Strings.CAPSULE_WARNING_ICON_IMAGE_SYSTEM_NAME)
            ToastStyle.NONE -> null
        }

private const val CAPSULE_TOP_PADDING = 8
private const val SWIPE_DISMISS_THRESHOLD = 10
private const val TITLE_MESSAGE_SPACING = 2
