//
//  LazyGroupedListView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.foundation.constants.GroupedListViewColors
import us.neotechnica.panther.designsystem.modules.foundation.constants.GroupedListViewFloats
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Constants Accessors

private typealias LazyGroupedListColors = GroupedListViewColors
private typealias LazyGroupedListFloats = GroupedListViewFloats

/**
 * A scrolling list of rows in the inset grouped style – a card with
 * rounded corners, inset from the list's edges, with a divider between
 * each pair of rows – composing only the rows on screen.
 *
 * Use this in place of [GroupedListView] for a group too long to
 * compose at once. Draw the list over the grouped content background;
 * it pads its rows above and below, and extends beneath
 * the navigation bar and keyboard, scrolling its last row clear of
 * them:
 *
 * ```kotlin
 * LazyGroupedListView(languageCodes, key = { it }, onSelect = ::select) { languageCode ->
 *     Components.Text(languageNames.getValue(languageCode))
 * }
 * ```
 *
 * Each row lays out its content in a row, centered vertically and
 * inset from the card's edges.
 *
 * @param items The items to list, one row per item, in display order.
 * @param onSelect The action performed when the user taps an item's
 *   row.
 * @param modifier The modifier for this view.
 * @param key A factory of stable keys for the items, or `null` to key
 *   them by position.
 * @param state The list's scroll state.
 * @param dividerLeadingPadding The distance from the card's leading
 *   edge at which each divider begins. The default aligns the dividers
 *   with the rows' content; to align them with text that follows a
 *   leading image, add the image's width and spacing.
 * @param content The content of an item's row.
 */
@Composable
@Suppress("LongParameterList")
fun <T> LazyGroupedListView(
    items: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    state: LazyListState = rememberLazyListState(),
    dividerLeadingPadding: Dp = LazyGroupedListFloats.ROW_HORIZONTAL_PADDING.dp,
    content: @Composable RowScope.(T) -> Unit,
) {
    val bottomSafeAreaInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        state = state,
        contentPadding =
            PaddingValues(
                top = LazyGroupedListFloats.LIST_VERTICAL_PADDING.dp,
                bottom = LazyGroupedListFloats.LIST_VERTICAL_PADDING.dp + bottomSafeAreaInset,
            ),
        modifier = modifier.fillMaxSize(),
    ) {
        itemsIndexed(items, key = key?.let { itemKey -> { _, item -> itemKey(item) } }) { index, item ->
            GroupedListRow(
                isFirst = index == 0,
                isLast = index == items.lastIndex,
                dividerLeadingPadding = dividerLeadingPadding,
                onClick = { onSelect(item) },
            ) { content(item) }
        }
    }
}

// MARK: - Auxiliary

// A row of an inset grouped card, rounded where it begins or ends the
// card, with a divider below it unless it ends the card.
@Composable
private fun GroupedListRow(
    isFirst: Boolean,
    isLast: Boolean,
    dividerLeadingPadding: Dp,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val dividerColor = if (isDarkModeActive) LazyGroupedListColors.DARK_DIVIDER else LazyGroupedListColors.LIGHT_DIVIDER
    val topCornerRadius = if (isFirst) LazyGroupedListFloats.CORNER_RADIUS.dp else 0.dp
    val bottomCornerRadius = if (isLast) LazyGroupedListFloats.CORNER_RADIUS.dp else 0.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(horizontal = LazyGroupedListFloats.CARD_HORIZONTAL_MARGIN.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topCornerRadius, topCornerRadius, bottomCornerRadius, bottomCornerRadius))
                .background(LocalPantherColors.current.groupedRowBackground)
                .drawBehind {
                    if (isLast) return@drawBehind

                    // A hairline along the row's bottom, ending at the card's
                    // trailing edge.
                    val y = size.height - HAIRLINE_WIDTH / 2
                    drawLine(
                        color = dividerColor,
                        start = Offset(dividerLeadingPadding.toPx(), y),
                        end = Offset(size.width, y),
                        strokeWidth = HAIRLINE_WIDTH,
                    )
                }.clickable(onClick = onClick)
                .heightIn(min = LazyGroupedListFloats.ROW_MIN_HEIGHT.dp)
                .padding(
                    horizontal = LazyGroupedListFloats.ROW_HORIZONTAL_PADDING.dp,
                    vertical = LazyGroupedListFloats.ROW_VERTICAL_PADDING.dp,
                ),
        content = content,
    )
}

// The width, in pixels, of a divider.
private const val HAIRLINE_WIDTH = 1f
