//
//  ChatInfoPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.bundle.chatInfoPageLoadingStateUpdated
import us.neotechnica.panther.bundle.currentConversationActivityChanged
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.ContextMenuHost
import us.neotechnica.panther.designsystem.modules.componentkit.components.DisclosureIndicator
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.LocalSwipeActionsCoordinator
import us.neotechnica.panther.designsystem.modules.componentkit.models.SwipeActionsCoordinator
import us.neotechnica.panther.designsystem.modules.foundation.components.HeaderView
import us.neotechnica.panther.designsystem.modules.foundation.components.ListRowView
import us.neotechnica.panther.designsystem.modules.foundation.constants.StackFloats
import us.neotechnica.panther.designsystem.modules.foundation.extensions.doneButton
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.models.MediaFile
import us.neotechnica.panther.modules.content.user.components.AvatarImageView
import us.neotechnica.panther.modules.content.user.components.ContactCardPresenter
import us.neotechnica.panther.modules.content.user.components.MediaPreviewOverlay
import us.neotechnica.panther.modules.content.user.components.contentpickerview.RegisterContentPickerLaunchers
import us.neotechnica.panther.modules.content.user.components.rememberContactCardPresenter
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.modules.content.user.dependencies.chatInfoPageViewService
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

private typealias Colors = ChatInfoPageViewColors
private typealias Floats = ChatInfoPageViewFloats

/**
 * The chat info page.
 *
 * For a group: a large avatar, the conversation title, a change name
 * and photo button, a segmented control between an expandable
 * participants card and the shared media card, and a leave row. A
 * participant row opens the participant's contact on tap and reveals a
 * delete button on a trailing swipe. For a one-to-one conversation:
 * the other participant's contact card.
 *
 * @param conversationIDKey The identifier key of the conversation the
 *   page describes.
 * @param modifier The modifier for this view.
 */
