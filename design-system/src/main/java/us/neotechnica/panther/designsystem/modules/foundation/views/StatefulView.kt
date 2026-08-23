//
//  StatefulView.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

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
 * @param onRetry An optional action performed when the user taps retry
 *   on the failure page. Pass `null` to hide the retry button.
 * @param content The content shown when the state is loaded.
 */
@Composable
fun StatefulView(
    state: ViewState,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
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
            is ViewState.Error -> FailurePage(target.exception, onRetry)
            ViewState.Loaded -> content()
            ViewState.Loading -> ProgressPage()
        }
    }
}

// MARK: - Progress Page

@Composable
private fun ProgressPage() {
    val colors = LocalPantherColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .background(LocalPantherColors.current.background),
    ) {
        CircularProgressIndicator(color = colors.titleText)
    }
}

// MARK: - Failure Page

@Composable
private fun FailurePage(
    exception: Exception,
    onRetry: (() -> Unit)?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxSize()
                .background(LocalPantherColors.current.background)
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            "Something went wrong.",
            color = LocalPantherColors.current.titleText,
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            exception.descriptor,
            color = LocalPantherColors.current.subtitleText,
            style = MaterialTheme.typography.bodyMedium,
        )

        onRetry?.let {
            TextButton(onClick = it) { Text("Retry") }
        }
    }
}
