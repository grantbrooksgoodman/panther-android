//
//  ReactionDetailsPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.reactiondetailspageview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.components.GroupedListView
import us.neotechnica.panther.designsystem.modules.foundation.components.HeaderView
import us.neotechnica.panther.designsystem.modules.foundation.extensions.doneButton
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.SquareIconImage
import us.neotechnica.panther.modules.content.user.constants.ReactionDetailsPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ReactionDetailsPageViewFloats
import us.neotechnica.panther.modules.content.user.models.SquareIconViewConfiguration
import us.neotechnica.panther.modules.networking.sources.conversation.models.Reaction
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

private typealias Colors = ReactionDetailsPageViewColors
private typealias Floats = ReactionDetailsPageViewFloats

/**
 * The reaction details page.
 *
 * Lists the reactions on a message, grouped by reaction, showing the
 * names of the participants who reacted with each, each row led by a
 * square icon of the reaction. The list sits below a sheet header with
 * the page title and a trailing Done button.
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
    val listItems = remember(state.messageID, state.viewID) { state.listItems }

    Column(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        HeaderView(
            centerItem =
                HeaderView.CenterItemType.Text(
                    HeaderView.TextAttributes(state.navigationTitle, foregroundColor = colors.navigationBarTitle),
                ),
            rightItem =
                HeaderView.PeripheralButtonType.doneButton(foregroundColor = Colors.doneHeaderItemForeground) {
                    viewModel.send(ReactionDetailsPageReducer.Action.DoneHeaderItemTapped)
                },
            attributes = HeaderView.Attributes(sizeClass = HeaderView.SizeClass.Sheet),
        )

        Column(
            modifier =
                Modifier.padding(
                    start = Floats.groupListViewHorizontalPadding,
                    top = Floats.groupListViewTopPadding,
                    end = Floats.groupListViewHorizontalPadding,
                ),
        ) {
            GroupedListView(listItems)
        }
    }
}

// MARK: - List Row Icon

/**
 * The leading icon of a reaction row: the reaction's square icon,
 * scaled to the list row's image frame.
 *
 * @param style The reaction style the icon represents.
 */
@Composable
internal fun ListRowIcon(style: Reaction.Style) {
    SquareIconImage(
        SquareIconViewConfiguration(
            backgroundColor = style.squareIconBackgroundColor,
            overlay =
                SquareIconViewConfiguration.OverlayConfiguration.Text(
                    style.emojiValue,
                    font = Font.system(FontScale.Custom(Floats.LIST_ROW_ICON_TEXT_FONT_SCALE)),
                ),
        ),
        modifier = Modifier.fillMaxSize(),
    )
}
