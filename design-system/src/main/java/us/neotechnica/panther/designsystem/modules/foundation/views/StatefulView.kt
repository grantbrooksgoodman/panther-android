//
//  StatefulView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.foundation.models.ConnectionAlert
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey

/**
 * A container that switches between loading, loaded, and error states
 * based on a [ViewState].
 *
 * When the state is [ViewState.Loading], a full-screen progress
 * indicator is displayed. When it is [ViewState.Loaded], `content` is
 * rendered. On [ViewState.Error], a failure page is shown with an
 * optional retry action. State transitions animate with an opacity
 * crossfade.
 *
 * @param state The current view state.
 * @param modifier The modifier for this container.
 * @param exceptionRetryHandler An optional action performed when the
 *   user taps Try Again on the failure page. Pass `null` to hide the
 *   Try Again button.
 * @param content The content shown when the state is loaded.
 */
@Composable
fun StatefulView(
    state: ViewState,
    modifier: Modifier = Modifier,
    exceptionRetryHandler: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AnimatedContent(
        contentKey = { it::class },
        label = "StatefulView",
        modifier = modifier.fillMaxSize(),
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { target ->
        when (target) {
            is ViewState.Error -> FailurePage(target.exception, exceptionRetryHandler)
            ViewState.Loaded -> content()
            ViewState.Loading -> ProgressPage()
        }
    }
}

// MARK: - Progress Page

@Composable
private fun ProgressPage() {
    val colors = LocalPantherColors.current
    // The spinner fades in over 200 ms after a 500 ms delay.
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(PROGRESS_SPINNER_DELAY_MILLIS)
        isVisible = true
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background),
    ) {
        AnimatedVisibility(
            enter = fadeIn(tween(PROGRESS_SPINNER_FADE_MILLIS)),
            exit = fadeOut(),
            visible = isVisible,
        ) {
            CircularProgressIndicator(color = colors.titleText)
        }
    }
}

// MARK: - Failure Page

@Composable
private fun FailurePage(
    exception: Exception,
    exceptionRetryHandler: (() -> Unit)?,
) {
    val colors = LocalPantherColors.current
    val showReportButton = exception.isReportable && !Logger.reportsErrorsAutomatically
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Components.Symbol(
            systemName = "exclamationmark.octagon.fill",
            foregroundColor = FAILURE_GLYPH_COLOR,
            modifier = Modifier.size(FAILURE_GLYPH_SIZE.dp),
        )

        Text(
            exception.userFacingDescriptor,
            color = colors.titleText,
            fontSize = FAILURE_DESCRIPTOR_SIZE.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        exceptionRetryHandler?.let {
            TextButton(onClick = it) {
                Text(Localized(SubsystemStringKey.TRY_AGAIN).wrappedValue, color = colors.accent)
            }
        }

        if (showReportButton) {
            val scope = rememberCoroutineScope()
            TextButton(
                onClick = {
                    scope.launch {
                        if (!Build.isOnline) return@launch ConnectionAlert.present()
                        AppSubsystem.delegates.errorReport?.fileReport(exception.hydrated)
                    }
                },
            ) {
                Text(Localized(SubsystemStringKey.REPORT_BUG).wrappedValue, color = colors.accent)
            }
        }
    }
}

private const val FAILURE_GLYPH_SIZE = 120
private const val FAILURE_DESCRIPTOR_SIZE = 22.5f
private val FAILURE_GLYPH_COLOR = Color(0xFFFF3B30)
private const val PROGRESS_SPINNER_DELAY_MILLIS = 500L
private const val PROGRESS_SPINNER_FADE_MILLIS = 200
