//
//  ChatParticipantView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import us.neotechnica.panther.subsystem.modules.foundation.services.Build

/**
 * A participant row for the chat info page.
 *
 * Shows the participant's avatar, name, and a user-info badge
 * (tappable only in developer mode), and opens the participant's
 * contact card on a row tap. A non-`null` [onRemove] enables a trailing
 * remove swipe; a non-`null` [onLongPress] handles a long press.
 *
 * @param participant The participant to display.
 * @param onTap The action performed on a row tap.
 * @param onUserInfoBadgeTapped The action performed when the user-info
 *   badge is tapped in developer mode.
 * @param onRemove The action performed when the row is swiped to remove
 *   the participant, or `null` to disable the remove swipe.
 * @param onLongPress The action performed on a long press, or `null` to
 *   disable it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatParticipantView(
    participant: ChatParticipant,
    onTap: () -> Unit,
    onUserInfoBadgeTapped: () -> Unit,
    onRemove: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
) {
    val remove = onRemove
    if (remove == null) {
        RowContent(participant, onTap, onUserInfoBadgeTapped, onLongPress)
        return
    }

    val dismissState =
        rememberSwipeToDismissBoxState(
            // Never actually dismiss: the swipe triggers the removal flow
            // (which presents its own confirmation) and the row snaps back.
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) remove()
                false
            },
        )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = { RemoveSwipeBackground() },
    ) {
        RowContent(participant, onTap, onUserInfoBadgeTapped, onLongPress)
    }
}

// MARK: - Row Content

@Composable
private fun RowContent(
    participant: ChatParticipant,
    onTap: () -> Unit,
    onUserInfoBadgeTapped: () -> Unit,
    onLongPress: (() -> Unit)?,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.groupedRowBackground)
                .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                .padding(horizontal = ChatInfoPageViewFloats.cardHorizontalPadding, vertical = ChatInfoPageViewFloats.rowVerticalPadding),
    ) {
        AvatarImageView(
            null,
            size = DpSize(ChatInfoPageViewFloats.rowAvatarSize, ChatInfoPageViewFloats.rowAvatarSize),
        )
        Components.Text(
            participant.displayName,
            foregroundColor = colors.titleText,
            font = Font.systemSemibold(),
            modifier = Modifier.padding(start = ChatInfoPageViewFloats.rowTextStartPadding),
        )
        participant.firstUser?.let { user ->
            Box(modifier = Modifier.padding(start = ChatInfoPageViewFloats.languageBadgeStartPadding)) {
                UserInfoBadgeView(
                    user = user,
                    action = if (Build.isDeveloperModeEnabled) onUserInfoBadgeTapped else null,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Components.Symbol(
            "chevron.forward",
            foregroundColor = colors.subtitleText,
            modifier = Modifier.size(ChatInfoPageViewFloats.chevronGlyphSize),
        )
    }
}

@Composable
private fun RemoveSwipeBackground() {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier =
            Modifier
                .fillMaxSize()
                .background(ChatInfoPageViewColors.destructive)
                .padding(horizontal = ChatInfoPageViewFloats.cardHorizontalPadding),
    ) {
        Components.Symbol(
            "trash",
            foregroundColor = Color.White,
            modifier = Modifier.size(ChatInfoPageViewFloats.chevronGlyphSize),
        )
    }
}
