//
//  ChatInfoPageView+ListViews.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.SFSymbol
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.ChatParticipantView
import us.neotechnica.panther.modules.content.user.components.ContactCardPresenter
import us.neotechnica.panther.modules.content.user.components.MediaItemView
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import androidx.compose.material3.Text as Material3Text

// MARK: - Constants Accessors

private typealias ListFloats = ChatInfoPageViewFloats

// MARK: - List Views

// The participants list or the media list, selected by the segmented
// control and faded between on a switch.
@Composable
internal fun ListViews(
    state: ChatInfoPageReducer.State,
    viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>,
    presentContactCard: ContactCardPresenter,
) {
    Crossfade(
        targetState = state.segmentedControlSelectionIndex == 0,
        animationSpec = SystemAnimation.easeIn(ListFloats.listTransitionAnimationDuration),
        label = "ChatInfoListViews",
    ) { showsParticipants ->
        if (showsParticipants) {
            ChatParticipantsList(state, viewModel, presentContactCard)
        } else {
            MediaItemList(state, viewModel)
        }
    }
}

// MARK: - Chat Participants List

@Composable
private fun ChatParticipantsList(
    state: ChatInfoPageReducer.State,
    viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>,
    presentContactCard: ContactCardPresenter,
) {
    InfoCard(modifier = Modifier.animateContentSize(SystemAnimation.spring())) {
        key(state.chatInfoCellViewID) {
            ChatInfoCell(state) { viewModel.send(ChatInfoPageReducer.Action.ChatInfoCellTapped) }
        }

        state.visibleParticipants.forEach { participant ->
            CardDivider()
            ChatParticipantView(
                participant,
                deleteAction =
                    if (state.showsRemoveUserSwipeAction) {
                        { viewModel.send(ChatInfoPageReducer.Action.RemoveUserButtonTapped(participant)) }
                    } else {
                        null
                    },
                userInfoBadgeViewAction =
                    if (state.isDeveloperModeEnabled) {
                        { viewModel.send(ChatInfoPageReducer.Action.UserInfoBadgeTapped(participant.firstUser)) }
                    } else {
                        null
                    },
                onTap = { presentContactCard.onTap(participant.firstUser?.phoneNumber, participant.displayName) },
            )
        }

        if (state.visibleParticipantsIncrement == 1) {
            CardDivider()
            AddContactButton(state, viewModel)
        }
    }
}

// MARK: - Media Item List

@Composable
private fun MediaItemList(
    state: ChatInfoPageReducer.State,
    viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>,
) {
    InfoCard {
        state.mediaItemMetadata.forEachIndexed { index, mediaItemMetadata ->
            if (index > 0) CardDivider()
            MediaItemView(mediaItemMetadata) {
                viewModel.send(ChatInfoPageReducer.Action.MediaItemViewTapped(mediaItemMetadata))
            }
        }
    }
}

// MARK: - Chat Info Cell

@Composable
private fun ChatInfoCell(
    state: ChatInfoPageReducer.State,
    onTap: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap).padding(ListFloats.cardPadding),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Components.Text(
                state.chatInfoCellTitleLabelText,
                foregroundColor = colors.titleText,
                font = Font.systemSemibold(),
                modifier = Modifier.padding(bottom = 1.dp),
            )
            Material3Text(
                state.chatInfoCellSubtitleLabelText,
                color = colors.subtitleText,
                style = Font.system(FontScale.Custom(ListFloats.CHAT_INFO_CELL_SUBTITLE_LABEL_FONT_SIZE)).textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ChatInfoCellImage(state.chatInfoCellImageSystemName)
    }
}

// An SF Symbol draws its own circle; a Material chevron is drawn in one.
@Composable
private fun ChatInfoCellImage(systemName: String) {
    val colors = LocalPantherColors.current
    if (SFSymbol.resolvesToSFSymbol(systemName)) {
        return Components.Symbol(
            systemName,
            foregroundColor = colors.subtitleText,
            modifier =
                Modifier
                    .padding(start = ListFloats.chevronBoxStartPadding)
                    .size(ListFloats.chevronCircleSymbolSize),
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .padding(start = ListFloats.chevronBoxStartPadding)
                .size(ListFloats.chevronBoxSize)
                .clip(CircleShape)
                .border(ListFloats.chevronBoxBorderWidth, colors.subtitleText, CircleShape),
    ) {
        Components.Symbol(
            systemName,
            foregroundColor = colors.subtitleText,
            modifier = Modifier.size(ListFloats.chevronGlyphSize),
        )
    }
}

// MARK: - Card Scaffolding

@Composable
internal fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalPantherColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = ListFloats.cardHorizontalMargin, vertical = ListFloats.cardVerticalMargin)
                .clip(RoundedCornerShape(ListFloats.cardCornerRadius))
                .background(colors.groupedRowBackground),
    ) {
        content()
    }
}

@Composable
private fun CardDivider() {
    val colors = LocalPantherColors.current
    HorizontalDivider(
        color = colors.groupedContentBackground,
        modifier = Modifier.padding(start = ListFloats.cardHorizontalPadding),
    )
}
