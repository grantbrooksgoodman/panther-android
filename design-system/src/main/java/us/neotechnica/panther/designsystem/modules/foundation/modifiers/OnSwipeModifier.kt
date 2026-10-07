//
//  OnSwipeModifier.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.modifiers

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Configuration values for swipe-gesture recognition. */
object SwipeModifierConfig {
    /** The minimum drag distance for a gesture to qualify as a swipe. */
    const val MINIMUM_DRAG_GESTURE_DISTANCE = 30f
}

/** The directions a swipe gesture can travel. */
enum class Swipe {
    /** A downward swipe. */
    DOWN,

    /** A leftward swipe. */
    LEFT,

    /** A rightward swipe. */
    RIGHT,

    /** An upward swipe. */
    UP,

    ;

    // MARK: - Companion

    companion object {
        /** Every swipe direction. */
        val all: Set<Swipe> = setOf(DOWN, LEFT, RIGHT, UP)
    }
}

/**
 * Performs an action when the user swipes over this element in any
 * of the given directions.
 *
 * The action runs once the drag ends, provided the accumulated
 * distance along a matching direction exceeds the recognition
 * threshold. Apply this modifier to an element that fills the area
 * you want the gesture to cover.
 *
 * @param swipe The directions that trigger the action.
 * @param sensitivity A multiplier applied to the recognition
 *   threshold. The default is `1`.
 * @param action The closure to run when a matching swipe is
 *   detected.
 *
 * @return A modifier that recognizes the given swipes.
 */
fun Modifier.onSwipe(
    swipe: Set<Swipe>,
    sensitivity: Float = 1f,
    action: () -> Unit,
): Modifier =
    pointerInput(swipe, sensitivity, action) {
        val threshold = SwipeModifierConfig.MINIMUM_DRAG_GESTURE_DISTANCE.dp.toPx() * sensitivity
        var totalX = 0f
        var totalY = 0f
        detectDragGestures(
            onDragStart = {
                totalX = 0f
                totalY = 0f
            },
            onDragEnd = {
                val swiped =
                    swipe.any { direction ->
                        when (direction) {
                            Swipe.DOWN -> totalY > threshold
                            Swipe.LEFT -> totalX < -threshold
                            Swipe.RIGHT -> totalX > threshold
                            Swipe.UP -> totalY < -threshold
                        }
                    }

                if (swiped) action()
            },
            onDragCancel = {
                totalX = 0f
                totalY = 0f
            },
        ) { _, dragAmount ->
            totalX += dragAmount.x
            totalY += dragAmount.y
        }
    }

/**
 * Performs an action when the user swipes over this element in the
 * given direction.
 *
 * @param swipe The direction that triggers the action.
 * @param sensitivity A multiplier applied to the recognition
 *   threshold. The default is `1`.
 * @param action The closure to run when a matching swipe is
 *   detected.
 *
 * @return A modifier that recognizes the given swipe.
 */
fun Modifier.onSwipe(
    swipe: Swipe,
    sensitivity: Float = 1f,
    action: () -> Unit,
): Modifier = onSwipe(setOf(swipe), sensitivity, action)
