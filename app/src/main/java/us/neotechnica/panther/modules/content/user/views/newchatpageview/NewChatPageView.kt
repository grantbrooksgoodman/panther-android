//
//  NewChatPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.newchatpageview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.isNewChatPageDoneToolbarButtonEnabled
import us.neotechnica.panther.bundle.messageOutboxDidChange
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageInputBar
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.services.InviteService
import us.neotechnica.panther.modules.content.user.components.ContactPairCellView
import us.neotechnica.panther.modules.content.user.components.ContentPickers
import us.neotechnica.panther.modules.content.user.components.DeliveryProgressView
import us.neotechnica.panther.modules.content.user.components.RecipientBar
import us.neotechnica.panther.modules.content.user.components.anyOutboxSending
import us.neotechnica.panther.modules.content.user.components.rememberContentPickers
import us.neotechnica.panther.modules.content.user.components.rememberRegisteredDeliveryProgressIndicatorService
import us.neotechnica.panther.modules.content.user.constants.NewChatPageViewFloats
import us.neotechnica.panther.modules.content.user.extensions.hasContactsBesidesCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.syncIfNeeded
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.RecipientBarContactSelectionUIService
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageReducer
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageView
import us.neotechnica.panther.modules.content.user.views.newchatpageview.NewChatPageReducer.Action
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.navigation.ChatNavigatorState
import us.neotechnica.panther.navigation.ChatRoute
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action as AlertKitAction

// MARK: - Constants Accessors

private typealias Floats = NewChatPageViewFloats

/**
 * The new-conversation page: choose recipients through the recipient
 * bar (typing a phone number or picking from contacts), compose a first
 * message or attach media, and create the conversation on send.
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deliveryProgressIndicatorService = rememberRegisteredDeliveryProgressIndicatorService()
    val pickers =
        rememberContentPickers(
            onPicked = { mediaFile ->
                scope.launch {
                    try {
                        MessageDeliveryService.sendMediaMessage(mediaFile)
                        val conversationIDKey = ConversationSessionService.currentConversation?.id?.key
                        if (conversationIDKey != null) viewModel.send(Action.SendReturned(conversationIDKey))
                    } catch (exception: Exception) {
                        Logger.log(exception, with = AlertType.toast)
                    }
                }
            },
            onFailed = { Logger.log(it, with = AlertType.toast) },
        )

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Box {
                Header(
                    title = state.navigationTitle,
                    doneText = state.doneToolbarButtonText,
                    isDoneBold = state.shouldUseBoldDoneToolbarButton,
                    isDoneEnabled = state.isDoneToolbarButtonEnabled,
                    onDone = { viewModel.send(Action.DoneToolbarButtonTapped) },
                )
                DeliveryProgressView(
                    progress = deliveryProgressIndicatorService.progress,
                    alpha = deliveryProgressIndicatorService.alpha,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            RecipientBar(
                selectedContactPairs = state.selectedContactPairs,
                query = state.recipientQuery,
                highlightedViewID = state.highlightedViewID,
                onQueryChange = { viewModel.send(Action.RecipientQueryChanged(it)) },
                onSubmit = { viewModel.send(Action.RecipientQuerySubmitted) },
                onBackspace = { viewModel.send(Action.RecipientBackspaced) },
                onChipTap = { viewModel.send(Action.RecipientChipTapped(it)) },
                onRemove = { viewModel.send(Action.RemoveRecipientTapped(it)) },
                onAdd = { scope.launch { selectContactButtonTapped(context, navigation) } },
            )

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(state.suggestions, key = { it.contact.encodedHash }) { contactPair ->
                    ContactPairCellView(
                        contactPair = contactPair,
                        action = { viewModel.send(Action.SuggestionTapped(contactPair)) },
                    )
                    HorizontalDivider(color = colors.groupedContentBackground)
                }
            }

            MessageInputBar(
                text = state.inputText,
                placeholder = LocalizedStringKey.NewMessage.localized(),
                isSending = state.isSendingMessage || state.hasSendingOutboxEntry,
                onTextChange = { viewModel.send(Action.InputChanged(it)) },
                onSend = { viewModel.send(Action.SendTapped) },
                onAttach = { scope.launch { presentAttachMediaSheet(pickers) } },
                enabled = state.selectedContactPairs.isNotEmpty(),
            )
        }

        if (navState.chat.sheet == ChatNavigatorState.SheetPath.ContactSelector) {
            ModalBottomSheet(
                onDismissRequest = { navigation.navigate(Route.Chat(ChatRoute.Sheet(null))) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                ContactSelectorPageView(entryPoint = ContactSelectorPageReducer.EntryPoint.NEW_CHAT_PAGE_VIEW)
            }
        }
    }
}

private fun buildNewChatViewModel(): ViewModel<NewChatPageReducer.State, NewChatPageReducer.Action> =
    ViewModel(NewChatPageReducer.State(), NewChatPageReducer())
        .observing(MessageDeliveryService.isSendingMessage) {
            Action.IsSendingMessageChanged(it)
        }.observing(DependencyValues.current.sharedEvents.messageOutboxDidChange.events) {
            Action.MessageOutboxChanged(anyOutboxSending())
        }.observing(RecipientBarContactSelectionUIService.selectedContactPairs) {
            Action.SelectedContactPairsChanged(it)
        }.observing(RecipientBarContactSelectionUIService.highlightedViewID) {
            Action.HighlightedViewIDChanged(it)
        }.observing(DependencyValues.current.sharedStates.isNewChatPageDoneToolbarButtonEnabled.changes) {
            Action.IsDoneToolbarButtonEnabledChanged(it)
        }

/**
 * Opens the contact selector, mirroring the iOS `selectContactButtonTapped`:
 * a call-to-action when contacts access is denied, a contact-pair sync and
 * an invitation prompt when the address book is empty, and otherwise the
 * selector.
 */
