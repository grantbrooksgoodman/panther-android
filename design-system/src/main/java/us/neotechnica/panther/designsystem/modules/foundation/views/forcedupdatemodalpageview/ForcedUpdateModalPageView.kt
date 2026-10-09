//
//  ForcedUpdateModalPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.forcedupdatemodalpageview

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.constants.ForcedUpdateModalPageViewColors
import us.neotechnica.panther.designsystem.modules.foundation.constants.ForcedUpdateModalPageViewConstants
import us.neotechnica.panther.designsystem.modules.foundation.constants.ForcedUpdateModalPageViewFloats
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.designsystem.modules.theming.views.ThemedView
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Type Aliases

private typealias ForcedUpdateModalPageViewModel =
    ViewModel<ForcedUpdateModalPageReducer.State, ForcedUpdateModalPageReducer.Action>

// MARK: - Constants Accessors

private typealias Colors = ForcedUpdateModalPageViewColors
private typealias Floats = ForcedUpdateModalPageViewFloats
private typealias Strings = ForcedUpdateModalPageViewConstants

// MARK: - Body

@Composable
internal fun ForcedUpdateModalPageView(viewModel: ForcedUpdateModalPageViewModel) {
    val state by viewModel.state.collectAsState()
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(viewModel) {
        viewModel.send(ForcedUpdateModalPageReducer.Action.ViewAppeared)
    }

    PrefersStatusBarHidden()

    StatefulView(state.viewState) {
        LaunchedEffect(Unit) {
            contentAlpha.animateTo(
                1f,
                tween(
                    durationMillis = (Floats.TRANSITION_ANIMATION_DURATION * MILLISECONDS_PER_SECOND).toInt(),
                    easing = EaseIn,
                ),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(LocalPantherColors.current.background)
                    .safeDrawingPadding()
                    .alpha(contentAlpha.value),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            AppIconImage()
            ThemedView { CallToActionContentView(state, viewModel) }
        }
    }
}

// MARK: - Auxiliary

@Composable
private fun AppIconImage() {
    val context = LocalContext.current
    val appIconImage: ImageBitmap? =
        remember {
            runCatching {
                context
                    .packageManager
                    .getApplicationIcon(context.packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull()
        }

    appIconImage ?: return
    Box(
        modifier =
            Modifier
                .padding(bottom = Floats.APP_ICON_IMAGE_BOTTOM_PADDING.dp)
                .size(
                    width = Floats.APP_ICON_IMAGE_MAX_WIDTH.dp,
                    height = Floats.APP_ICON_IMAGE_MAX_HEIGHT.dp,
                ),
    ) {
        Image(
            bitmap = appIconImage,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Floats.APP_ICON_IMAGE_CORNER_RADIUS.dp)),
        )

        AppIconImageOverlay(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun AppIconImageOverlay(modifier: Modifier = Modifier) {
    // The symbol's mark renders in the primary color over a filled
    // shape in the secondary color.
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .offset(
                    x = Floats.APP_ICON_IMAGE_OVERLAY_SYMBOL_X_OFFSET.dp,
                    y = Floats.APP_ICON_IMAGE_OVERLAY_SYMBOL_Y_OFFSET.dp,
                ).size(
                    width = Floats.APP_ICON_IMAGE_OVERLAY_SYMBOL_FRAME_MAX_WIDTH.dp,
                    height = Floats.APP_ICON_IMAGE_OVERLAY_SYMBOL_FRAME_MAX_HEIGHT.dp,
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .offset(y = (Floats.APP_ICON_IMAGE_OVERLAY_SYMBOL_FRAME_MAX_HEIGHT * SYMBOL_MARK_Y_OFFSET_RATIO).dp)
                    .size(
                        width = (Floats.APP_ICON_IMAGE_OVERLAY_SYMBOL_FRAME_MAX_WIDTH * SYMBOL_MARK_WIDTH_RATIO).dp,
                        height = (Floats.APP_ICON_IMAGE_OVERLAY_SYMBOL_FRAME_MAX_HEIGHT * SYMBOL_MARK_HEIGHT_RATIO).dp,
                    ).background(Colors.appIconImageOverlayForeground),
        )

        Components.Symbol(
            Strings.APP_ICON_IMAGE_OVERLAY_SYMBOL_NAME,
            foregroundColor = Colors.appIconImageOverlaySecondaryForeground,
            modifier = Modifier.fillMaxSize(),
            usesIntrinsicSize = false,
        )
    }
}

@Composable
private fun ColumnScope.CallToActionContentView(
    state: ForcedUpdateModalPageReducer.State,
    viewModel: ForcedUpdateModalPageViewModel,
) {
    val colors = LocalPantherColors.current
    Components.Text(
        state.strings.value(ForcedUpdateModalPageViewStringKey.TITLE_LABEL_TEXT),
        foregroundColor = colors.titleText,
        font = Font.systemBold(FontScale.Large),
        modifier =
            Modifier.padding(
                bottom = Floats.TITLE_LABEL_TEXT_BOTTOM_PADDING.dp,
                start = Floats.TITLE_LABEL_TEXT_HORIZONTAL_PADDING.dp,
                end = Floats.TITLE_LABEL_TEXT_HORIZONTAL_PADDING.dp,
            ),
    )

    Components.Text(
        state.strings.value(ForcedUpdateModalPageViewStringKey.SUBTITLE_LABEL_TEXT),
        foregroundColor = colors.titleText,
        font = Font.system(FontScale.Custom(Floats.SUBTITLE_LABEL_TEXT_SYSTEM_FONT_SCALE)),
        modifier =
            Modifier.padding(
                bottom = Floats.SUBTITLE_LABEL_TEXT_BOTTOM_PADDING.dp,
                start = Floats.SUBTITLE_LABEL_TEXT_HORIZONTAL_PADDING.dp,
                end = Floats.SUBTITLE_LABEL_TEXT_HORIZONTAL_PADDING.dp,
            ),
        textAlign = TextAlign.Center,
    )

    if (state.shouldShowInstallButton) {
        Components.CapsuleButton(
            state.strings.value(ForcedUpdateModalPageViewStringKey.INSTALL_BUTTON_TEXT),
            font = Font.systemSemibold(),
            foregroundColor = colors.background,
        ) {
            viewModel.send(ForcedUpdateModalPageReducer.Action.InstallButtonTapped)
        }
    }

    Spacer(modifier = Modifier.weight(1f))

    Components.Text(
        state.versionLabelText,
        foregroundColor = Colors.versionLabelTextForeground,
        font = Font.system(FontScale.Small),
    )
}

@Composable
private fun PrefersStatusBarHidden() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.hide(WindowInsetsCompat.Type.statusBars())
        onDispose { insetsController?.show(WindowInsetsCompat.Type.statusBars()) }
    }
}

private val Context.activity: Activity?
    get() =
        when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.activity
            else -> null
        }

private fun Map<ForcedUpdateModalPageViewStringKey, String>.value(key: ForcedUpdateModalPageViewStringKey): String =
    (this[key] ?: key.rawValue).sanitized

private const val MILLISECONDS_PER_SECOND = 1_000f
private const val SYMBOL_MARK_HEIGHT_RATIO = 0.4f
private const val SYMBOL_MARK_WIDTH_RATIO = 0.1f
private const val SYMBOL_MARK_Y_OFFSET_RATIO = 0.08f
