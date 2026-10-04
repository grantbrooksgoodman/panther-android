//
//  ReactionDetailsPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/03/2025.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.reactiondetailspageview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.SquareIconView
import us.neotechnica.panther.modules.content.user.constants.ReactionDetailsPageViewFloats
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.models.SquareIconViewConfiguration
import us.neotechnica.panther.modules.localization.models.LocalizationSource
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.extensions.messages
import us.neotechnica.panther.modules.session.entity.extensions.reactions
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

private typealias Floats = ReactionDetailsPageViewFloats

/**
 * The reaction details page.
 *
 * Lists the reactions on a message, grouped by reaction, showing the
 * names of the participants who reacted with each, each row led by a
 * square icon of the reaction.
 *
 * @param messageID The identifier of the message whose reactions are
 *   shown.
 * @param modifier The modifier for this view.
 */
@Composable
fun ReactionDetailsPageView(
    messageID: String,
    modifier: Modifier = Modifier,
) {
    val viewModel =
        remember(messageID) {
            ViewModel(ReactionDetailsPageReducer.State(messageID), ReactionDetailsPageReducer())
                .observing(DependencyValues.current.sharedEvents.currentConversationMetadataChanged.events) {
                    ReactionDetailsPageReducer.Action.UpdateViewID
                }
        }
    LaunchedEffect(Unit) { viewModel.send(ReactionDetailsPageReducer.Action.ViewAppeared) }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.send(ReactionDetailsPageReducer.Action.ViewDisappeared)
            viewModel.close()
        }
    }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val groups = remember(state.messageID, state.viewID) { reactionGroups(state.messageID) }

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Floats.headerHorizontalPadding, vertical = Floats.headerVerticalPadding),
            ) {
                CircleChipButton(
                    systemName = "checkmark",
                    contentDescription = LocalizedStringKey.Done.localized(LocalizationSource.SUBSYSTEM),
                    onClick = { viewModel.send(ReactionDetailsPageReducer.Action.DoneHeaderItemTapped) },
                    tint = colors.titleText,
                )
            }

            Components.Text(
                state.navigationTitle,
                color = colors.titleText,
                font = Font.systemBold(FontScale.Large),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = Floats.titleBottomPadding),
            )

            ReactionGroupList(groups)
        }
    }
}

// MARK: - Reaction Group List

@Composable
private fun ReactionGroupList(groups: List<ReactionGroup>) {
    val colors = LocalPantherColors.current
    Column(
        modifier =
            Modifier
                .padding(horizontal = Floats.listHorizontalPadding)
                .clip(RoundedCornerShape(Floats.listCornerRadius))
                .background(colors.groupedRowBackground),
    ) {
        groups.forEachIndexed { index, group ->
            if (index > 0) HorizontalDivider(color = colors.groupedContentBackground)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Floats.rowHorizontalPadding, vertical = Floats.rowVerticalPadding),
            ) {
                SquareIconView(
                    configuration =
                        SquareIconViewConfiguration(
                            size = DpSize(Floats.rowIconSize, Floats.rowIconSize),
                            backgroundColor = group.style.squareIconBackgroundColor,
                            overlay =
                                SquareIconViewConfiguration.OverlayConfiguration.Text(
                                    group.style.emojiValue,
                                    font = Font.system(FontScale.Custom(Floats.ICON_FONT_SIZE)),
                                ),
                        ),
                    modifier = Modifier.padding(end = Floats.rowIconSpacing),
                )
                Components.Text(group.names, color = colors.titleText)
            }
        }
    }
}

// MARK: - Auxiliary

private data class ReactionGroup(
    val style: Reaction.Style,
    val names: String,
)

private fun reactionGroups(messageID: String): List<ReactionGroup> {
    val currentUserID = User.currentUserID ?: return emptyList()
    val reactions =
        ConversationSessionService.currentConversation
            ?.messages
            ?.firstOrNull { it.id == messageID }
            ?.reactions ?: return emptyList()

    val nameByUserID =
        (SessionStore.users.values.toList() + listOfNotNull(UserSessionService.currentUser))
            .distinctBy { it.id }
            .associate { it.id to reactionDisplayName(it, currentUserID) }

    return reactions
        .groupBy { it.style }
        .mapNotNull { (style, styleReactions) ->
            val names =
                styleReactions
                    .mapNotNull { nameByUserID[it.userID] }
                    .sortedBy { it.lowercase() }
                    .joinToString("\n")
            if (names.isEmpty()) null else ReactionGroup(style, names)
        }.sortedBy { it.style.orderValue }
}

private fun reactionDisplayName(
    user: User,
    currentUserID: String,
): String = if (user.id == currentUserID) LocalizedStringKey.You.localized() else user.displayName
