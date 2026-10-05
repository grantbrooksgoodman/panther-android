//
//  OnSwipeModifier.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.modifiers

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

// MARK: - Constants

private val MINIMUM_SWIPE_DISTANCE = 30.dp

/**
 * Performs an action when the user swipes down over this element.
 *
 * The action runs once the drag ends, provided the accumulated
 * downward distance exceeds the recognition threshold. Apply this
 * modifier to an element that fills the area you want the gesture to
 * cover.
 *
 * @param action The closure to run when a downward swipe is detected.
 *
 * @return A modifier that recognizes downward swipes.
 */
fun Modifier.onSwipeDown(action: () -> Unit): Modifier =
    pointerInput(action) {
        val threshold = MINIMUM_SWIPE_DISTANCE.toPx()
        var totalDrag = 0f
        detectVerticalDragGestures(
            onDragStart = { totalDrag = 0f },
            onDragEnd = { if (totalDrag > threshold) action() },
            onDragCancel = { totalDrag = 0f },
        ) { _, dragAmount -> totalDrag += dragAmount }
    }
