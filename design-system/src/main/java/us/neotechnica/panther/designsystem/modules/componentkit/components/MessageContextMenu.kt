//
//  MessageContextMenu.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice

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
 * @param liftedBackground An opaque background painted behind the lifted
 *   copy, so a transparent row (such as a list cell) stays readable over
 *   the dimmed backdrop. Pass `null` when the content paints its own
 *   background, such as a message bubble.
 * @param menuLeadingOffset For [ContextMenuAlignment.LEADING], the inset
 *   from the content's leading edge at which the menu's leading text (and
 *   the reaction row's leading edge) is aligned, so a full-width row can
 *   line its menu text up with an inner element such as an avatar rather
 *   than the screen edge.
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
    liftedBackground: Color? = null,
    menuLeadingOffset: Dp = 0.dp,
    alignsMenuCardToLeadingEdge: Boolean = false,
    menuKey: String? = null,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val controller = LocalContextMenuController.current
    val haptics = LocalHapticFeedback.current
    var anchorBounds by remember { mutableStateOf(Rect.Zero) }

    // Register this menu under its key so it can be opened
    // programmatically (such as focusing a message from search).
    if (menuKey != null && controller != null) {
        val currentActions by rememberUpdatedState(actions)
        val currentReactionChoices by rememberUpdatedState(reactionChoices)
        val currentContent by rememberUpdatedState(content)
        DisposableEffect(menuKey, controller) {
            controller.register(menuKey) {
                val hasMenu = currentActions.isNotEmpty() || currentReactionChoices.isNotEmpty()
                if (!hasMenu || anchorBounds == Rect.Zero) {
                    null
                } else {
                    ActiveContextMenu(
                        anchorBounds,
                        alignment,
                        currentActions,
                        currentReactionChoices,
                        liftScale,
                        liftedBackground,
                        menuLeadingOffset,
                        alignsMenuCardToLeadingEdge,
                        currentContent,
                    )
                }
            }
            onDispose { controller.unregister(menuKey) }
        }
    }

    // Hide the origin bubble while it is the lifted one, so only the
    // overlay's copy is visible (no double image).
    val isLifted =
        controller?.active?.anchorBounds == anchorBounds && controller?.active != null && anchorBounds != Rect.Zero

    Column(
        modifier,
        horizontalAlignment = if (alignment == ContextMenuAlignment.LEADING) Alignment.Start else Alignment.End,
    ) {
        // The header (a group message's sender name) shows in the origin cell
        // but is hidden while the bubble is lifted, and is excluded from the
        // measured bounds so the reaction row hugs the bubble rather than
        // clearing the name above it.
        header?.let { Box(Modifier.alpha(if (isLifted) 0f else 1f)) { it() } }

        Box(
            Modifier
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
                                    ActiveContextMenu(
                                        anchorBounds,
                                        alignment,
                                        actions,
                                        reactionChoices,
                                        liftScale,
                                        liftedBackground,
                                        menuLeadingOffset,
                                        alignsMenuCardToLeadingEdge,
                                        content,
                                    ),
                                )
                            }
                        },
                    )
                }.alpha(if (isLifted) 0f else 1f),
        ) { content() }
    }
}
