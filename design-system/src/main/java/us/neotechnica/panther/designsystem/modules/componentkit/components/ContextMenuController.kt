//
//  ContextMenuController.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import us.neotechnica.panther.designsystem.R
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.GestureRecognizerState
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice
import us.neotechnica.panther.designsystem.modules.foundation.services.SoundPlayer

/**
 * A presented context menu: the lifted message bubble and its action
 * menu.
 *
 * The anchor bounds are in the host's coordinate space, where the
 * overlay draws the lifted bubble, so a transform the host shares with
 * the bubble – the page sliding in as it is pushed – can't move the
 * lifted bubble away from its origin.
 */
internal data class ActiveContextMenu(
    val originID: Any,
    val anchorBounds: Rect,
    val alignment: ContextMenuAlignment,
    val actions: List<ContextMenuAction>,
    val reactionChoices: List<ReactionChoice>,
    val liftScale: Float,
    val liftedBackground: Color?,
    val menuLeadingOffset: Dp,
    val alignsMenuCardToLeadingEdge: Boolean,
    val content: @Composable () -> Unit,
)

/**
 * Controls presentation of a message context menu within a
 * [ContextMenuHost].
 *
 * A long press that presents a menu carries into it: dragging the
 * held pointer over the menu's elements or reactions highlights the
 * one beneath it, and lifting the pointer over an element selects
 * it, or over a reaction applies it. A touch that begins on the
 * reaction row once the menu is presented works the same way across
 * the reactions.
 */
class ContextMenuController internal constructor() {
    // MARK: - Properties

    internal var active by mutableStateOf<ActiveContextMenu?>(null)
        private set

    // The last presented menu, kept while it animates away after dismissal.
    internal var lastPresented by mutableStateOf<ActiveContextMenu?>(null)

    internal var canBegin: Boolean = true
    internal var hapticFeedback: HapticFeedback? = null

    // How far each reaction's hit area extends to either side, in
    // pixels: halfway into the spacing beside it.
    internal var reactionHitSlop: Float = 0f

    // The host's layout coordinates, against which lifted bubbles are
    // measured.
    internal var hostCoordinates: LayoutCoordinates? = null

    /** The index of the menu element under a held long press, or `null`. */
    internal var highlightedElementIndex by mutableStateOf<Int?>(null)
        private set

    /** The index of the reaction under a tracked touch, or `null`. */
    internal var highlightedReactionIndex by mutableStateOf<Int?>(null)
        private set

    private val elementCoordinates = mutableMapOf<Int, LayoutCoordinates>()
    private val reactionCoordinates = mutableMapOf<Int, LayoutCoordinates>()
    private val registry = mutableMapOf<String, () -> ActiveContextMenu?>()

    // The location, in the root coordinate space, at which the held long
    // press that presented the current menu began.
    private var interactionOrigin: Offset? = null

    // MARK: - Computed Properties

    /**
     * Whether a context menu is on screen, including while it animates
     * away after dismissal.
     */
    val isDisplayed: Boolean
        get() = displayed != null

    /** Whether a context menu is currently presented. */
    val isPresenting: Boolean
        get() = active != null

    // The menu on screen: the presented menu, or the dismissed menu while
    // it animates away.
    internal val displayed: ActiveContextMenu?
        get() = active ?: lastPresented

    // MARK: - Host Bounds

    /**
     * Returns the bounds of the given layout in the host's coordinate
     * space, or `null` when either is detached.
     *
     * @param coordinates The layout coordinates of a bubble within the
     *   host.
     */
    internal fun boundsInHost(coordinates: LayoutCoordinates): Rect? {
        val hostCoordinates = hostCoordinates?.takeIf { it.isAttached } ?: return null
        if (!coordinates.isAttached) return null
        return Rect(hostCoordinates.localPositionOf(coordinates, Offset.Zero), coordinates.size.toSize())
    }

    // MARK: - Presentation

    internal fun present(item: ActiveContextMenu) {
        resetElements()
        lastPresented = item
        active = item
    }

    /**
     * Presents the menu registered under the given key, if one is
     * registered and can currently be built.
     *
     * @param key The key of the menu to present.
     *
     * @return `true` if a menu was presented; otherwise, `false`.
     */
    fun present(key: String): Boolean {
        val item = registry[key]?.invoke() ?: return false
        present(item)
        return true
    }

    internal fun register(
        key: String,
        factory: () -> ActiveContextMenu?,
    ) {
        registry[key] = factory
    }

    internal fun unregister(key: String) {
        registry.remove(key)
    }

    // Dismisses the presented menu and performs the given selection – an
    // action or a reaction – unless the menu is already animating away.
    internal fun select(selection: () -> Unit) {
        if (!isPresenting) return
        dismiss()
        selection()
    }

    /** Dismisses the presented context menu, if any. */
    fun dismiss() {
        interactionOrigin = null
        resetElements()
        active = null
    }

    // MARK: - Interaction

