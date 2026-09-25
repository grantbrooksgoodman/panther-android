//
//  MessageContextMenu.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import kotlin.math.roundToInt

/**
 * A presented context menu: the lifted message bubble and its action
 * menu.
 */
internal data class ActiveContextMenu(
    val anchorBounds: Rect,
    val alignment: ContextMenuAlignment,
    val actions: List<ContextMenuAction>,
    val reactionChoices: List<ReactionChoice>,
    val liftScale: Float,
    val content: @Composable () -> Unit,
)

/** Controls presentation of a message context menu within a [ContextMenuHost]. */
class ContextMenuController internal constructor() {
    internal var active by mutableStateOf<ActiveContextMenu?>(null)
        private set

    internal var canBegin: Boolean = true

    internal fun present(item: ActiveContextMenu) {
        active = item
    }

    /** Dismisses the presented context menu, if any. */
    fun dismiss() {
        active = null
    }
}

/** Provides the [ContextMenuController] to descendant [MessageContextMenu]s. */
val LocalContextMenuController = compositionLocalOf<ContextMenuController?> { null }

/**
 * Hosts message context menus in the same composition as [content], so
 * the lifted bubble aligns exactly with its origin over a dimmed,
 * tap-to-dismiss backdrop.
 *
 * @param modifier The modifier for the host.
 * @param canBegin Whether a long press may present a menu; when
 *   `false`, long presses are ignored but double-taps still fire.
 * @param content The hosted content, containing [MessageContextMenu]s.
 */
@Composable
fun ContextMenuHost(
    modifier: Modifier = Modifier,
    canBegin: Boolean = true,
    content: @Composable () -> Unit,
) {
    val controller = remember { ContextMenuController() }
    controller.canBegin = canBegin

    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalContextMenuController provides controller) {
            content()
        }

        controller.active?.let { active ->
            ContextMenuOverlay(active = active, onDismiss = controller::dismiss)
        }
    }
}

/**
 * Wraps a message bubble so a long press lifts it and presents [actions]
 * in a context menu, following the bubble's [alignment], with an optional
 * row of [reactionChoices] above the bubble.
 *
 * @param actions The menu's actions.
 * @param alignment The side the menu anchors to.
 * @param reactionChoices The reaction options shown above the bubble; the
 *   row is omitted when empty.
 * @param liftScale The fraction the lifted copy scales up by; pass `0`
 *   to lift a full-width row without scaling it past the screen edge.
 * @param onTap The action performed on a single tap, or `null` when the
 *   bubble has none. Handling it here (rather than a `clickable` inside
 *   [content]) keeps the tap from consuming the long-press and double-tap.
 * @param modifier The modifier for the wrapper.
 * @param content The message bubble.
 */
@Composable
@Suppress("LongParameterList")
fun MessageContextMenu(
    actions: List<ContextMenuAction>,
    alignment: ContextMenuAlignment,
    reactionChoices: List<ReactionChoice> = emptyList(),
    liftScale: Float = LIFT_SCALE_BONUS,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val controller = LocalContextMenuController.current
    val haptics = LocalHapticFeedback.current
    var anchorBounds by remember { mutableStateOf(Rect.Zero) }

    // Hide the origin bubble while it is the lifted one, so only the
    // overlay's copy is visible (no double image).
    val isLifted =
        controller?.active?.anchorBounds == anchorBounds && controller?.active != null && anchorBounds != Rect.Zero

    Box(
        modifier
            .onGloballyPositioned { coordinates ->
                anchorBounds = Rect(coordinates.positionInRoot(), coordinates.size.toSize())
            }.pointerInput(actions, reactionChoices, controller, onTap) {
                // A non-null onDoubleTap delays single taps, so install one only for a double-tap-default reaction.
                val doubleTapChoice = reactionChoices.firstOrNull { it.isDoubleTapDefault }
                detectTapGestures(
                    onTap = onTap?.let { tap -> { tap() } },
                    onDoubleTap =
                        doubleTapChoice?.let { choice ->
                            {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                choice.onSelect()
                            }
                        },
                    onLongPress = {
                        val hasMenu = actions.isNotEmpty() || reactionChoices.isNotEmpty()
                        if (hasMenu && controller?.canBegin == true && anchorBounds != Rect.Zero) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            controller.present(
                                ActiveContextMenu(anchorBounds, alignment, actions, reactionChoices, liftScale, content),
                            )
                        }
                    },
                )
            }.alpha(if (isLifted) 0f else 1f),
    ) { content() }
}

