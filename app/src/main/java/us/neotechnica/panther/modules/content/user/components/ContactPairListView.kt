//
//  ContactPairListView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.constants.ContactPairListViewColors
import us.neotechnica.panther.modules.content.user.constants.ContactPairListViewFloats
import us.neotechnica.panther.modules.content.user.extensions.alphabeticallySorted
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash

// MARK: - Constants Accessors

private typealias ListColors = ContactPairListViewColors
private typealias ListFloats = ContactPairListViewFloats

// MARK: - List View

/**
 * A list of contact pairs in alphabetical sections.
 *
 * Use `ContactPairListView` to offer contact pairs to choose from. Each
 * section is headed by its letter, and a divider ends each header and
 * row, inset to the content's leading edge. Once a section's header
 * scrolls past the top of the list, it floats there until the next
 * section's header – sliding beneath it – reaches the top in turn.
 * Rows respond to taps and highlight from edge to edge when pressed,
 * unless their contact pair can't be selected.
 *
 * The list draws no background of its own; place it over the page's
 * background.
 *
 * @param sections The contact pairs to list, keyed by section letter.
 *   The sections are listed in alphabetical order of their letters.
 * @param onSelect The action performed when the user taps a contact
 *   pair's row.
 * @param modifier The modifier for this view.
 * @param state The list's scroll state.
 * @param contentPadding The padding around the list's content – such
 *   as the bottom safe area, for a list that extends beneath the
 *   navigation bar and keyboard.
 */
@Composable
fun ContactPairListView(
    sections: Map<String, List<ContactPair>>,
    onSelect: (ContactPair) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = LocalPantherColors.current
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val dividerColor = if (isDarkModeActive) ListColors.darkDivider else ListColors.lightDivider
    val letters = remember(sections) { sections.keys.toList().alphabeticallySorted }
    val overscrollEffect = rememberOverscrollEffect()
    val pinnedLetter by rememberPinnedLetter(state, letters, sections)

    Box(modifier = modifier) {
        CompositionLocalProvider(
            LocalIndication provides
                RowHighlightIndication(
                    if (isDarkModeActive) ListColors.rowDarkHighlight else ListColors.rowLightHighlight,
                ),
        ) {
            LazyColumn(
                state = state,
                contentPadding = contentPadding,
                overscrollEffect = overscrollEffect,
                modifier = Modifier.fillMaxSize(),
            ) {
                letters.forEach { letter ->
                    item(key = "section-$letter") {
                        SectionHeaderView(
                            letter,
                            modifier =
                                Modifier
                                    .padding(top = ListFloats.sectionHeaderTopPadding)
                                    .divider(dividerColor),
                        )
                    }

                    items(sections[letter].orEmpty(), key = { it.contact.encodedHash }) { contactPair ->
                        ContactPairCellView(
                            contactPair = contactPair,
                            modifier =
                                Modifier
                                    .divider(dividerColor)
                                    .heightIn(min = ListFloats.defaultMinListRowHeight),
                            contentPadding = PaddingValues(horizontal = ListFloats.rowHorizontalPadding),
                            action = { onSelect(contactPair) },
                        )
                    }
                }
            }
        }

        // The floating header stays in place while hidden, so that a drag on
        // it that scrolls the list back to the top carries on once it hides.
        // Like the list it covers, it scrolls when dragged; unlike the list,
        // it keeps taps from reaching the rows beneath it.
        SectionHeaderView(
            pinnedLetter.orEmpty(),
            modifier =
                Modifier
                    .scrollable(
                        state = state,
                        orientation = Orientation.Vertical,
                        overscrollEffect = overscrollEffect,
                        reverseDirection =
                            ScrollableDefaults.reverseDirection(
                                LocalLayoutDirection.current,
                                Orientation.Vertical,
                                reverseScrolling = false,
                            ),
                    ).then(if (pinnedLetter == null) Modifier else Modifier.background(colors.navigationBarBackground)),
        )
    }
}

// MARK: - Auxiliary

// A section's header: its letter, at the content's leading edge.
@Composable
private fun SectionHeaderView(
    letter: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            modifier
                .fillMaxWidth()
                .height(ListFloats.sectionHeaderFrameHeight)
                .padding(horizontal = ListFloats.rowHorizontalPadding),
    ) {
        Components.Text(letter, foregroundColor = LocalPantherColors.current.titleText)
    }
}

// Returns the letter of the section whose header floats at the top of
// the list – the last section whose header has scrolled past the top –
// or `null` while every header is in place.
@Composable
private fun rememberPinnedLetter(
    listState: LazyListState,
    letters: List<String>,
    sections: Map<String, List<ContactPair>>,
): State<String?> {
    // A header's label begins below the spacing above the header.
    val headerTopPadding = with(LocalDensity.current) { ListFloats.sectionHeaderTopPadding.roundToPx() }
    val headerIndices =
        remember(letters, sections) {
            letters
                .runningFold(0) { index, letter -> index + 1 + sections[letter].orEmpty().size }
                .dropLast(1)
        }

    return remember(listState, letters, headerIndices, headerTopPadding) {
        derivedStateOf {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
            val scrolledPastCount =
                headerIndices.count { index ->
                    index < firstVisibleItemIndex ||
                        (index == firstVisibleItemIndex && firstVisibleItemScrollOffset > headerTopPadding)
                }

            letters.getOrNull(scrolledPastCount - 1)
        }
    }
}

// Draws a divider along the bottom edge, from the content's leading edge
// to the trailing edge.
private fun Modifier.divider(color: Color): Modifier =
    drawBehind {
        val inset = ListFloats.rowHorizontalPadding.toPx()
        val y = size.height - HAIRLINE_WIDTH / 2
        val isRightToLeft = layoutDirection == LayoutDirection.Rtl
        drawLine(
            color = color,
            start = Offset(if (isRightToLeft) 0f else inset, y),
            end = Offset(if (isRightToLeft) size.width - inset else size.width, y),
            strokeWidth = HAIRLINE_WIDTH,
        )
    }

// The highlight of a pressed row: a solid fill behind the row's content.
private data class RowHighlightIndication(
    val color: Color,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = RowHighlightNode(interactionSource, color)
}

private class RowHighlightNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
) : Modifier.Node(),
    DrawModifierNode {
    // MARK: - Properties

    private var isPressed = false

    // MARK: - Object Lifecycle

    override fun onAttach() {
        coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                }

                if (presses.isNotEmpty() == isPressed) return@collect
                isPressed = presses.isNotEmpty()
                invalidateDraw()
            }
        }
    }

    // MARK: - DrawModifierNode Conformance

    override fun ContentDrawScope.draw() {
        if (isPressed) drawRect(color)
        drawContent()
    }
}

// The width, in pixels, of a divider.
private const val HAIRLINE_WIDTH = 1f
