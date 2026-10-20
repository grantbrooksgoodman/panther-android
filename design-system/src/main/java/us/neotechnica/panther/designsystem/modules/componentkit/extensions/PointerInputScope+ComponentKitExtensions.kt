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
import us.neotechnica.panther.designsystem.modules.componentkit.models.GestureRecognizerState
import kotlin.time.Duration

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
 * A long press is continuous: it begins once the press is held for
 * [minimumPressDuration], and from then on [onLongPress] is told
 * each time the held pointer moves – wherever it moves, on or off
 * the bubble – and when the pointer lifts or the gesture is
 * cancelled. Every event of a long press is consumed, so nothing
 * beneath it scrolls or swipes.
 *
 * @param minimumPressDuration How long a press must be held to begin
 *   a long press.
 * @param onDoubleTap The action performed on a double tap, given the
 *   second release's position, or `null` to report every tap at
 *   once.
 * @param onLongPress The action performed as a long press begins,
 *   changes, ends, or is cancelled, given the gesture's state and the
 *   pointer's position, or `null` to ignore long presses.
 * @param onTap The action performed on a tap, given the release
 *   position, or `null` to ignore taps.
 */
internal suspend fun PointerInputScope.detectMessageTapGestures(
    minimumPressDuration: Duration,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((GestureRecognizerState, Offset) -> Unit)? = null,
    onTap: ((Offset) -> Unit)? = null,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    down.consume()
    val firstUp =
        when (val result = awaitReleaseOrLongPress(down, minimumPressDuration, onLongPress)) {
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
    when (val result = awaitReleaseOrLongPress(secondDown, minimumPressDuration, onLongPress)) {
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
// is given – to hold for the minimum press duration, in which case the
// long press begins and is tracked until its pointer lifts.
private suspend fun AwaitPointerEventScope.awaitReleaseOrLongPress(
    down: PointerInputChange,
    minimumPressDuration: Duration,
    onLongPress: ((GestureRecognizerState, Offset) -> Unit)?,
): PressResult {
    if (onLongPress == null) return awaitReleaseOrCancellation(down)
    val result =
        withTimeoutOrNull(minimumPressDuration.inWholeMilliseconds) {
            awaitReleaseOrCancellation(down)
        }
    if (result != null) return result

    onLongPress(GestureRecognizerState.BEGAN, down.position)
    trackLongPress(down, onLongPress)
    return PressResult.LongPressed
}

// Reports each movement of a long press's pointer, then its release
// – or its cancellation, which arrives as a release already
// consumed – consuming every event along the way.
private suspend fun AwaitPointerEventScope.trackLongPress(
    down: PointerInputChange,
    onLongPress: (GestureRecognizerState, Offset) -> Unit,
) {
    var position = down.position
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
        if (event.changes.none { it.pressed }) {
            val state = if (change.isConsumed) GestureRecognizerState.CANCELLED else GestureRecognizerState.ENDED
            event.changes.forEach { it.consume() }
            return onLongPress(state, change.position)
        }

        event.changes.forEach { it.consume() }
        if (change.position == position) continue
        position = change.position
        onLongPress(GestureRecognizerState.CHANGED, position)
    }
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