@Composable
private fun ContextMenuOverlay(
    active: ActiveContextMenu,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    var reactionRowWidthPx by remember { mutableIntStateOf(0) }
    var reactionRowHeightPx by remember { mutableIntStateOf(0) }
    var menuHeightPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        progress.snapTo(0f)
        progress.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
    }

    val bounds = active.anchorBounds
    val originX = if (active.alignment == ContextMenuAlignment.LEADING) 0f else 1f
    val hasReactions = active.reactionChoices.isNotEmpty()
    val hasActions = active.actions.isNotEmpty()

    BoxWithConstraints(
        Modifier
            .background(Color.Black.copy(alpha = SCRIM_ALPHA * progress.value))
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        val gapPx = with(density) { MENU_GAP.toPx() }
        val reactionGapPx = with(density) { REACTION_ROW_GAP.toPx() }
        val edgeMarginPx = with(density) { EDGE_MARGIN.toPx() }
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
                        val x = if (active.alignment == ContextMenuAlignment.LEADING) bounds.left else bounds.right - reactionRowWidthPx
                        val y = bounds.top - reactionRowHeightPx - reactionGapPx + shiftPx
                        IntOffset(x.roundToInt().coerceAtLeast(0), y.roundToInt().coerceAtLeast(0))
                    }.graphicsLayer {
                        alpha = progress.value
                        val scale = MENU_MIN_SCALE + (1f - MENU_MIN_SCALE) * progress.value
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
                    val scale = 1f + active.liftScale * progress.value
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(originX, 0f)
                },
        ) { active.content() }

        // Action menu, below the scaled bubble, aligned to its side.
        if (hasActions) {
            Box(
                Modifier
                    .onGloballyPositioned { menuHeightPx = it.size.height }
                    .offset {
                        val menuWidthPx = with(density) { MENU_WIDTH.toPx() }
                        val x = if (active.alignment == ContextMenuAlignment.LEADING) bounds.left else bounds.right - menuWidthPx
                        val y = bubbleBottomPx + gapPx + shiftPx
                        IntOffset(x.roundToInt().coerceAtLeast(0), y.roundToInt())
                    }.graphicsLayer {
                        alpha = progress.value
                        val scale = MENU_MIN_SCALE + (1f - MENU_MIN_SCALE) * progress.value
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

@Composable
private fun ContextMenuCard(
    actions: List<ContextMenuAction>,
    onSelect: (ContextMenuAction) -> Unit,
) {
    val colors = LocalPantherColors.current

    Column(
        Modifier
            .width(MENU_WIDTH)
            .clip(RoundedCornerShape(MENU_CORNER_RADIUS))
            .background(colors.reactionButtonBackground),
    ) {
        actions.forEachIndexed { index, action ->
            val contentColor = if (action.isDestructive) DESTRUCTIVE_COLOR else colors.titleText
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(MENU_ROW_HEIGHT)
                        .clickable { onSelect(action) }
                        .padding(start = 16.dp, end = 12.dp),
            ) {
                Components.Text(
                    action.title,
                    color = contentColor,
                    font = Font.system,
                    modifier = Modifier.weight(1f),
                )
                Components.Symbol(
                    action.systemImageName,
                    color = contentColor,
                    modifier = Modifier.size(MENU_ICON_SIZE),
                )
            }

            if (index != actions.lastIndex) {
                HorizontalDivider(thickness = MENU_DIVIDER_THICKNESS, color = colors.subtitleText.copy(alpha = 0.25f))
            }
        }
    }
}

private val MENU_WIDTH = 250.dp
private val MENU_GAP = 8.dp
private val EDGE_MARGIN = 12.dp
private val MENU_CORNER_RADIUS = 12.dp
private val MENU_ROW_HEIGHT = 44.dp
private val MENU_ICON_SIZE = 22.dp
private val MENU_DIVIDER_THICKNESS = 0.6.dp
private const val SCRIM_ALPHA = 0.45f
private const val LIFT_SCALE_BONUS = 0.06f
private const val MENU_MIN_SCALE = 0.85f
private val DESTRUCTIVE_COLOR = Color(0xFFFF3B30)
private val REACTION_ROW_GAP = 8.dp

private fun androidx.compose.ui.unit.IntSize.toSize() =
    androidx.compose.ui.geometry
        .Size(width.toFloat(), height.toFloat())
