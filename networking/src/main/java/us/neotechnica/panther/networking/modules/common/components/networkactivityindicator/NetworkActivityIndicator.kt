//
//  NetworkActivityIndicator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.components.networkactivityindicator

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.networking.modules.common.constants.NetworkActivityIndicatorColors
import us.neotechnica.panther.networking.modules.common.constants.NetworkActivityIndicatorFloats
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Colors = NetworkActivityIndicatorColors
private typealias Floats = NetworkActivityIndicatorFloats

// MARK: - View

/**
 * A circular indicator that reflects in-flight network activity.
 *
 * The indicator's background reflects the current network health
 * tier, and tapping it performs the registered delegate's tap
 * action – or presents a summary of the current network health.
 *
 * @param viewModel The view model that drives the indicator.
 * @param modifier The modifier for this indicator.
 */
@Composable
fun NetworkActivityIndicator(
    viewModel: ViewModel<NetworkActivityIndicatorReducer.State, NetworkActivityIndicatorReducer.Action>,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val yOffset by animateFloatAsState(
        targetValue = state.yOffset,
        animationSpec = spring(),
        label = "NetworkActivityIndicatorYOffset",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .offset(y = yOffset.dp)
                .alpha(if (state.isVisible) 1f else 0f)
                .size(
                    width = Floats.FRAME_WIDTH.dp,
                    height = Floats.FRAME_HEIGHT.dp,
                ).padding(Floats.PADDING.dp)
                .clip(CircleShape)
                .background(state.backgroundColor ?: Colors.GLASS_EFFECT_TINT)
                .clickable(enabled = state.allowsHitTesting) {
                    viewModel.send(NetworkActivityIndicatorReducer.Action.IndicatorTapped)
                },
    ) {
        CircularProgressIndicator(
            color = state.progressViewTintColor ?: Color.White,
            strokeWidth = PROGRESS_STROKE_WIDTH.dp,
            modifier = Modifier.size(PROGRESS_SIZE.dp),
        )
    }
}

private const val PROGRESS_SIZE = 20
private const val PROGRESS_STROKE_WIDTH = 2