private suspend fun selectContactButtonTapped(
    context: Context,
    navigation: Navigation,
) {
    if (!ContactService.hasContactPermission()) {
        presentContactsPermissionCTA(context)
        return
    }

    if (!ContactService.hasContactsBesidesCurrentUser) {
        val isDoneToolbarButtonEnabled = DependencyValues.current.sharedStates.isNewChatPageDoneToolbarButtonEnabled
        isDoneToolbarButtonEnabled.value = false
        HUD.showProgress(isModal = true)
        runCatching { ContactService.syncIfNeeded() }
        HUD.hide()
        isDoneToolbarButtonEnabled.value = true

        if (!ContactService.hasContactsBesidesCurrentUser) {
            runCatching { InviteService.presentInvitationPrompt() }
            return
        }
    }

    navigation.navigate(Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPath.ContactSelector)))
}

private suspend fun presentContactsPermissionCTA(context: Context) {
    val shouldOpenSettings =
        ActionSheetAlert(
            title = "Contacts Access",
            message = "Enable contacts access in Settings to choose a recipient.",
            confirmButtonTitle = "Open Settings",
            cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
        ).present(
            translating =
                listOf(
                    ActionSheetAlert.TranslationOptionKey.Actions(),
                    ActionSheetAlert.TranslationOptionKey.Message,
                    ActionSheetAlert.TranslationOptionKey.Title,
                ),
        )
    if (!shouldOpenSettings) return

    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }
}

// Mirrors the iOS `MediaActionHandlerService.attachMediaButtonTapped` action sheet.
private suspend fun presentAttachMediaSheet(pickers: ContentPickers) {
    ActionSheetAlert(
        title = "Attach media",
        actions =
            listOf(
                AlertKitAction("Take photo") { pickers.launchCamera() },
                AlertKitAction("Select document") { pickers.launchDocument() },
                AlertKitAction("Select photo or video") { pickers.launchPhotoOrVideo() },
            ),
        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
    ).present(
        translating =
            listOf(
                ActionSheetAlert.TranslationOptionKey.Title,
                ActionSheetAlert.TranslationOptionKey.Actions(),
            ),
    )
}

// MARK: - Header

@Composable
private fun Header(
    title: String,
    doneText: String,
    isDoneBold: Boolean,
    isDoneEnabled: Boolean,
    onDone: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.headerHorizontalPadding, vertical = Floats.headerVerticalPadding),
    ) {
        Components.Text(
            title,
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            modifier = Modifier.align(Alignment.Center),
        )
        Components.Text(
            doneText,
            color = if (isDoneEnabled) colors.accent else colors.disabled,
            font = if (isDoneBold) Font.systemSemibold() else Font.system,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(enabled = isDoneEnabled, onClick = onDone),
        )
    }
}
