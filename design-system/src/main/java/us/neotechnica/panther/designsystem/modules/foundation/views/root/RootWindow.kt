//
//  RootWindow.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.root

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import us.neotechnica.panther.designsystem.modules.foundation.extensions.rootViewTapped
import us.neotechnica.panther.designsystem.modules.foundation.views.forcedupdatemodalpageview.ForcedUpdateModalPageReducer
import us.neotechnica.panther.designsystem.modules.foundation.views.forcedupdatemodalpageview.ForcedUpdateModalPageView
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvent
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import us.neotechnica.panther.subsystem.modules.shared.models.isForcedUpdateRequired
import us.neotechnica.panther.subsystem.modules.shared.models.send

/**
 * The status of the app's root window.
 *
 * Use [RootWindowStatus] to determine which page the root window
 * displays. The root window shows the app's content until a forced
 * update is required, at which point it replaces that content with
 * the forced-update modal for the remainder of the session.
 */
object RootWindowStatus {
    // MARK: - Types

    /** The pages the root window can display. */
    enum class RootView {
        /** The app's own content. */
        APP_CONTENT,

        /** The undismissable forced-update modal. */
        FORCED_UPDATE_MODAL_PAGE,
    }

    // MARK: - Properties

    private val mutableRootOverlayWindowAlpha = MutableStateFlow(1f)
    private val mutableRootView = MutableStateFlow(RootView.APP_CONTENT)

    // MARK: - Computed Properties

    /** The page the root window currently displays. */
    val rootView: StateFlow<RootView> = mutableRootView.asStateFlow()

    internal val rootOverlayWindowAlpha: StateFlow<Float> = mutableRootOverlayWindowAlpha.asStateFlow()

    // MARK: - Methods

    internal suspend fun listenForForcedUpdateStatusChanges() {
        if (AppSubsystem.delegates.forcedUpdateModal == null) return
        SharedState { it.isForcedUpdateRequired }
            .projectedValue
            .changes
            .first { it }

        mutableRootView.value = RootView.FORCED_UPDATE_MODAL_PAGE
    }

    internal fun setRootOverlayWindowAlpha(rootOverlayWindowAlpha: Float) {
        mutableRootOverlayWindowAlpha.value = rootOverlayWindowAlpha
    }
}

/**
 * Displays the page the root window's status specifies.
 *
 * Place [RootWindow] at the root of the app's composition, wrapping
 * the app's content. When a forced update becomes required – and
 * a forced-update modal delegate is registered – the window
 * replaces `content` with the forced-update modal. In prerelease
 * builds, every touch on the window sends the `rootViewTapped`
 * shared event.
 *
 * @param content The app's content.
 */
@Composable
fun RootWindow(content: @Composable () -> Unit) {
    val rootView by RootWindowStatus.rootView.collectAsState()

    LaunchedEffect(Unit) {
        RootWindowStatus.listenForForcedUpdateStatusChanges()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    if (Build.milestone == Build.Milestone.GENERAL_RELEASE) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )

                        SharedEvent { it.rootViewTapped }.wrappedValue.send()
                    }
                },
    ) {
        when (rootView) {
            RootWindowStatus.RootView.APP_CONTENT -> content()

            RootWindowStatus.RootView.FORCED_UPDATE_MODAL_PAGE ->
                ForcedUpdateModalPageView(
                    remember {
                        ViewModel(
                            ForcedUpdateModalPageReducer.State(),
                            ForcedUpdateModalPageReducer(),
                        )
                    },
                )
        }
    }
}
