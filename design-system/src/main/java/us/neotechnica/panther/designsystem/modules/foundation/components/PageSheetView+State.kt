//
//  PageSheetView+State.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation

// MARK: - State

// The resting positions of the sheet.
internal enum class SheetValue {
    HIDDEN,
    PRESENTED,
}

// Holds the last non-`null` item a sheet presented.
internal class PresentedItem<T : Any> {
    var value: T? = null
}

// The position of a sheet, measured as its downward offset from where it
// rests when presented.
internal class PageSheetState {
    // MARK: - Properties

    val draggableState = AnchoredDraggableState(SheetValue.HIDDEN)

    var hasAnchors by mutableStateOf(false)
        private set
    private var transitionCount by mutableIntStateOf(0)

    // MARK: - Computed Properties

    // Whether the sheet is animating on behalf of its presentation or
    // dismissal.
    val isTransitioning: Boolean
        get() = transitionCount > 0

    // The sheet's offset, placing it offscreen until its height is known.
    val offset: Float
        get() = draggableState.offset.takeUnless { it.isNaN() } ?: UNMEASURED_OFFSET

    // How far the sheet is presented, from 0 when hidden to 1 when presented.
    val presentedFraction: Float
        get() {
            val hiddenOffset = draggableState.anchors.maxPosition()
            if (!hasAnchors || hiddenOffset <= 0f || draggableState.offset.isNaN()) return 0f
            return (1f - draggableState.offset / hiddenOffset).coerceIn(0f, 1f)
        }

    // MARK: - Methods

    suspend fun dismiss() = transition(to = SheetValue.HIDDEN)

    // Hides the sheet in response to a tap on the dimmed content. A drag
    // interrupts it.
    suspend fun hide() = draggableState.animateTo(SheetValue.HIDDEN, sheetAnimationSpec)

    suspend fun present() = transition(to = SheetValue.PRESENTED)

    // Settles the sheet after a fling handed off by a scrolling list: a
    // fast enough fling decides the direction; otherwise the sheet returns
    // to the nearer resting position.
    suspend fun settle(
        velocity: Float,
        velocityThreshold: Float,
    ) {
        val target =
            when {
                velocity > velocityThreshold -> SheetValue.HIDDEN
                velocity < -velocityThreshold -> SheetValue.PRESENTED
                else -> draggableState.anchors.closestAnchor(draggableState.requireOffset()) ?: SheetValue.PRESENTED
            }
        draggableState.animateTo(target, sheetAnimationSpec)
    }

    // Keeps an animation under way headed for the resting position it
    // was already animating to.
    fun updateAnchors(height: Float) {
        draggableState.updateAnchors(
            DraggableAnchors {
                SheetValue.PRESENTED at 0f
                SheetValue.HIDDEN at height
            },
            draggableState.targetValue,
        )
        hasAnchors = true
    }

    // Animates the sheet to the given resting position on behalf of its
    // presentation or dismissal. The animation outranks any gesture, so
    // a drag or a tap on the dimmed content cannot interrupt it.
    private suspend fun transition(to: SheetValue) {
        transitionCount++
        try {
            draggableState.anchoredDrag(to, MutatePriority.PreventUserInput) { anchors, target ->
                val initialOffset = draggableState.offset
                val targetOffset = anchors.positionOf(target)
                if (initialOffset.isNaN() || targetOffset.isNaN() || initialOffset == targetOffset) return@anchoredDrag
                animate(
                    animationSpec = sheetAnimationSpec,
                    initialValue = initialOffset,
                    initialVelocity = draggableState.lastVelocity,
                    targetValue = targetOffset,
                ) { value, velocity -> dragTo(value, velocity) }
            }
        } finally {
            transitionCount--
        }
    }
}

// MARK: - Constants

// The distance, in pixels, from its resting position at which a sheet
// animation ends.
internal const val SHEET_VISIBILITY_THRESHOLD = 1f

// The spring of every sheet animation, ending once the sheet visibly
// comes to rest.
internal val sheetAnimationSpec: SpringSpec<Float>
    get() = SystemAnimation.sheetTransition(SHEET_VISIBILITY_THRESHOLD)

// An offset that keeps a sheet below the screen before its height is known.
internal const val UNMEASURED_OFFSET = 100_000f
