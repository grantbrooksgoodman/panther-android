//
//  ChatParticipantView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.DisclosureIndicator
import us.neotechnica.panther.designsystem.modules.componentkit.components.SwipeActionsCoordinationEffect
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.modifiers.swipeActionsDraggable
import us.neotechnica.panther.designsystem.modules.foundation.constants.StackFloats
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatParticipantViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatParticipantViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatParticipantViewStrings
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import kotlin.math.roundToInt

// MARK: - Constants Accessors

private typealias ParticipantColors = ChatParticipantViewColors
private typealias ParticipantFloats = ChatParticipantViewFloats
private typealias ParticipantStrings = ChatParticipantViewStrings

// MARK: - Types

private enum class SwipeAnchor {
    SETTLED,
    TRAILING,
}

/**
 * A row describing a participant in a conversation.
 *
 * Use [ChatParticipantView] to display a participant's avatar, display
 * name, and user info badge, with an optional swipe-to-delete action.
 * A trailing swipe reveals a held delete button; the row never
 * deletes on a full swipe.
 *
 * @param participant The participant the row describes.
 * @param deleteAction The action the revealed delete button performs.
 *   Pass `null` to omit the swipe action.
 * @param userInfoBadgeViewAction The action performed when the user
 *   taps the user info badge. Pass `null` for no action.
 * @param onTap The action performed when the user taps the row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatParticipantView(
    participant: ChatParticipant,
    deleteAction: (() -> Unit)?,
    userInfoBadgeViewAction: (() -> Unit)?,
    onTap: () -> Unit,
) {
    if (deleteAction == null) {
        RowContent(participant, userInfoBadgeViewAction, onTap)
        return
    }

    val colors = LocalPantherColors.current
    val deleteButtonWidth = with(LocalDensity.current) { ParticipantFloats.deleteButtonWidth.toPx() }
    val draggableState =
        remember {
            AnchoredDraggableState(
                initialValue = SwipeAnchor.SETTLED,
                anchors =
                    DraggableAnchors {
                        SwipeAnchor.SETTLED at 0f
                        SwipeAnchor.TRAILING at -deleteButtonWidth
                    },
            )
        }
    val swipeOffset = draggableState.offset.takeIf { !it.isNaN() } ?: 0f
    val rowBounds = remember { mutableStateOf(Rect.Zero) }
    val scope = rememberCoroutineScope()

    SwipeActionsCoordinationEffect(draggableState, SwipeAnchor.SETTLED, rowBounds)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .onGloballyPositioned { rowBounds.value = it.boundsInRoot() },
    ) {
        DeleteButton(
            onClick = {
                scope.launch { draggableState.animateTo(SwipeAnchor.SETTLED) }
                deleteAction()
            },
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )

        Box(
            modifier =
                Modifier
                    .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                    .background(colors.groupedRowBackground)
                    .swipeActionsDraggable(draggableState, enabled = true),
        ) {
            RowContent(participant, userInfoBadgeViewAction, onTap)
        }
    }
}

// MARK: - Row Content

@Composable
private fun RowContent(
    participant: ChatParticipant,
    userInfoBadgeViewAction: (() -> Unit)?,
    onTap: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StackFloats.defaultSpacing),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(
                    horizontal = ChatInfoPageViewFloats.cardHorizontalPadding,
                    vertical = ChatInfoPageViewFloats.rowVerticalPadding,
                ),
    ) {
        AvatarImageView(
            remember(participant.contactPair) { participant.thumbnailImage?.asImageBitmap() },
            size = DpSize(ParticipantFloats.avatarImageViewSizeWidth, ParticipantFloats.avatarImageViewSizeHeight),
            modifier = Modifier.padding(end = ParticipantFloats.avatarImageViewTrailingPadding),
        )
        Components.Text(
            participant.displayName,
            foregroundColor = colors.titleText,
            font = Font.systemSemibold(),
        )
        participant.firstUser?.let { user ->
            UserInfoBadgeView(user = user, action = userInfoBadgeViewAction)
        }
        Spacer(modifier = Modifier.weight(1f))
        DisclosureIndicator(foregroundColor = colors.subtitleText)
    }
}

// MARK: - Delete Button

@Composable
private fun DeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .width(ParticipantFloats.deleteButtonWidth)
                .background(ParticipantColors.deleteButtonTint)
                .clickable(onClick = onClick),
    ) {
        Components.Symbol(ParticipantStrings.DELETE_BUTTON_IMAGE_SYSTEM_NAME, foregroundColor = Color.White)
    }
}
