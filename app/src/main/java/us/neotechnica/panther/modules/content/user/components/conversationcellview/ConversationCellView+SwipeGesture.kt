//
//  ConversationCellView+SwipeGesture.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.conversationcellview

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

// MARK: - Swipe Gesture

/**
 * Drags a conversation cell's swipe actions open or closed with
 * horizontal swipes, leaving any other gesture to scroll the list.
 *
 * A swipe begins only once its horizontal movement passes the touch
 * slop while at least twice its vertical movement, so a scroll that
 * drifts sideways never reveals the actions. On release, the cell
 * settles as `anchoredDraggable` settles it: a fling moves it to the
 * next anchor in the fling's direction, and a slower release to the
 * nearest anchor.
 *
 * @param state The state of the cell's swipe offset.
 * @param enabled Whether the cell responds to swipes.
 */
internal fun <T> Modifier.swipeActionsDraggable(
    state: AnchoredDraggableState<T>,
    enabled: Boolean,
): Modifier =
    pointerInput(state, enabled) {
        if (!enabled) return@pointerInput
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val initialDelta = awaitHorizontalSwipe(down.id) ?: return@awaitEachGesture

                // The drag holds the state's drag lock, interrupting any
                // settling animation, and follows the pointer's movements.
                val deltas = Channel<Float>(Channel.UNLIMITED).apply { trySend(initialDelta) }
                val drag =
                    launch {
                        state.anchoredDrag { anchors ->
                            for (delta in deltas) {
                                dragTo((state.requireOffset() + delta).coerceIn(anchors.minPosition(), anchors.maxPosition()))
                            }
                        }
                    }

                val velocityTracker = VelocityTracker()
                horizontalDrag(down.id) { change ->
                    velocityTracker.addPointerInputChange(change)
                    deltas.trySend(change.positionChange().x)
                    change.consume()
                }

                deltas.close()
                val velocity = velocityTracker.calculateVelocity().x
                val minFlingVelocity = MIN_FLING_VELOCITY.toPx()
                launch {
                    drag.join()
                    state.animateTo(state.settleTarget(velocity, minFlingVelocity))
                }
            }
        }
    }

// MARK: - Auxiliary

// Waits for the given pointer to begin a horizontal swipe, returning its
// horizontal movement past the touch slop, or `null` once the pointer
// lifts, its movement is consumed, or it moves past the touch slop in
// any other direction.
private suspend fun AwaitPointerEventScope.awaitHorizontalSwipe(pointerID: PointerId): Float? {
    val touchSlop = viewConfiguration.touchSlop
    var totalMovement = Offset.Zero

    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointerID } ?: return null
        if (change.changedToUpIgnoreConsumed() || change.isConsumed) return null
        totalMovement += change.positionChange()

        val horizontalMovement = abs(totalMovement.x)
        val verticalMovement = abs(totalMovement.y)
        if (horizontalMovement > touchSlop && horizontalMovement >= verticalMovement * HORIZONTAL_DOMINANCE_RATIO) {
            change.consume()
            return totalMovement.x - touchSlop * sign(totalMovement.x)
        }

        if (verticalMovement > touchSlop) return null
    }
}

// The anchor a release with the given velocity settles at: the next
// anchor in a fling's direction, or the anchor nearest a slower release.
private fun <T> AnchoredDraggableState<T>.settleTarget(
    velocity: Float,
    minFlingVelocity: Float,
): T {
    val offset = requireOffset()
    val target =
        if (abs(velocity) < minFlingVelocity) {
            anchors.closestAnchor(offset)
        } else {
            anchors.closestAnchor(offset, searchUpwards = velocity > 0)
        }

    return target ?: currentValue
}

// How many times the vertical movement a swipe's horizontal movement
// must be when it passes the touch slop.
private const val HORIZONTAL_DOMINANCE_RATIO = 2f

// The speed, per second, past which a release flings the cell to the
// next anchor – `anchoredDraggable`'s minimum fling velocity.
private val MIN_FLING_VELOCITY = 125.dp
