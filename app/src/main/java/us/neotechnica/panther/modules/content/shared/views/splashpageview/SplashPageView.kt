//
//  SplashPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.views.splashpageview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import us.neotechnica.panther.R
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.networkActivityOccurred
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.shared.components.GIFImageView
import us.neotechnica.panther.modules.content.shared.constants.SplashPageViewColors
import us.neotechnica.panther.modules.content.shared.constants.SplashPageViewFloats
import us.neotechnica.panther.modules.content.shared.constants.SplashPageViewStrings
import us.neotechnica.panther.modules.content.shared.services.SplashPageViewService
import us.neotechnica.panther.modules.content.shared.services.SplashPageViewService.LoadingIndicatorStyle
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvent
import java.util.Date

// MARK: - Constants Accessors

private typealias Colors = SplashPageViewColors
private typealias Floats = SplashPageViewFloats
private typealias Strings = SplashPageViewStrings

/**
 * The splash page.
 *
 * Presents the animated logotype while the bundle initializes,
 * displaying a determinate progress bar for heavy loads, an
 * indeterminate spinner for light loads, and no indicator for the
 * first second of a fresh load.
 *
 * @param viewModel The view model that drives the page.
 * @param modifier The modifier for this view.
 */
@Composable
fun SplashPageView(
    viewModel: ViewModel<SplashPageReducer.State, SplashPageReducer.Action>,
    modifier: Modifier = Modifier,
) {
    val observedViewModel =
        remember(viewModel) {
            viewModel.observing(
                SharedEvent { it.networkActivityOccurred }.wrappedValue.events,
            ) { SplashPageReducer.Action.BundleInitializationProgressOccurred }
        }

    val colors = LocalPantherColors.current
    val isDarkMode = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val loadingIndicatorStyle by SplashPageViewService.loadingIndicatorStyle.collectAsState()
    val initializationProgress by SplashPageViewService.initializationProgress.collectAsState()

    LaunchedEffect(Unit) {
        Application.loadStartDate = Date()
        observedViewModel.send(SplashPageReducer.Action.ViewAppeared)
    }

    DisposableEffect(observedViewModel) {
        onDispose { observedViewModel.close() }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = Floats.padding),
        ) {
            GIFImageView(
                name = Strings.GIF_IMAGE_NAME,
                isActive = loadingIndicatorStyle == LoadingIndicatorStyle.BAR,
                modifier =
                    Modifier
                        .width(Floats.imageFrameWidth)
                        .height(Floats.imageFrameHeight)
                        .alpha(if (loadingIndicatorStyle == LoadingIndicatorStyle.BAR) 1f else 0f),
            )

            Image(
                painter = painterResource(R.drawable.hello_wordmark),
                contentDescription = null,
                colorFilter = if (isDarkMode) ColorFilter.tint(Colors.imageDarkForeground) else null,
                contentScale = ContentScale.FillBounds,
                modifier =
                    Modifier
                        .width(Floats.imageFrameWidth)
                        .height(Floats.imageFrameHeight)
                        .alpha(if (loadingIndicatorStyle == LoadingIndicatorStyle.BAR) 0f else 1f),
            )
        }

        ProgressBar(
            initializationProgress = initializationProgress,
            loadingIndicatorStyle = loadingIndicatorStyle,
            tint = colors.titleText,
        )

        if (loadingIndicatorStyle == LoadingIndicatorStyle.SPINNER) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .scale(Floats.ACTIVITY_INDICATOR_SCALE_EFFECT)
                        .padding(top = Floats.padding),
            )
        }
    }
}

// MARK: - Auxiliary

@Composable
private fun ProgressBar(
    initializationProgress: Float,
    loadingIndicatorStyle: LoadingIndicatorStyle,
    tint: Color,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = initializationProgress,
        animationSpec = tween(easing = EaseIn),
        label = "SplashInitializationProgress",
    )

    val fadeIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(Floats.PROGRESS_BAR_FADE_IN_DELAY_MILLISECONDS)
        fadeIn.animateTo(1f)
    }

    LinearProgressIndicator(
        progress = { animatedProgress },
        color = tint,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.progressBarHorizontalPadding)
                .alpha(fadeIn.value * if (loadingIndicatorStyle == LoadingIndicatorStyle.BAR) 1f else 0f),
    )
}
