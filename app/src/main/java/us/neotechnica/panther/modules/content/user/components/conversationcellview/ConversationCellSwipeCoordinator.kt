//
//  ConversationCellSwipeCoordinator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.conversationcellview

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
 * The conversation cell whose swipe actions the user can currently see,
 * provided to the cells of the conversations page.
 */
internal val LocalConversationCellSwipeCoordinator = staticCompositionLocalOf<ConversationCellSwipeCoordinator?> { null }

// Tracks the one conversation cell whose swipe actions are revealed.
// While a cell is open, a touch anywhere on the page outside its revealed
// actions closes it and goes no further: a tap on the open cell closes it
// rather than opening the conversation, and a touch elsewhere closes it
// rather than reaching the view beneath. Dragging the open cell itself
// still slides it closed.
internal class ConversationCellSwipeCoordinator {
    // MARK: - Types

    private class OpenCell(
        val id: Any,
        val bounds: () -> Rect,
        val actionBounds: () -> Rect,
        val close: () -> Unit,
    )

    // MARK: - Properties

    private var openCell: OpenCell? = null
    private var pageCoordinates: LayoutCoordinates? = null

    // MARK: - Methods

    // Records that the cell with the given identifier closed.
    fun cellClosed(id: Any) {
        if (openCell?.id == id) openCell = null
    }

    // Records that the cell with the given identifier revealed its swipe
    // actions, closing any other open cell. Bounds are in root coordinates.
    fun cellOpened(
        id: Any,
        bounds: () -> Rect,
        actionBounds: () -> Rect,
        close: () -> Unit,
    ) {
        openCell?.takeIf { it.id != id }?.close?.invoke()
        openCell = OpenCell(id, bounds, actionBounds, close)
    }

    // A modifier for the page that dismisses the open cell's swipe actions
    // when the page is touched outside them.
    fun dismissalModifier(): Modifier =
        Modifier
            .onGloballyPositioned { pageCoordinates = it }
            .pointerInput(this) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val cell = openCell ?: return@awaitEachGesture
                    val position = pageCoordinates?.localToRoot(down.position) ?: return@awaitEachGesture
                    if (cell.actionBounds().contains(position)) return@awaitEachGesture

                    down.consume()
                    val isOnCell = cell.bounds().contains(position)
                    if (!isOnCell) cell.close()

                    var isTracking = true
                    while (isTracking) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                        when {
                            change == null -> isTracking = false

                            change.changedToUp() -> {
                                change.consume()
                                if (isOnCell) cell.close()
                                isTracking = false
                            }

                            // A drag on the open cell slides it closed itself.
                            isOnCell && (change.position - down.position).getDistance() > viewConfiguration.touchSlop -> isTracking = false

                            !isOnCell -> change.consume()
                        }
                    }
                }
            }
}
