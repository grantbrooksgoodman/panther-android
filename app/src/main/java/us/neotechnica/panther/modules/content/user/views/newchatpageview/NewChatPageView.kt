//
//  NewChatPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.newchatpageview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.bundle.isNewChatPageDoneToolbarButtonEnabled
import us.neotechnica.panther.bundle.messageOutboxDidChange
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageInputBar
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.foundation.components.HeaderView
import us.neotechnica.panther.designsystem.modules.foundation.components.PageSheetView
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.components.ContactPairListView
import us.neotechnica.panther.modules.content.user.components.DeliveryProgressView
import us.neotechnica.panther.modules.content.user.components.RecipientBar
import us.neotechnica.panther.modules.content.user.components.RegisterContentPickerLaunchers
import us.neotechnica.panther.modules.content.user.components.anyOutboxSending
import us.neotechnica.panther.modules.content.user.components.rememberRegisteredDeliveryProgressIndicatorService
import us.neotechnica.panther.modules.content.user.constants.NewChatPageViewColors
import us.neotechnica.panther.modules.content.user.services.InputBarConfigService
import us.neotechnica.panther.modules.content.user.services.MediaActionHandlerService
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.recipientbar.RecipientBarLayoutService
import us.neotechnica.panther.modules.content.user.services.recipientbar.RecipientBarService
import us.neotechnica.panther.modules.content.user.services.recipientbar.RecipientBarTableViewService
import us.neotechnica.panther.modules.content.user.views.chatpageview.ChatPageView
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageView
import us.neotechnica.panther.modules.content.user.views.newchatpageview.NewChatPageReducer.Action
import us.neotechnica.panther.navigation.ChatNavigatorState
import us.neotechnica.panther.navigation.ChatRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates

