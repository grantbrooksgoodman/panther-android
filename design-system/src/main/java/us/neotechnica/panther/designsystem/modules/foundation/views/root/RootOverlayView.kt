//
//  RootOverlayView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.foundation.constants.RootOverlayViewFloats
import us.neotechnica.panther.designsystem.modules.foundation.extensions.rootViewTapped
import us.neotechnica.panther.designsystem.modules.foundation.views.buildinfooverlayview.BuildInfoOverlayReducer
import us.neotechnica.panther.designsystem.modules.foundation.views.buildinfooverlayview.BuildInfoOverlayView
import us.neotechnica.panther.subsystem.modules.foundation.services.BuildInfoOverlay
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvent

// MARK: - Constants Accessors

private typealias Floats = RootOverlayViewFloats

// MARK: - Body

/**
 * Displays the build info overlay above the app's content.
 *
 * Place a single [RootOverlayView] above the app's content, after
 * every other root host, so that it remains frontmost. The view
 * renders nothing while the build info overlay is hidden.
 */
@Composable
fun RootOverlayView() {
    val isBuildInfoOverlayHidden by BuildInfoOverlay.isHidden.collectAsState()
    val rootOverlayWindowAlpha by RootWindowStatus.rootOverlayWindowAlpha.collectAsState()
    if (isBuildInfoOverlayHidden) return

    val safeAreaBottomInsets = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    val buildInfoOverlayYOffset = if (safeAreaBottomInsets == 0) BOTTOM_Y_OFFSET else INSET_BOTTOM_Y_OFFSET

    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(rootOverlayWindowAlpha)
                .navigationBarsPadding()
                .padding(bottom = 1.dp),
    ) {
        // The window's insets arrive after the first frame, so the
        // overlay is recreated once they resolve.
        val buildInfoOverlayViewModel =
            remember(buildInfoOverlayYOffset) {
                ViewModel(
                    BuildInfoOverlayReducer.State(yOffset = buildInfoOverlayYOffset),
                    BuildInfoOverlayReducer(),
                ).observing(
                    SharedEvent { it.rootViewTapped }.wrappedValue.events,
                ) { BuildInfoOverlayReducer.Action.RootViewTapped }
            }

        DisposableEffect(buildInfoOverlayViewModel) {
            onDispose { buildInfoOverlayViewModel.close() }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(Floats.BUILD_INFO_OVERLAY_FRAME_MAX_HEIGHT.dp),
        ) {
            BuildInfoOverlayView(buildInfoOverlayViewModel)
        }
    }
}

private const val BOTTOM_Y_OFFSET = 10f
private const val INSET_BOTTOM_Y_OFFSET = 30f
