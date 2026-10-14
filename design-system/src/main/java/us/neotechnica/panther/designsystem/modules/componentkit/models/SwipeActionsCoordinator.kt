//
//  SwipeActionsCoordinator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * The coordinator of the list whose rows can reveal swipe actions,
 * provided to the rows of that list, or `null` outside one.
 */
val LocalSwipeActionsCoordinator = staticCompositionLocalOf<SwipeActionsCoordinator?> { null }

/**
 * Tracks the one row of a list whose swipe actions are revealed.
 *
 * Provide a coordinator to a list's rows through
 * [LocalSwipeActionsCoordinator] and apply [dismissalModifier] to the
 * list. While a row is open, a touch anywhere on the list outside its
 * revealed actions closes it and goes no further: a tap on the open
 * row closes it rather than activating the row, and a touch elsewhere
 * closes it rather than reaching the view beneath. Dragging the open
 * row itself still slides it closed.
 */
class SwipeActionsCoordinator {
    // MARK: - Types

    private class OpenRow(
        val id: Any,
        val bounds: () -> Rect,
        val actionBounds: () -> Rect,
        val close: () -> Unit,
    )

    // MARK: - Properties

    private var openRow: OpenRow? = null
    private var listCoordinates: LayoutCoordinates? = null

    // MARK: - Methods

    /**
     * Records that the row with the given identifier closed.
     *
     * @param id The identifier of the row.
     */
    fun rowClosed(id: Any) {
        if (openRow?.id == id) openRow = null
    }

    /**
     * Records that the row with the given identifier revealed its
     * swipe actions, closing any other open row.
     *
     * @param id The identifier of the row.
     * @param bounds Returns the row's bounds in root coordinates.
     * @param actionBounds Returns the bounds of the row's revealed
     *   actions in root coordinates.
     * @param close Closes the row's swipe actions.
     */
    fun rowOpened(
        id: Any,
        bounds: () -> Rect,
        actionBounds: () -> Rect,
        close: () -> Unit,
    ) {
        openRow?.takeIf { it.id != id }?.close?.invoke()
        openRow = OpenRow(id, bounds, actionBounds, close)
    }

    /**
     * Returns a modifier for the list that dismisses the open row's
     * swipe actions when the list is touched outside them.
     *
     * @return The modifier.
     */
    fun dismissalModifier(): Modifier =
        Modifier
            .onGloballyPositioned { listCoordinates = it }
            .pointerInput(this) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val row = openRow ?: return@awaitEachGesture
                    val position = listCoordinates?.localToRoot(down.position) ?: return@awaitEachGesture
                    if (row.actionBounds().contains(position)) return@awaitEachGesture

                    down.consume()
                    val isOnRow = row.bounds().contains(position)
                    if (!isOnRow) row.close()

                    var isTracking = true
                    while (isTracking) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                        when {
                            change == null -> isTracking = false

                            change.changedToUp() -> {
                                change.consume()
                                if (isOnRow) row.close()
                                isTracking = false
                            }

                            // A drag on the open row slides it closed itself.
                            isOnRow && (change.position - down.position).getDistance() > viewConfiguration.touchSlop -> isTracking = false

                            !isOnRow -> change.consume()
                        }
                    }
                }
            }
}
