//
//  MessageContextMenu.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.constants.MessageContextMenuFloats
import us.neotechnica.panther.designsystem.modules.componentkit.extensions.detectMessageTapGestures
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Wraps a message bubble so a long press lifts it and presents
 * [actions] in a context menu, following the bubble's [alignment],
 * with an optional row of [reactionChoices] above the bubble.
 *
 * The long press carries into the menu: without lifting, the user
 * can drag onto an action – highlighting it – and release to
 * select it.
 *
 * @param actions The menu's actions.
 * @param alignment The side the menu anchors to.
 * @param reactionChoices The reaction options shown above the
 *   bubble; the row is omitted when empty.
 * @param liftScale The fraction the lifted copy scales up by;
 *   pass `0` to lift a full-width row without scaling it past the
 *   screen edge.
 * @param liftedBackground An opaque background painted behind the
 *   lifted copy, so a transparent row (such as a list cell) stays
 *   readable over the dimmed backdrop. Pass `null` when the
 *   content paints its own background, such as a message bubble.
 * @param minimumPressDuration How long a press must be held to
 *   present the menu, or `null` for the default: the
 *   220-millisecond message bubble press, or the system's
 *   long-press timeout while
 *   [SystemAnimation.usesAppleAnimations] is `false`.
 * @param menuLeadingOffset For [ContextMenuAlignment.LEADING],
 *   the inset from the content's leading edge at which the menu's
 *   leading text (and the reaction row's leading edge) is
 *   aligned, so a full-width row can line its menu text up with
 *   an inner element such as an avatar rather than the screen
 *   edge.
 * @param onTap The action performed on a single tap, given the
 *   tap's location in the root coordinate space, or `null` when
 *   the bubble has none. Handling it here (rather than a
 *   `clickable` inside [content]) keeps the tap from consuming
 *   the long-press and double-tap.
 * @param modifier The modifier for the wrapper.
 * @param content The message bubble.
 */
@Composable
@Suppress("LongParameterList")
fun MessageContextMenu(
    actions: List<ContextMenuAction>,
    alignment: ContextMenuAlignment,
    reactionChoices: List<ReactionChoice> = emptyList(),
    liftScale: Float = MessageContextMenuFloats.LIFT_SCALE_BONUS,
    liftedBackground: Color? = null,
    minimumPressDuration: Duration? = null,
    menuLeadingOffset: Dp = 0.dp,
    alignsMenuCardToLeadingEdge: Boolean = false,
    menuKey: String? = null,
    onTap: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val controller = LocalContextMenuController.current
    var bubbleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val originID = remember { Any() }
    val pressDuration =
        minimumPressDuration ?: if (SystemAnimation.usesAppleAnimations) {
            MessageContextMenuFloats.GESTURE_PRESS_DURATION
        } else {
            LocalViewConfiguration.current.longPressTimeoutMillis.milliseconds
        }

    // The gesture handler reads these through snapshot state, so its
    // pointer-input coroutine keys on the gesture's stable shape (see
    // below) rather than restarting on every recomposition. A restart
    // discards an in-progress long press; a bubble that recomposes
    // mid-press – an incoming receipt, a speech highlight, audio
    // progress – keeps its context menu.
    val currentActions by rememberUpdatedState(actions)
    val currentContent by rememberUpdatedState(content)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentReactionChoices by rememberUpdatedState(reactionChoices)

    // The menu as it stands, or `null` when the bubble has neither
    // actions nor reactions or has not yet been laid out.
    fun activeContextMenu(): ActiveContextMenu? {
        val hasMenu = currentActions.isNotEmpty() || currentReactionChoices.isNotEmpty()
        val liftedBounds = bubbleCoordinates?.let { controller?.boundsInHost(it) }
        if (!hasMenu || liftedBounds == null) return null
        return ActiveContextMenu(
            originID,
            liftedBounds,
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

    // The position of the bubble's top-leading corner in the root, from
    // which a pointer position within the bubble is measured.
    fun bubblePositionInRoot(): Offset = bubbleCoordinates?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero

    // Register this menu under its key so it can be opened
    // programmatically (such as focusing a message from search).
    if (menuKey != null && controller != null) {
        DisposableEffect(menuKey, controller) {
            controller.register(menuKey, ::activeContextMenu)
            onDispose { controller.unregister(menuKey) }
        }
    }

    // Hide the origin bubble while it is the lifted one – until its copy
    // finishes animating back after dismissal – so only the overlay's copy
    // is visible (no double image).
    val isLifted = controller?.displayed?.originID == originID

    Column(
        modifier,
        horizontalAlignment = if (alignment == ContextMenuAlignment.LEADING) Alignment.Start else Alignment.End,
    ) {
        // The header (a group message's sender name) shows in the origin cell
        // but is hidden while the bubble is lifted, and is excluded from the
        // measured bounds so the reaction row hugs the bubble rather than
        // clearing the name above it.
        header?.let { Box(Modifier.alpha(if (isLifted) 0f else 1f)) { it() } }

        // Key the detector on the gesture's stable shape, not on the
        // per-recomposition identities of the action and reaction lists:
        // whether a tap is handled, and whether a double tap applies a
        // reaction. Both hold steady across a bubble's lifetime, so the
        // detector persists and its callbacks read the latest values
        // through the snapshot state declared above.
        val handlesTap = onTap != null
        val hasDoubleTapDefault = reactionChoices.any { it.isDoubleTapDefault }

        Box(
            Modifier
                .onGloballyPositioned { bubbleCoordinates = it }
                .pointerInput(controller, handlesTap, hasDoubleTapDefault, pressDuration) {
                    detectMessageTapGestures(
                        minimumPressDuration = pressDuration,
                        onTap =
                            if (handlesTap) {
                                { position -> currentOnTap?.invoke(bubblePositionInRoot() + position) }
                            } else {
                                null
                            },
                        // A non-null onDoubleTap delays single taps, so install one only for a double-tap-default reaction.
                        onDoubleTap =
                            if (hasDoubleTapDefault) {
                                {
                                    // The choice's own handler supplies any feedback.
                                    val choice = currentReactionChoices.firstOrNull { it.isDoubleTapDefault }
                                    (choice?.onDoubleTap ?: choice?.onSelect)?.invoke()
                                }
                            } else {
                                null
                            },
                        // The press carries into the menu it presents: releasing
                        // it over a menu element selects that element.
                        onLongPress = { state, position ->
                            controller?.beginInteraction(
                                state,
                                location = bubblePositionInRoot() + position,
                                dragThreshold = MessageContextMenuFloats.INTERACTION_DRAG_THRESHOLD.toPx(),
                                menu = ::activeContextMenu,
                            )
                        },
                    )
                }.alpha(if (isLifted) 0f else 1f),
        ) { content() }
    }
}
