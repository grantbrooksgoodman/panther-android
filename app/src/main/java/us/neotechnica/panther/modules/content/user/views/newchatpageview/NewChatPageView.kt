//
//  NewChatPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.newchatpageview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageInputBar
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.ContactPairCellView
import us.neotechnica.panther.modules.content.user.components.DeliveryProgressView
import us.neotechnica.panther.modules.content.user.constants.NewChatPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.NewChatPageViewStrings
import us.neotechnica.panther.modules.content.user.services.DeliveryProgressIndicatorService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.services.InviteService
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.extensions.hasContactsBesidesCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.syncIfNeeded
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageReducer
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageView
import us.neotechnica.panther.modules.content.user.views.newchatpageview.NewChatPageReducer.Action
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.navigation.ChatNavigatorState
import us.neotechnica.panther.navigation.ChatRoute
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.modules.session.entity.extensions.messageOutboxDidChange
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.modules.session.clientSession

// MARK: - Constants Accessors

private typealias Floats = NewChatPageViewFloats
private typealias Strings = NewChatPageViewStrings

/**
 * The new-conversation page: add recipients via the recipient bar (typing
 * a phone number or picking from contacts), compose a first message, and
 * create the conversation on send. Mirrors the iOS `NewChatPageView`.
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
        viewModel.send(Action.ViewFirstAppeared)
        viewModel.send(Action.MessageOutboxChanged(anyOutboxSending()))
    }

    val state by viewModel.state.collectAsState()
    val navState by navigation.state.collectAsState()
    val colors = LocalPantherColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deliveryProgressIndicatorService = rememberRegisteredDeliveryProgressIndicatorService()

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Box {
                Header(onClose = { viewModel.send(Action.BackTapped) })
                DeliveryProgressView(
                    progress = deliveryProgressIndicatorService.progress,
                    alpha = deliveryProgressIndicatorService.alpha,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            RecipientBar(
                recipients = state.recipients,
                query = state.recipientQuery,
                highlightedRecipientID = state.highlightedRecipientID,
                onQueryChange = { viewModel.send(Action.RecipientQueryChanged(it)) },
                onSubmit = { viewModel.send(Action.RecipientQuerySubmitted) },
                onBackspace = { viewModel.send(Action.RecipientBackspaced) },
                onRemove = { viewModel.send(Action.RemoveRecipient(it)) },
                onAdd = { scope.launch { selectContactButtonTapped(context, navigation) } },
            )

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(state.suggestions, key = { it.userID }) { contact ->
                    val user = SessionStore.users[contact.userID]
                    val contactPair = user?.let { it.contactPair ?: ContactPair.withUser(it, name = it.displayName) }
                    if (contactPair != null) {
                        ContactPairCellView(
                            contactPair = contactPair,
                            action = { viewModel.send(Action.AddRecipient(contact.userID, contact.fullName)) },
                        )
                        HorizontalDivider(color = colors.groupedContentBackground)
                    }
                }
            }

            MessageInputBar(
                text = state.inputText,
                placeholder = LocalizedStringKey.NewMessage.localized(),
                isSending = state.isSendingMessage || state.hasSendingOutboxEntry,
                onTextChange = { viewModel.send(Action.InputChanged(it)) },
                onSend = { viewModel.send(Action.SendTapped) },
                onAttach = {},
                enabled = state.recipients.isNotEmpty(),
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

@Composable
private fun rememberRegisteredDeliveryProgressIndicatorService(): DeliveryProgressIndicatorService {
    val scope = rememberCoroutineScope()
    val service = remember { DeliveryProgressIndicatorService(scope) }
    DisposableEffect(service) {
        DependencyValues.current.clientSession.registerDeliveryProgressIndicator(service)
        onDispose { service.teardown() }
    }
    return service
}

private fun buildNewChatViewModel(): ViewModel<NewChatPageReducer.State, NewChatPageReducer.Action> =
    ViewModel(NewChatPageReducer.State(), NewChatPageReducer())
        .observing(MessageDeliveryService.isSendingMessage) {
            Action.IsSendingMessageChanged(it)
        }.observing(DependencyValues.current.sharedEvents.messageOutboxDidChange.events) {
            Action.MessageOutboxChanged(anyOutboxSending())
        }

private fun anyOutboxSending(): Boolean = MessageOutboxService.allEntries.any { it.state == OutboxEntry.State.SENDING }

/**
 * Opens the contact selector, mirroring the iOS `selectContactButtonTapped`:
 * a call-to-action when contacts access is denied, a contact-pair sync and an
 * invitation prompt when the address book is empty, and otherwise the selector.
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
        runCatching { ContactService.syncIfNeeded() }
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

// MARK: - Header

@Composable
private fun Header(onClose: () -> Unit) {
    val colors = LocalPantherColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.headerHorizontalPadding, vertical = Floats.headerVerticalPadding),
    ) {
        Components.Text(
            LocalizedStringKey.NewMessage.localized(),
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            modifier = Modifier.align(Alignment.Center),
        )
        CircleChipButton(
            systemName = "xmark",
            contentDescription = Strings.CLOSE,
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterEnd),
            tint = colors.titleText,
            glyphSize = Floats.closeButtonGlyphSize,
        )
    }
}

// MARK: - Recipient Bar

@Composable
@Suppress("LongParameterList")
private fun RecipientBar(
    recipients: List<NewChatPageReducer.Recipient>,
    query: String,
    highlightedRecipientID: String?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackspace: () -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Floats.recipientBarHorizontalPadding,
                    vertical = Floats.recipientBarVerticalPadding,
                ).clip(RoundedCornerShape(Floats.recipientBarCornerRadius))
                .background(colors.background)
                .padding(
                    start = Floats.recipientBarStartPadding,
                    end = Floats.recipientBarEndPadding,
                    top = Floats.recipientBarTopPadding,
                    bottom = Floats.recipientBarBottomPadding,
                ),
    ) {
        Components.Text(
            LocalizedStringKey.To.localized(),
            color = colors.subtitleText,
            modifier = Modifier.padding(end = Floats.toLabelEndPadding),
        )

        RecipientBarContent(
            recipients = recipients,
            query = query,
            highlightedRecipientID = highlightedRecipientID,
            onQueryChange = onQueryChange,
            onSubmit = onSubmit,
            onBackspace = onBackspace,
            onRemove = onRemove,
            modifier = Modifier.weight(1f),
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .padding(start = Floats.addButtonStartPadding)
                    .size(Floats.addButtonSize)
                    .clip(CircleShape)
                    .background(colors.groupedContentBackground)
                    .clickable(onClick = onAdd),
        ) {
            Components.Symbol("plus", color = colors.accent, modifier = Modifier.size(Floats.addButtonGlyphSize))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongParameterList")
private fun RecipientBarContent(
    recipients: List<NewChatPageReducer.Recipient>,
    query: String,
    highlightedRecipientID: String?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackspace: () -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Floats.chipSpacing),
        verticalArrangement = Arrangement.spacedBy(Floats.chipSpacing),
        modifier = modifier,
    ) {
        recipients.forEach { recipient ->
            RecipientChip(
                recipient = recipient,
                isHighlighted = recipient.userID == highlightedRecipientID,
                onRemove = { onRemove(recipient.userID) },
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = Font.system.textStyle.copy(color = colors.titleText),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier =
                Modifier
                    .defaultMinSize(minWidth = Floats.fieldMinWidth)
                    .padding(vertical = Floats.fieldVerticalPadding)
                    .onPreviewKeyEvent { event ->
                        val isBackspaceOnEmpty =
                            event.type == KeyEventType.KeyDown &&
                                event.key == Key.Backspace &&
                                query.isEmpty() &&
                                recipients.isNotEmpty()
                        if (isBackspaceOnEmpty) {
                            onBackspace()
                            true
                        } else {
                            false
                        }
                    },
        )
    }
}

@Composable
private fun RecipientChip(
    recipient: NewChatPageReducer.Recipient,
    isHighlighted: Boolean,
    onRemove: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val backgroundColor = if (isHighlighted) colors.accent else colors.accent.copy(alpha = Floats.CHIP_BACKGROUND_ALPHA)
    val contentColor = if (isHighlighted) Color.White else colors.accent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(onClick = onRemove)
                .padding(
                    start = Floats.recipientChipStartPadding,
                    end = Floats.recipientChipEndPadding,
                    top = Floats.recipientChipVerticalPadding,
                    bottom = Floats.recipientChipVerticalPadding,
                ),
    ) {
        Components.Text(recipient.displayName, color = contentColor, font = Font.systemMedium(FontScale.Small))
        Components.Symbol(
            "xmark",
            color = contentColor,
            modifier =
                Modifier
                    .padding(start = Floats.chipRemoveIconStartPadding)
                    .size(Floats.chipRemoveIconSize),
        )
    }
}
