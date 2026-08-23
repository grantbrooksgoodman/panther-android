//
//  WheelPicker.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.constants.WheelPickerFloats
import kotlin.math.abs

/**
 * A scrolling wheel (spinner) picker, mirroring the iOS `.wheel` picker
 * style: a fixed viewport of rows that snap to center, with the selected
 * row highlighted in a pill and neighboring rows fading with distance.
 *
 * @param items The row labels.
 * @param selectedIndex The index of the currently selected item.
 * @param onSelectedIndexChange Invoked when the settled center changes.
 * @param modifier The modifier for this picker.
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val rowHeightPx = with(LocalDensity.current) { WheelPickerFloats.rowHeight.toPx() }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Keep the wheel aligned when the selection changes from outside.
    LaunchedEffect(selectedIndex, items.size) {
        if (selectedIndex in items.indices &&
            !listState.isScrollInProgress &&
            centeredIndex(listState) != selectedIndex
        ) {
            listState.scrollToItem(selectedIndex)
        }
    }

    // Report the settled center back to the caller.
    LaunchedEffect(listState, items.size) {
        snapshotFlow { listState.isScrollInProgress }.collect { isScrolling ->
            if (!isScrolling) {
                val index = centeredIndex(listState)
                if (index in items.indices && index != selectedIndex) onSelectedIndexChange(index)
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.height(WheelPickerFloats.rowHeight * WheelPickerFloats.VISIBLE_ROW_COUNT),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(WheelPickerFloats.rowHeight)
                    .clip(RoundedCornerShape(WheelPickerFloats.pillCornerRadius))
                    .background(colors.groupedContentBackground),
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(WheelPickerFloats.HALF_ROW_COUNT) { Spacer(Modifier.height(WheelPickerFloats.rowHeight)) }
            itemsIndexed(items) { index, item ->
                val listIndex = index + WheelPickerFloats.HALF_ROW_COUNT
                val alpha by remember { derivedStateOf { rowAlpha(listState, listIndex, rowHeightPx) } }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(WheelPickerFloats.rowHeight),
                ) {
                    Components.Text(
                        item,
                        color = colors.titleText,
                        font = Font.system,
                        modifier = Modifier.graphicsLayer { this.alpha = alpha },
                    )
                }
            }
            items(WheelPickerFloats.HALF_ROW_COUNT) { Spacer(Modifier.height(WheelPickerFloats.rowHeight)) }
        }
    }
}

/** The data index whose row is closest to the viewport center. */
private fun centeredIndex(listState: LazyListState): Int {
    val layoutInfo = listState.layoutInfo
    if (layoutInfo.visibleItemsInfo.isEmpty()) {
        return (listState.firstVisibleItemIndex - WheelPickerFloats.HALF_ROW_COUNT).coerceAtLeast(0)
    }
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val nearest =
        layoutInfo.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2f) - viewportCenter) }
    return ((nearest?.index ?: listState.firstVisibleItemIndex) - WheelPickerFloats.HALF_ROW_COUNT).coerceAtLeast(0)
}

/** The opacity for the row at [listIndex], fading with distance from center. */
private fun rowAlpha(
    listState: LazyListState,
    listIndex: Int,
    rowHeightPx: Float,
): Float {
    val layoutInfo = listState.layoutInfo
    val item =
        layoutInfo.visibleItemsInfo.firstOrNull { it.index == listIndex }
            ?: return WheelPickerFloats.MIN_ROW_ALPHA
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val distance = abs((item.offset + item.size / 2f) - viewportCenter)
    val maxDistance = rowHeightPx * WheelPickerFloats.HALF_ROW_COUNT
    return (1f - distance / maxDistance).coerceIn(WheelPickerFloats.MIN_ROW_ALPHA, 1f)
}