/**
 * The new-conversation page: choose recipients through the recipient
 * bar, preview the matching conversation, compose a first message or
 * attach media, and create the conversation on send.
 *
 * @param modifier The modifier for this view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatPageView(modifier: Modifier = Modifier) {
    val viewModel = remember { buildNewChatViewModel() }
    val navigation = remember { DependencyValues.current.navigation }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.send(Action.ViewDisappeared)
            navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))
            viewModel.close()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.send(Action.ViewAppeared)
        viewModel.send(Action.MessageOutboxChanged(anyOutboxSending()))
    }

    val state by viewModel.state.collectAsState()
    val navState by navigation.state.collectAsState()
    val colors = LocalPantherColors.current
    val deliveryProgressIndicatorService = rememberRegisteredDeliveryProgressIndicatorService()
    RegisterContentPickerLaunchers()
    RegisterMediaMessageSentHook(viewModel)

    val recipientBar = RecipientBarService
    val selectedContactPairs by recipientBar.contactSelectionUI.selectedContactPairs.collectAsState()
    val isTextFieldFirstResponder by recipientBar.layout.isTextFieldFirstResponder.collectAsState()
    val inputBarPlaceholder by recipientBar.layout.inputBarPlaceholder.collectAsState()
    val isTableViewVisible by recipientBar.tableView.isTableViewVisible.collectAsState()
    val sections by recipientBar.tableView.sections.collectAsState()
    val conversationIDKey by recipientBar.config.conversationIDKey.collectAsState()

    val inputBarFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        recipientBar.layout.firstResponderRequests.collect { responder ->
            if (responder == RecipientBarLayoutService.FirstResponder.INPUT_BAR) {
                withFrameNanos {}
                runCatching { inputBarFocusRequester.requestFocus() }
            }
        }
    }

    // The page is dismissed only with its navigation bar button; the
    // system back gesture is ignored.
    BackHandler {}

    Box(modifier = modifier.fillMaxSize().background(pageBackgroundColor())) {
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
            Box {
                NewChatPageHeader(state = state, onDone = { viewModel.send(Action.DoneToolbarButtonTapped) })
                DeliveryProgressView(
                    progress = deliveryProgressIndicatorService.progress,
                    alpha = deliveryProgressIndicatorService.alpha,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            RecipientBar()

            if (isTableViewVisible) {
                SuggestionsTableView(
                    sections = sections,
                    onContactPairSelected = { contactPair ->
                        recipientBar.contactSelectionUI.selectContactPair(contactPair)
                        if (!recipientBar.layout.isTextFieldFirstResponder.value) {
                            recipientBar.layout.textFieldBecomeFirstResponder()
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    conversationIDKey?.let { key ->
                        key(key) {
                            ChatPageView(
                                conversationIDKey = key,
                                configuration = ChatPageView.Configuration.NewChat,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                MessageInputBar(
                    text = state.inputText,
                    placeholder = inputBarPlaceholder ?: "",
                    isSending = state.isSendingMessage || state.hasSendingOutboxEntry,
                    onTextChange = { viewModel.send(Action.InputChanged(it)) },
                    onSend = { viewModel.send(Action.SendTapped) },
                    onAttach = { MediaActionHandlerService.attachMediaButtonTapped() },
                    isSendEnabled = selectedContactPairs.isNotEmpty() && !isTextFieldFirstResponder && state.inputText.isNotBlank(),
                    isAttachEnabled = !isTextFieldFirstResponder,
                    images = InputBarConfigService.images(),
                    focusRequester = inputBarFocusRequester,
                )
            }
        }

        PageSheetView(
            isPresented = navState.chat.sheet == ChatNavigatorState.SheetPaths.ContactSelector,
            onDismissRequest = { navigation.navigate(Route.Chat(ChatRoute.Sheet(null))) },
            ignoresBottomSafeArea = true,
        ) {
            ContactSelectorPageView(entryPoint = ContactSelectorPageView.EntryPoint.NEW_CHAT_PAGE_VIEW)
        }
    }
}

// MARK: - Media Message Sent Hook

// A picked attachment sends as the first message; the hook then navigates
// into the created conversation.
@Composable
private fun RegisterMediaMessageSentHook(viewModel: ViewModel<NewChatPageReducer.State, NewChatPageReducer.Action>) {
    DisposableEffect(viewModel) {
        MediaActionHandlerService.onMediaMessageSent = { conversation ->
            viewModel.send(Action.FirstMessageSent)
            viewModel.send(Action.SendReturned(conversation.id.key))
        }
        onDispose { MediaActionHandlerService.onMediaMessageSent = null }
    }
}

// MARK: - Header

@Composable
private fun NewChatPageHeader(
    state: NewChatPageReducer.State,
    onDone: () -> Unit,
) {
    val colors = LocalPantherColors.current

    HeaderView(
        centerItem =
            HeaderView.CenterItemType.Text(
                HeaderView.TextAttributes(state.navigationTitle, foregroundColor = colors.navigationBarTitle),
            ),
        rightItem =
            HeaderView.PeripheralButtonType.Text(
                HeaderView.TextButtonAttributes(
                    text =
                        HeaderView.TextAttributes(
                            state.doneToolbarButtonText,
                            font = if (state.shouldUseBoldDoneToolbarButton) Font.systemSemibold() else Font.system,
                            foregroundColor = if (state.isDoneToolbarButtonEnabled) colors.navigationBarButton else colors.disabled,
                        ),
                    isEnabled = state.isDoneToolbarButtonEnabled,
                    action = onDone,
                ),
            ),
        attributes =
            HeaderView.Attributes(
                showsDivider = state.shouldUseBoldDoneToolbarButton,
                sizeClass = HeaderView.SizeClass.Sheet,
                windowInsets = WindowInsets.statusBars,
            ),
    )
}

// MARK: - Suggestions Table

// The recipient bar's suggestions, laid out as the contact selector's
// list is.
@Composable
private fun SuggestionsTableView(
    sections: List<RecipientBarTableViewService.TableViewSection>,
    onContactPairSelected: (ContactPair) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val contactPairsByLetter = remember(sections) { sections.associate { it.letter to it.contactPairs } }

    // Dismiss the keyboard once the contact list starts scrolling.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { if (it) KeyboardService.resignFirstResponders() }
    }

    ContactPairListView(
        sections = contactPairsByLetter,
        onSelect = onContactPairSelected,
        modifier = modifier,
        state = listState,
    )
}

private fun buildNewChatViewModel(): ViewModel<NewChatPageReducer.State, NewChatPageReducer.Action> =
    ViewModel(NewChatPageReducer.State(), NewChatPageReducer())
        .observing(MessageDeliveryService.isSendingMessage) {
            Action.IsSendingMessageChanged(it)
        }.observing(DependencyValues.current.sharedEvents.messageOutboxDidChange.events) {
            Action.MessageOutboxChanged(anyOutboxSending())
        }.observing(DependencyValues.current.sharedStates.isNewChatPageDoneToolbarButtonEnabled.changes) {
            Action.IsDoneToolbarButtonEnabledChanged(it)
        }

// MARK: - Auxiliary

// In dark mode, the chat area and input bar sit on dark gray rather than
// the default black background.
@Composable
private fun pageBackgroundColor(): Color =
    if (ThemeService.isDarkModeActive(isSystemInDarkTheme())) {
        NewChatPageViewColors.darkBackground
    } else {
        LocalPantherColors.current.background
    }