@Composable
fun ChatInfoPageView(
    conversationIDKey: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { buildChatInfoPageViewModel(conversationIDKey) }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.send(ChatInfoPageReducer.Action.ViewDisappeared)
            viewModel.close()
        }
    }
    LaunchedEffect(conversationIDKey) {
        viewModel.send(ChatInfoPageReducer.Action.ViewAppeared(conversationIDKey))
    }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val swipeCoordinator = remember { SwipeActionsCoordinator() }

    RegisterContentPickerLaunchers()

    // The page is dismissed only with its header's Done item; the
    // system back gesture is ignored.
    BackHandler {}

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        // The host fills the page so a lifted row's scrim covers the
        // header and the system bars.
        ContextMenuHost(modifier = Modifier.fillMaxSize().then(swipeCoordinator.dismissalModifier())) {
            CompositionLocalProvider(LocalSwipeActionsCoordinator provides swipeCoordinator) {
                StatefulView(state = state.viewState) {
                    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        HeaderView(
                            rightItem =
                                HeaderView.PeripheralButtonType.doneButton(
                                    action = { viewModel.send(ChatInfoPageReducer.Action.DoneHeaderItemTapped) },
                                    foregroundColor = colors.navigationBarButton,
                                ),
                            attributes = HeaderView.Attributes(windowInsets = WindowInsets.statusBars),
                        )
                        key(state.viewID) {
                            ContentView(state, viewModel, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        MediaPreviewHost()
        SheetView(viewModel)
    }
}

// MARK: - Content View

// The conversation's avatar, title, change name and photo button,
// segmented control, lists, and leave row, scrolling beneath the
// header; or, for a one-to-one conversation, the other participant's
// contact card.
@Composable
private fun ContentView(
    state: ChatInfoPageReducer.State,
    viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val presentContactCard = rememberContactCardPresenter()
    val singleChatParticipant = state.singleChatParticipant
    val contactTapModifier =
        singleChatParticipant?.let { participant ->
            Modifier.clickable { presentContactCard.onTap(participant.firstUser?.phoneNumber, participant.displayName) }
        } ?: Modifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StackFloats.defaultSpacing),
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
    ) {
        AvatarImageView(
            remember(state.viewID) { state.avatarImage?.asImageBitmap() },
            badgeCount = if (singleChatParticipant == null) -1 else 0,
            size = DpSize(Floats.avatarImageViewSizeWidth, Floats.avatarImageViewSizeHeight),
            modifier =
                contactTapModifier
                    .padding(bottom = 1.dp)
                    .padding(top = Floats.avatarImageViewTopPadding)
                    .padding(horizontal = Floats.avatarImageViewHorizontalPadding),
        )

        Components.Text(
            state.chatTitleLabelText,
            foregroundColor = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            textAlign = TextAlign.Center,
            modifier =
                contactTapModifier
                    .padding(bottom = 1.dp)
                    .padding(horizontal = Floats.chatTitleLabelHorizontalPadding),
        )

        if (singleChatParticipant != null) {
            SingleContactCard(singleChatParticipant, presentContactCard)
        } else {
            if (state.showsChangeMetadataButton) {
                ChangeMetadataButton(state, viewModel)
            }

            SegmentedControlView(state, viewModel)
            ListViews(state, viewModel, presentContactCard)
            LeaveConversationListRowView(state, viewModel)
        }

        Spacer(modifier = Modifier.padding(bottom = Floats.bottomSpacerPadding))
    }
}

// MARK: - Change Metadata Button

@Composable
private fun ChangeMetadataButton(
    state: ChatInfoPageReducer.State,
    viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>,
) {
    Components.CapsuleButton(
        state.strings.value(ChatInfoPageViewStrings.changeMetadataButtonText),
        font = Font.systemSemibold(FontScale.Custom(Floats.CHANGE_METADATA_BUTTON_LABEL_FONT_SIZE)),
        usesShadow = false,
        isEnabled = state.isChangeMetadataButtonEnabled,
        modifier =
            Modifier
                .padding(bottom = 1.dp)
                .padding(horizontal = Floats.changeMetadataButtonHorizontalPadding),
    ) { viewModel.send(ChatInfoPageReducer.Action.ChangeMetadataButtonTapped) }
}

// MARK: - Leave Conversation List Row View

@Composable
private fun LeaveConversationListRowView(
    state: ChatInfoPageReducer.State,
    viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>,
) {
    Box(modifier = Modifier.padding(horizontal = Floats.leaveConversationListRowViewHorizontalPadding)) {
        ListRowView(
            ListRowView.Configuration(
                ListRowView.Interaction.Button { viewModel.send(ChatInfoPageReducer.Action.LeaveConversationButtonTapped) },
                innerText = state.strings.value(ChatInfoPageViewStrings.leaveConversation),
                innerTextColor = Colors.leaveConversationListRowViewForeground,
                isEnabled = state.isLeaveConversationButtonEnabled,
            ),
        )
    }
}

// MARK: - Single Contact Card

// The sole participant's phone number, opening their contact on tap.
@Composable
private fun SingleContactCard(
    participant: ChatParticipant,
    presentContactCard: ContactCardPresenter,
) {
    val colors = LocalPantherColors.current
    val number = participant.firstUser?.phoneNumber?.formattedString() ?: return
    InfoCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { presentContactCard.onTap(participant.firstUser?.phoneNumber, participant.displayName) }
                    .padding(horizontal = Floats.cardHorizontalPadding, vertical = Floats.rowVerticalPadding),
        ) {
            Components.Text(number, foregroundColor = colors.titleText, font = Font.systemSemibold())
            Spacer(modifier = Modifier.weight(1f))
            DisclosureIndicator(foregroundColor = colors.subtitleText)
        }
    }
}

// MARK: - Media Preview Host

// Presents the media preview the view service holds.
@Composable
private fun MediaPreviewHost() {
    val viewService = remember { DependencyValues.current.chatInfoPageViewService }
    val mediaPreview by viewService.mediaPreview.collectAsState()
    val preview = mediaPreview ?: return
    MediaPreviewOverlay(
        mediaFiles = preview.filePaths.mapNotNull { MediaFile.from(it) },
        startIndex = preview.startingIndex,
        onDismiss = { viewService.mediaPreviewDismissed() },
        title = preview.title,
    )
}

// MARK: - Auxiliary

private fun buildChatInfoPageViewModel(conversationIDKey: String): ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action> =
    ViewModel(ChatInfoPageReducer.State(), ChatInfoPageReducer())
        .observing(DependencyValues.current.sharedEvents.chatInfoPageLoadingStateUpdated.events) {
            ChatInfoPageReducer.Action.LoadingStateUpdated
        }.observing(DependencyValues.current.sharedEvents.currentConversationActivityChanged.events) {
            ChatInfoPageReducer.Action.ViewAppeared(conversationIDKey)
        }.observing(DependencyValues.current.sharedEvents.currentConversationMetadataChanged.events) {
            ChatInfoPageReducer.Action.CurrentConversationMetadataChanged
        }.observing(MessageDeliveryService.isSendingMessage) {
            ChatInfoPageReducer.Action.IsSendingMessageChanged(it)
        }
