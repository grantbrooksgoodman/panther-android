//
//  SwipeActionsCoordinationEffect.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.componentkit.models.LocalSwipeActionsCoordinator

/**
 * Reports a row's revealed swipe actions to the list's coordinator,
 * which closes them when the list is touched outside them.
 *
 * Apply this effect in a row that reveals swipe actions through an
 * [AnchoredDraggableState]. It does nothing outside a list that
 * provides a coordinator through
 * [LocalSwipeActionsCoordinator].
 *
 * @param draggableState The state of the row's swipe offset.
 * @param settledValue The anchor at which the row's actions are
 *   hidden.
 * @param rowBounds The row's bounds in root coordinates.
 */
@Composable
fun <T> SwipeActionsCoordinationEffect(
    draggableState: AnchoredDraggableState<T>,
    settledValue: T,
    rowBounds: State<Rect>,
) {
    val coordinator = LocalSwipeActionsCoordinator.current ?: return
    val id = remember { Any() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(draggableState.settledValue) {
        if (draggableState.settledValue == settledValue) return@LaunchedEffect coordinator.rowClosed(id)
        coordinator.rowOpened(
            id = id,
            bounds = { rowBounds.value },
            actionBounds = {
                val bounds = rowBounds.value
                val offset = draggableState.offset.takeIf { !it.isNaN() } ?: 0f
                if (offset > 0f) {
                    Rect(bounds.left, bounds.top, bounds.left + offset, bounds.bottom)
                } else {
                    Rect(bounds.right + offset, bounds.top, bounds.right, bounds.bottom)
                }
            },
            close = { scope.launch { draggableState.animateTo(settledValue) } },
        )
    }

    DisposableEffect(coordinator) { onDispose { coordinator.rowClosed(id) } }
}