    /**
     * Handles a change in the state of a long press on a message
     * bubble.
     *
     * When the press begins, the bubble's menu is presented. As the
     * held pointer moves, the menu element or reaction beneath it is
     * highlighted. When the pointer lifts over an element or reaction,
     * having travelled more than [dragThreshold] from where the press
     * began, the menu dismisses and the element's action runs or the
     * reaction applies; lifting anywhere else leaves the menu
     * presented.
     *
     * @param state The long press's new state.
     * @param location The pointer's location in the root coordinate
     *   space.
     * @param dragThreshold The distance, in pixels, the pointer must
     *   travel from where the press began for its release to select
     *   an element.
     * @param menu Returns the menu to present as the press begins, or
     *   `null` when the bubble has none.
     */
    internal fun beginInteraction(
        state: GestureRecognizerState,
        location: Offset,
        dragThreshold: Float,
        menu: () -> ActiveContextMenu?,
    ) {
        when (state) {
            GestureRecognizerState.BEGAN -> {
                if (!canBegin || active != null) return
                val item = menu() ?: return
                SoundPlayer.play(R.raw.selection)
                hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
                present(item)
                interactionOrigin = location
            }

            GestureRecognizerState.CHANGED -> {
                if (interactionOrigin == null) return
                highlightElement(location)
                highlightReaction(location)
            }

            GestureRecognizerState.ENDED -> {
                val origin = interactionOrigin ?: return
                interactionOrigin = null
                if ((location - origin).getDistance() <= dragThreshold) return unhighlightAllElements()

                element(location)?.let { action ->
                    unhighlightAllElements()
                    return select(action.onSelect)
                }

                if (!selectReaction(location)) unhighlightAllElements()
            }

            GestureRecognizerState.CANCELLED -> {
                interactionOrigin = null
                unhighlightAllElements()
            }
        }
    }

    // MARK: - Menu Elements

    /**
     * Records the layout coordinates of the presented menu's element at
     * the given index, for hit-testing a held long press.
     *
     * @param index The element's index among the menu's actions.
     * @param coordinates The element's layout coordinates.
     */
    internal fun setElementCoordinates(
        index: Int,
        coordinates: LayoutCoordinates,
    ) {
        elementCoordinates[index] = coordinates
    }

    // Returns the presented menu's action whose element contains the
    // given location, or `null`.
    private fun element(location: Offset): ContextMenuAction? = elementIndex(location)?.let { active?.actions?.getOrNull(it) }

    private fun elementIndex(location: Offset): Int? =
        elementCoordinates.entries
            .firstOrNull { (_, coordinates) -> coordinates.isAttached && coordinates.boundsInRoot().contains(location) }
            ?.key

    // Highlights the element at the given location, if any, playing a
    // selection haptic as the highlight moves from one element directly
    // to another.
    private fun highlightElement(location: Offset) {
        val newHighlightedElementIndex = elementIndex(location)
        if (newHighlightedElementIndex == highlightedElementIndex) return

        if (highlightedElementIndex != null && newHighlightedElementIndex != null) {
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

        highlightedElementIndex = newHighlightedElementIndex
    }

    private fun resetElements() {
        elementCoordinates.clear()
        reactionCoordinates.clear()
        unhighlightAllElements()
    }

    private fun unhighlightAllElements() {
        highlightedElementIndex = null
        highlightedReactionIndex = null
    }

    // MARK: - Reactions

    /**
     * Records the layout coordinates of the presented menu's reaction
     * at the given index, for hit-testing a tracked touch.
     *
     * @param index The reaction's index among the menu's reactions.
     * @param coordinates The reaction's layout coordinates.
     */
    internal fun setReactionCoordinates(
        index: Int,
        coordinates: LayoutCoordinates,
    ) {
        reactionCoordinates[index] = coordinates
    }

    /**
     * Handles a change in the state of a touch that begins on the
     * reaction row once the menu is presented.
     *
     * As the touch moves, the reaction beneath it is highlighted.
     * When the touch lifts over a reaction, the menu dismisses and the
     * reaction applies; lifting anywhere else leaves the menu
     * presented.
     *
     * @param state The touch's new state.
     * @param location The touch's location in the root coordinate
     *   space.
     */
    internal fun trackReactionTouch(
        state: GestureRecognizerState,
        location: Offset,
    ) {
        when (state) {
            GestureRecognizerState.BEGAN,
            GestureRecognizerState.CHANGED,
            -> highlightReaction(location)

            GestureRecognizerState.ENDED -> if (!selectReaction(location)) unhighlightAllElements()

            GestureRecognizerState.CANCELLED -> unhighlightAllElements()
        }
    }

    // Highlights the reaction at the given location, if any, in place of
    // the current selection, playing a selection haptic as the highlight
    // moves from one reaction directly to another.
    private fun highlightReaction(location: Offset) {
        val newHighlightedReactionIndex = reactionIndex(location)
        if (newHighlightedReactionIndex == highlightedReactionIndex) return

        if (highlightedReactionIndex != null && newHighlightedReactionIndex != null) {
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

        highlightedReactionIndex = newHighlightedReactionIndex
    }

    // Returns the index of the presented menu's reaction whose hit area
    // contains the given location, or `null`.
    private fun reactionIndex(location: Offset): Int? =
        reactionCoordinates.entries
            .firstOrNull { (_, coordinates) ->
                if (!coordinates.isAttached) return@firstOrNull false
                val bounds = coordinates.boundsInRoot()
                Rect(
                    left = bounds.left - reactionHitSlop,
                    top = bounds.top,
                    right = bounds.right + reactionHitSlop,
                    bottom = bounds.bottom,
                ).contains(location)
            }?.key

    // Applies the reaction at the given location, if any, keeping it
    // highlighted while the menu animates away. Returns whether a
    // reaction was applied.
    private fun selectReaction(location: Offset): Boolean {
        val index = reactionIndex(location) ?: return false
        val choice = active?.reactionChoices?.getOrNull(index) ?: return false
        select(choice.onSelect)
        highlightedReactionIndex = index
        return true
    }
}
