//
//  MessageContextReactionRow.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.constants.MessageContextReactionRowColors
import us.neotechnica.panther.designsystem.modules.componentkit.constants.MessageContextReactionRowFloats
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.GestureRecognizerState
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Constants Accessors

private typealias ReactionRowColors = MessageContextReactionRowColors
private typealias ReactionRowFloats = MessageContextReactionRowFloats

/**
 * The row of reaction options shown above a lifted message bubble: a
 * capsule as tall as its reaction buttons, so that a selected
 * reaction's circle fills the capsule's height.
 *
 * A touch on the row is tracked as it moves: dragging across the row
 * highlights each reaction in turn, and lifting the touch over a
 * reaction applies it. A highlighted reaction shows its selected
 * background in place of the current selection. Each reaction's hit
 * area extends halfway into the spacing beside it, so the highlight
 * never clears between adjacent reactions.
 *
 * @param controller The controller of the menu the row belongs to.
 */
@Composable
internal fun ReactionRow(controller: ContextMenuController) {
    val colors = LocalPantherColors.current
    val choices = controller.displayed?.reactionChoices.orEmpty()
    val highlightedIndex = controller.highlightedReactionIndex
    var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    controller.reactionHitSlop = with(LocalDensity.current) { ReactionRowFloats.stackViewSpacing.toPx() / 2 }

    val containerColor =
        (if (isSystemInDarkTheme()) ReactionRowColors.reactionButtonDarkBackground else ReactionRowColors.reactionButtonLightBackground)
            .copy(alpha = ReactionRowFloats.VIEW_ALPHA)
    Row(
        Modifier
            .clip(CircleShape)
            .background(containerColor)
            // A drag that begins on a reaction near the screen's edge
            // stays with the row rather than becoming the system back
            // gesture.
            .systemGestureExclusion()
            .onGloballyPositioned { rowCoordinates = it }
            .pointerInput(controller) {
                awaitEachGesture {
                    trackTouch(
                        positionInRoot = { position -> rowCoordinates?.takeIf { it.isAttached }?.localToRoot(position) ?: position },
                        onTouch = controller::trackReactionTouch,
                    )
                }
            },
        horizontalArrangement = Arrangement.spacedBy(ReactionRowFloats.stackViewSpacing),
    ) {
        choices.forEachIndexed { index, choice ->
            val isShownSelected = if (highlightedIndex == null) choice.isSelected else index == highlightedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(ReactionRowFloats.reactionButtonHeight)
                        .onGloballyPositioned { controller.setReactionCoordinates(index, it) }
                        .clip(CircleShape)
                        .background(if (isShownSelected) choice.selectedColor else Color.Transparent)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            onClick {
                                controller.select(choice.onSelect)
                                true
                            }
                        },
            ) {
                Components.Text(
                    choice.emoji,
                    foregroundColor = colors.titleText,
                    font = Font.system(FontScale.Custom(ReactionRowFloats.REACTION_BUTTON_TITLE_LABEL_SYSTEM_FONT_SIZE)),
                )
            }
        }
    }
}

// MARK: - Auxiliary

// Reports a touch's beginning and each movement, then its release –
// or its cancellation, which arrives as a release already consumed –
// with each location in the root coordinate space, consuming every
// event along the way.
private suspend fun AwaitPointerEventScope.trackTouch(
    positionInRoot: (Offset) -> Offset,
    onTouch: (GestureRecognizerState, Offset) -> Unit,
) {
    val down = awaitFirstDown()
    down.consume()
    onTouch(GestureRecognizerState.BEGAN, positionInRoot(down.position))

    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
        val location = positionInRoot(change.position)
        if (event.changes.none { it.pressed }) {
            val state = if (change.isConsumed) GestureRecognizerState.CANCELLED else GestureRecognizerState.ENDED
            event.changes.forEach { it.consume() }
            return onTouch(state, location)
        }

        event.changes.forEach { it.consume() }
        onTouch(GestureRecognizerState.CHANGED, location)
    }
}
