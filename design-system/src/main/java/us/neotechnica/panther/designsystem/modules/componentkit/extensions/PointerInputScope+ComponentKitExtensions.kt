//
//  PointerInputScope+ComponentKitExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.extensions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Detects taps, double taps, and long presses on a message bubble.
 *
 * This detector follows `detectTapGestures`, with two differences for
 * a bubble inside a scrolling list:
 *
 * - A press whose down event an ancestor consumed – the list claiming
 *   the touch to stop a fling – still begins a gesture, so a press on
 *   a list that has not quite settled is not dropped.
 * - A press is cancelled only when the pointer moves past the touch
 *   slop, leaves the bubble, or lifts – not when an ancestor consumes
 *   a position change – so the list dragging along with a still
 *   finger does not cancel the long press.
 *
 * A release that a descendant consumed, such as an audio bubble's
 * play button, does not count as a tap. When [onDoubleTap] is
 * non-`null`, a single tap is reported only once the double-tap
 * timeout passes without a second press.
 *
 * @param onDoubleTap The action performed on a double tap, given the
 *   second release's position, or `null` to report every tap at
 *   once.
 * @param onLongPress The action performed on a long press, given the
 *   press position, or `null` to ignore long presses.
 * @param onTap The action performed on a tap, given the release
 *   position, or `null` to ignore taps.
 */
internal suspend fun PointerInputScope.detectMessageTapGestures(
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onTap: ((Offset) -> Unit)? = null,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    down.consume()
    val firstUp =
        when (val result = awaitReleaseOrLongPress(down, onLongPress)) {
            is PressResult.Released -> result.up
            PressResult.LongPressed, PressResult.Cancelled -> return@awaitEachGesture
        }
    firstUp.consume()

    if (onDoubleTap == null) {
        onTap?.invoke(firstUp.position)
        return@awaitEachGesture
    }

    val secondDown = awaitSecondDown(firstUp)
    if (secondDown == null) {
        onTap?.invoke(firstUp.position)
        return@awaitEachGesture
    }

    // A second press that lifts in time is a double tap; one that is
    // cancelled leaves the first tap standing; one that is held is a
    // long press alone.
    when (val result = awaitReleaseOrLongPress(secondDown, onLongPress)) {
        is PressResult.Released -> {
            result.up.consume()
            onDoubleTap(result.up.position)
        }

        PressResult.Cancelled -> onTap?.invoke(firstUp.position)
        PressResult.LongPressed -> Unit
    }
}

// MARK: - Auxiliary

// The outcome of a press: lifted within the bubble, held into a long
// press, or cancelled.
private sealed interface PressResult {
    data class Released(
        val up: PointerInputChange,
    ) : PressResult

    data object LongPressed : PressResult

    data object Cancelled : PressResult
}

// Waits for the press to lift or cancel or – when a long-press action
// is given – to hold for the long-press timeout, in which case the
// action runs and the rest of the gesture is consumed.
private suspend fun AwaitPointerEventScope.awaitReleaseOrLongPress(
    down: PointerInputChange,
    onLongPress: ((Offset) -> Unit)?,
): PressResult {
    if (onLongPress == null) return awaitReleaseOrCancellation(down)
    val result =
        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            awaitReleaseOrCancellation(down)
        }
    if (result != null) return result

    onLongPress(down.position)
    consumeUntilUp()
    return PressResult.LongPressed
}

// Waits for every pointer to lift, cancelling if a pointer leaves the
// bubble or moves past the touch slop. A consumed release cancels; a
// consumed position change does not.
private suspend fun AwaitPointerEventScope.awaitReleaseOrCancellation(down: PointerInputChange): PressResult {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.all { it.changedToUpIgnoreConsumed() }) {
            val up = event.changes.first()
            return if (up.isConsumed) PressResult.Cancelled else PressResult.Released(up)
        }

        val isCancelled =
            event.changes.any { change ->
                change.isOutOfBounds(size, extendedTouchPadding) ||
                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
            }
        if (isCancelled) return PressResult.Cancelled
    }
}

// Waits up to the double-tap timeout for a second press that lands no
// sooner than the double-tap minimum time after the first release.
private suspend fun AwaitPointerEventScope.awaitSecondDown(firstUp: PointerInputChange): PointerInputChange? =
    withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
        val minimumUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
        var change: PointerInputChange
        do {
            change = awaitFirstDown()
        } while (change.uptimeMillis < minimumUptime)
        change
    }

// Consumes every event until no pointer is pressed.
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
}
