//
//  ConversationCellView+SwipeActions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.conversationcellview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewColors
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewFloats
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewStrings

// MARK: - Constants Accessors

private typealias SwipeColors = ConversationCellViewColors
private typealias SwipeFloats = ConversationCellViewFloats
private typealias SwipeStrings = ConversationCellViewStrings

// MARK: - Swipe Action Buttons

@Composable
internal fun SwipeActionButtons(
    state: ConversationCellReducer.State,
    swipeOffset: Float,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        if (swipeOffset > 0f) {
            SwipeActionButton(
                systemName = SwipeStrings.BLOCK_USERS_BUTTON_IMAGE_SYSTEM_NAME,
                tint = SwipeColors.blockUsersButtonImageTint,
                contentDescription = state.blockUsersButtonText,
                onClick = onBlock,
            )
            SwipeActionButton(
                systemName = SwipeStrings.REPORT_USERS_BUTTON_IMAGE_SYSTEM_NAME,
                tint = SwipeColors.reportUsersButtonImageTint,
                contentDescription = state.reportUsersButtonText,
                onClick = onReport,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (swipeOffset < 0f) {
            SwipeActionButton(
                systemName = SwipeStrings.DELETE_CONVERSATION_BUTTON_IMAGE_SYSTEM_NAME,
                tint = SwipeColors.deleteConversationButtonImageTint,
                contentDescription = state.deleteConversationButtonText,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun SwipeActionButton(
    systemName: String,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .width(SwipeFloats.swipeActionButtonWidth)
                .fillMaxHeight()
                .background(tint)
                .clickable(onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
    ) {
        Components.Symbol(systemName, foregroundColor = Color.White)
    }
}
