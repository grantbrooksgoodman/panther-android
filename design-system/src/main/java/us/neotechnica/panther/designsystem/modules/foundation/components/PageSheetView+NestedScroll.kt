//
//  PageSheetView+NestedScroll.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

// MARK: - Nested Scroll

// Hands scrolling between a sheet and a list inside it: a drag that
// begins with the list at its top lowers the sheet, and an upward swipe
// raises a partly lowered sheet before the list scrolls. A drag that
// begins while the list can still scroll down stays with the list, even
// once the list reaches its top – as with a UIKit scroll view in a sheet.
// While the sheet ignores swipes, the list keeps every drag and fling.
internal class PageSheetNestedScrollConnection(
    private val sheetState: PageSheetState,
    private val velocityThreshold: Float,
    private val isEnabled: () -> Boolean,
) : NestedScrollConnection {
    // MARK: - Properties

    // Whether the current drag moves the sheet: decided by the drag's first
    // movement, or `null` before it.
    private var isDraggingSheet: Boolean? = null

    // MARK: - NestedScrollConnection Conformance

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!isEnabled() || available.y >= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        return Offset(0f, sheetState.draggableState.dispatchRawDelta(available.y))
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!isEnabled() || source != NestedScrollSource.UserInput) return Offset.Zero
        val isDraggingSheet = isDraggingSheet ?: (consumed.y == 0f && available.y > 0f).also { this.isDraggingSheet = it }
        if (!isDraggingSheet) return Offset.Zero
        return Offset(0f, sheetState.draggableState.dispatchRawDelta(available.y))
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        isDraggingSheet = null
        if (!isEnabled()) return Velocity.Zero
        val draggableState = sheetState.draggableState
        if (available.y >= 0f || draggableState.requireOffset() <= draggableState.anchors.minPosition()) return Velocity.Zero
        sheetState.settle(available.y, velocityThreshold)
        return available
    }

    // A fling the list cannot finish moves the sheet only if a drag has
    // already lowered it – a list flung to its top stops there, as a
    // UIKit scroll view in a sheet does.
    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        if (!isEnabled()) return Velocity.Zero
        val draggableState = sheetState.draggableState
        if (draggableState.requireOffset() <= draggableState.anchors.minPosition()) return Velocity.Zero
        sheetState.settle(available.y, velocityThreshold)
        return available
    }
}
