//
//  ContextMenuOverlay.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import kotlin.math.roundToInt

@Composable
internal fun ContextMenuOverlay(
    active: ActiveContextMenu,
    progress: Float,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    var reactionRowWidthPx by remember { mutableIntStateOf(0) }
    var reactionRowHeightPx by remember { mutableIntStateOf(0) }
    var menuHeightPx by remember { mutableIntStateOf(0) }

    val bounds = active.anchorBounds
    val originX = if (active.alignment == ContextMenuAlignment.LEADING) 0f else 1f
    val hasReactions = active.reactionChoices.isNotEmpty()
    val hasActions = active.actions.isNotEmpty()

    // A blur separates the content on API 31+, so a lighter tint suffices;
    // pre-31 keeps the full scrim.
    val scrimAlpha = contextMenuScrimAlpha()
    BoxWithConstraints(
        Modifier
            .background(Color.Black.copy(alpha = scrimAlpha * progress))
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        val gapPx = with(density) { MENU_GAP.toPx() }
        val reactionGapPx = with(density) { REACTION_ROW_GAP.toPx() }
        val edgeMarginPx = with(density) { EDGE_MARGIN.toPx() }
        val menuLeadingOffsetPx = with(density) { active.menuLeadingOffset.toPx() }
        val topLimitPx = WindowInsets.systemBars.getTop(density) + edgeMarginPx
        val bottomLimitPx = constraints.maxHeight - WindowInsets.systemBars.getBottom(density) - edgeMarginPx

        // The bubble scales up from its top, so its visual bottom sits below
        // the measured bounds; the menu must clear that, not just `bounds.bottom`.
        val bubbleBottomPx = bounds.bottom + bounds.height * active.liftScale

        // Shift the whole stack vertically so the reaction row clears the top
        // inset and the menu clears the bottom inset, keeping every item on
        // screen when the bubble is near an edge.
        val shiftPx =
            run {
                val stackTopPx = if (hasReactions) bounds.top - reactionGapPx - reactionRowHeightPx else bounds.top
                val stackBottomPx = if (hasActions) bubbleBottomPx + gapPx + menuHeightPx else bubbleBottomPx
                var shift = (bottomLimitPx - stackBottomPx).coerceAtMost(0f)
                if (stackTopPx + shift < topLimitPx) shift = topLimitPx - stackTopPx
                shift
            }

        // Reaction row, above the bubble, aligned to its side.
        if (hasReactions) {
            Box(
                Modifier
                    .onGloballyPositioned {
                        reactionRowWidthPx = it.size.width
                        reactionRowHeightPx = it.size.height
                    }.offset {
                        val leadingX = bounds.left + menuLeadingOffsetPx
                        val x = if (active.alignment == ContextMenuAlignment.LEADING) leadingX else bounds.right - reactionRowWidthPx
                        val y = bounds.top - reactionRowHeightPx - reactionGapPx + shiftPx
                        IntOffset(x.roundToInt().coerceAtLeast(0), y.roundToInt().coerceAtLeast(0))
                    }.graphicsLayer {
                        alpha = progress
                        val scale = MENU_MIN_SCALE + (1f - MENU_MIN_SCALE) * progress
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(originX, 1f)
                    },
            ) {
                ReactionRow(active.reactionChoices) { choice ->
                    onDismiss()
                    choice.onSelect()
                }
            }
        }

        // Lifted bubble copy, anchored over its (possibly shifted) origin.
        Box(
            Modifier
                .offset { IntOffset(bounds.left.roundToInt(), (bounds.top + shiftPx).roundToInt()) }
                .graphicsLayer {
                    val scale = 1f + active.liftScale * progress
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(originX, 0f)
                }.then(active.liftedBackground?.let { Modifier.background(it) } ?: Modifier),
        ) { active.content() }

        // Action menu, below the scaled bubble, aligned to its side.
        if (hasActions) {
            Box(
                Modifier
                    .onGloballyPositioned { menuHeightPx = it.size.height }
                    .offset {
                        val menuWidthPx = with(density) { MENU_WIDTH.toPx() }
                        // Pull the card left by its text inset so the text lands at the offset,
                        // unless the caller wants the card's leading edge itself at the offset
                        // (aligning the menu with the message bubble's leading edge).
                        val textInsetPx = if (active.alignsMenuCardToLeadingEdge) 0f else with(density) { MENU_ROW_START_PADDING.toPx() }
                        val leadingX = bounds.left + menuLeadingOffsetPx - textInsetPx
                        val x = if (active.alignment == ContextMenuAlignment.LEADING) leadingX else bounds.right - menuWidthPx
                        val y = bubbleBottomPx + gapPx + shiftPx
                        IntOffset(x.roundToInt().coerceAtLeast(0), y.roundToInt())
                    }.graphicsLayer {
                        alpha = progress
                        val scale = MENU_MIN_SCALE + (1f - MENU_MIN_SCALE) * progress
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(originX, 0f)
                    },
            ) {
                ContextMenuCard(active.actions) { action ->
                    onDismiss()
                    action.onSelect()
                }
            }
        }
    }
}
