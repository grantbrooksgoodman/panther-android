//
//  ChatInfoPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.chatInfoPageLoadingStateUpdated
import us.neotechnica.panther.bundle.currentConversationActivityChanged
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.components.ContactCardPresenter
import us.neotechnica.panther.modules.common.contacts.components.rememberContactCardPresenter
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.content.user.components.AddContactButton
import us.neotechnica.panther.modules.content.user.components.ChatInfoContactSelectorHost
import us.neotechnica.panther.modules.content.user.components.ChatParticipantView
import us.neotechnica.panther.modules.content.user.components.MediaItemView
import us.neotechnica.panther.modules.content.user.components.MediaPreviewOverlay
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import us.neotechnica.panther.modules.content.user.models.MediaItemViewData
import us.neotechnica.panther.modules.content.user.services.MediaActionHandlerService
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.localization.models.LocalizationSource
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import java.io.File
import androidx.compose.material3.Text as Material3Text

// MARK: - Constants Accessors

private typealias Floats = ChatInfoPageViewFloats
private typealias Colors = ChatInfoPageViewColors
private typealias Strings = ChatInfoPageViewConstants

/**
 * A conversation's info page.
 *
 * For a group: a large avatar, the conversation title, a rename
 * action, an expandable participants card, and a leave action. A
 * participant row opens a contact card on tap and swipes to remove the
 * participant; a long press removes a saved device contact through the
 * contact sheet, while a number that is not a saved contact offers
 * removal from the sheet its tap already opens. For a one-to-one
 * conversation: the other participant's contact card.
 *
 * @param conversationIDKey The identifier key of the conversation.
 * @param modifier The modifier for this view.
 */
@Composable
fun ChatInfoPageView(
    conversationIDKey: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { buildChatInfoViewModel() }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.send(ChatInfoPageReducer.Action.ViewDisappeared)
            viewModel.close()
        }
    }
    LaunchedEffect(conversationIDKey) {
        viewModel.send(ChatInfoPageReducer.Action.ViewFirstAppeared(conversationIDKey))
    }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val conversation = state.conversation
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    val showMediaSegment = state.mediaItems.isNotEmpty() && state.segmentedControlSelectionIndex == 1
    val presentContactCard = rememberContactCardPresenter()
    val singleParticipant = conversation?.takeUnless { state.isGroup }?.let { state.chatParticipants.firstOrNull() }

    GroupPhotoCaptureEffect(state.photoCaptureRequest, viewModel)

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        StatefulView(state = state.viewState) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .verticalScroll(rememberScrollState()),
            ) {
                ChatInfoHeader(
                    conversation = conversation,
                    title = state.chatTitleLabelText,
                    isGroup = state.isGroup,
                    onDone = { viewModel.send(ChatInfoPageReducer.Action.DoneHeaderItemTapped) },
                    onContactTap =
                        singleParticipant?.firstUser?.let { user ->
                            { presentContactCard.onTap(user.phoneNumber, user.displayName, null) }
                        },
                )

                if (state.isGroup && conversation != null) {
                    GroupContent(
                        state = state,
                        viewModel = viewModel,
                        showMediaSegment = showMediaSegment,
                        presentContactCard = presentContactCard,
                        onPreviewIndexChange = { previewIndex = it },
                    )
                } else if (singleParticipant != null) {
                    SingleContactCard(singleParticipant) {
                        singleParticipant.firstUser?.let { presentContactCard.onTap(it.phoneNumber, it.displayName, null) }
                    }
                }

                Spacer(modifier = Modifier.padding(bottom = Floats.bottomSpacerPadding))
            }
        }

        previewIndex?.let { index ->
            MediaPreviewOverlay(
                mediaFiles = state.mediaItems.map { it.file },
                startIndex = index,
                onDismiss = { previewIndex = null },
            )
        }

        ChatInfoContactSelectorHost()
    }
}

// MARK: - Group Content

@Composable
@Suppress("LongParameterList")
private fun GroupContent(
    state: ChatInfoPageReducer.State,
    viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>,
    showMediaSegment: Boolean,
    presentContactCard: ContactCardPresenter,
    onPreviewIndexChange: (Int?) -> Unit,
) {
    Components.CapsuleButton(
        state.strings.value(ChatInfoPageViewStrings.changeMetadataButtonText),
        onClick = { viewModel.send(ChatInfoPageReducer.Action.ChangeMetadataButtonTapped) },
        primary = true,
        isEnabled = state.isChangeMetadataButtonEnabled,
        modifier =
            Modifier
                .padding(top = Floats.changeNameTopPadding)
                .padding(bottom = Floats.changeNameBottomPadding),
    )

    if (state.mediaItems.isNotEmpty()) {
        SegmentedControl(
            titles =
                listOf(
                    state.strings.value(ChatInfoPageViewStrings.segmentedControlParticipantsOptionText),
                    state.strings.value(ChatInfoPageViewStrings.segmentedControlMediaOptionText),
                ),
            selectedIndex = state.segmentedControlSelectionIndex,
            onSelect = { viewModel.send(ChatInfoPageReducer.Action.SegmentedControlSelectionIndexChanged(it)) },
        )
    }

    if (showMediaSegment) {
        MediaList(items = state.mediaItems, onTap = { onPreviewIndexChange(it) })
    } else {
        ParticipantsCard(
            participants = state.visibleParticipants,
            allParticipants = state.chatParticipants,
            isExpanded = state.visibleParticipants.isNotEmpty(),
            strings = state.strings,
            showAddContact = state.visibleParticipantsIncrement == 1,
            isAddContactEnabled = state.isAddContactButtonEnabled,
            showsRemoveUserSwipeAction = state.showsRemoveUserSwipeAction,
            onToggle = { viewModel.send(ChatInfoPageReducer.Action.ChatInfoCellTapped) },
            onAddContact = { viewModel.send(ChatInfoPageReducer.Action.AddContactButtonTapped) },
            onParticipantTap = { participant ->
                val removeFromConversation: (() -> Unit)? =
                    if (state.showsRemoveUserSwipeAction) {
                        { viewModel.send(ChatInfoPageReducer.Action.RemoveUserButtonTapped(participant)) }
                    } else {
                        null
                    }
                presentContactCard.onTap(participant.firstUser?.phoneNumber, participant.displayName, removeFromConversation)
            },
            onParticipantLongPress = { participant ->
                presentContactCard.onLongPress(participant.firstUser?.phoneNumber, participant.displayName) {
                    viewModel.send(ChatInfoPageReducer.Action.RemoveUserButtonTapped(participant))
                }
            },
            onUserInfoBadgeTapped = { viewModel.send(ChatInfoPageReducer.Action.UserInfoBadgeTapped(it.firstUser)) },
            onRemove = { viewModel.send(ChatInfoPageReducer.Action.RemoveUserButtonTapped(it)) },
        )
    }

    // The leave row shows under both segments.
    LeaveRow(
        enabled = state.isLeaveConversationButtonEnabled,
        text = state.strings.value(ChatInfoPageViewStrings.leaveConversation),
        onClick = { viewModel.send(ChatInfoPageReducer.Action.LeaveConversationButtonTapped) },
    )
}

// MARK: - Header

@Composable
private fun ChatInfoHeader(
    conversation: Conversation?,
    title: String,
    isGroup: Boolean,
    onDone: () -> Unit,
    onContactTap: (() -> Unit)?,
) {
    val colors = LocalPantherColors.current
    Row(
        horizontalArrangement = Arrangement.End,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = Floats.headerHorizontalPadding,
                    end = Floats.headerHorizontalPadding,
                    top = Floats.headerTopPadding,
                ),
    ) {
        CircleChipButton(
            systemName = "checkmark",
            contentDescription = LocalizedStringKey.Done.localized(LocalizationSource.SUBSYSTEM),
            onClick = onDone,
            tint = colors.titleText,
            glyphSize = Floats.doneButtonGlyphSize,
        )
    }

    val contactTapModifier = if (onContactTap != null) Modifier.clickable(onClick = onContactTap) else Modifier

    AvatarImageView(
        modifier = contactTapModifier.padding(top = Floats.avatarTopPadding).size(Floats.avatarSize),
        imageData = conversation?.metadata?.imageData,
        fallbackSymbol = if (isGroup) "person.2" else "person.crop.circle.fill",
        glyphSize = Floats.avatarGlyphSize,
    )

    Components.Text(
        title,
        color = colors.titleText,
        font = Font.systemBold(FontScale.Large),
        textAlign = TextAlign.Center,
        modifier =
            contactTapModifier.padding(
                top = Floats.titleTopPadding,
                start = Floats.titleHorizontalPadding,
                end = Floats.titleHorizontalPadding,
            ),
    )
}

// MARK: - Single Contact Card

@Composable
private fun SingleContactCard(
    participant: ChatParticipant,
    onTap: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val number = participant.firstUser?.phoneNumber?.formattedString() ?: return
    InfoCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTap)
                    .padding(horizontal = Floats.cardHorizontalPadding, vertical = Floats.rowVerticalPadding),
        ) {
            Components.Text(number, color = colors.titleText, font = Font.systemSemibold())
            Spacer(modifier = Modifier.weight(1f))
            Components.Symbol("chevron.right", color = colors.subtitleText, modifier = Modifier.size(Floats.chevronGlyphSize))
        }
    }
}

// MARK: - Segmented Control

@Composable
private fun SegmentedControl(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalPantherColors.current
    val isDarkMode = ThemeService.isDarkModeActive(isSystemInDarkTheme())

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Floats.segmentedControlHorizontalPadding,
                    vertical = Floats.segmentedControlTopPadding,
                ).clip(RoundedCornerShape(Floats.segmentedControlCornerRadius))
                .background(Colors.segmentedControlTrack)
                .padding(Floats.segmentedControlTrackPadding),
    ) {
        titles.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Floats.segmentCornerRadius))
                        .background(if (isSelected) (if (isDarkMode) Color.Gray else Colors.segmentedControlTrack) else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = Floats.segmentVerticalPadding),
            ) {
                Components.Text(
                    title,
                    color = if (isSelected) colors.titleText else colors.subtitleText,
                    font = if (isSelected) Font.systemSemibold(FontScale.Small) else Font.system(FontScale.Small),
                )
            }
        }
    }
}

// MARK: - Media List

@Composable
private fun MediaList(
    items: List<MediaItemViewData>,
    onTap: (Int) -> Unit,
) {
    InfoCard {
        items.forEachIndexed { index, item ->
            if (index > 0) CardDivider()
            MediaItemView(data = item, onClick = { onTap(index) })
        }
    }
}

// MARK: - Participants Card

@Composable
@Suppress("LongParameterList")
private fun ParticipantsCard(
    participants: List<ChatParticipant>,
    allParticipants: List<ChatParticipant>,
    isExpanded: Boolean,
    strings: List<TranslationOutputMap>,
    showAddContact: Boolean,
    isAddContactEnabled: Boolean,
    showsRemoveUserSwipeAction: Boolean,
    onToggle: () -> Unit,
    onAddContact: () -> Unit,
    onParticipantTap: (ChatParticipant) -> Unit,
    onParticipantLongPress: (ChatParticipant) -> Unit,
    onUserInfoBadgeTapped: (ChatParticipant) -> Unit,
    onRemove: (ChatParticipant) -> Unit,
) {
    InfoCard {
        ParticipantsHeaderRow(
            count = allParticipants.size,
            peopleText = strings.value(ChatInfoPageViewStrings.participantCountLabelText),
            subtitle = allParticipants.joinToString(Strings.PARTICIPANTS_SEPARATOR) { it.displayName },
            isExpanded = isExpanded,
            onToggle = onToggle,
        )
        if (isExpanded) {
            participants.forEach { participant ->
                CardDivider()
                ChatParticipantView(
                    participant = participant,
                    onTap = { onParticipantTap(participant) },
                    onUserInfoBadgeTapped = { onUserInfoBadgeTapped(participant) },
                    onRemove =
                        if (showsRemoveUserSwipeAction) {
                            { onRemove(participant) }
                        } else {
                            null
                        },
                    onLongPress =
                        if (showsRemoveUserSwipeAction) {
                            { onParticipantLongPress(participant) }
                        } else {
                            null
                        },
                )
            }
            if (showAddContact) {
                CardDivider()
                AddContactButton(
                    text = strings.value(ChatInfoPageViewStrings.addContactButtonText),
                    isEnabled = isAddContactEnabled,
                    onClick = onAddContact,
                )
            }
        }
    }
}

@Composable
private fun ParticipantsHeaderRow(
    count: Int,
    peopleText: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(Floats.cardPadding),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Components.Text(
                "$count $peopleText",
                color = colors.titleText,
                font = Font.systemBold(),
            )
            Material3Text(
                subtitle,
                color = colors.subtitleText,
                style = Font.system(FontScale.Small).textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Floats.subtitleTopPadding),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .padding(start = Floats.chevronBoxStartPadding)
                    .size(Floats.chevronBoxSize)
                    .clip(CircleShape)
                    .border(Floats.chevronBoxBorderWidth, colors.subtitleText, CircleShape),
        ) {
            Components.Symbol(
                if (isExpanded) "chevron.down" else "chevron.right",
                color = colors.subtitleText,
                modifier = Modifier.size(Floats.chevronGlyphSize),
            )
        }
    }
}

// MARK: - Actions

@Composable
private fun LeaveRow(
    enabled: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.cardHorizontalMargin, vertical = Floats.cardVerticalMargin)
                .clip(RoundedCornerShape(Floats.cardCornerRadius))
                .background(colors.groupedRowBackground)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(Floats.cardPadding),
    ) {
        Components.Text(
            text,
            color = if (enabled) Colors.destructive else colors.subtitleText,
        )
    }
}

// MARK: - Card Scaffolding

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    val colors = LocalPantherColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.cardHorizontalMargin, vertical = Floats.cardVerticalMargin)
                .clip(RoundedCornerShape(Floats.cardCornerRadius))
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
        modifier = Modifier.padding(start = Floats.cardHorizontalPadding),
    )
}

// MARK: - Auxiliary

private fun buildChatInfoViewModel(): ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action> =
    ViewModel(ChatInfoPageReducer.State(), ChatInfoPageReducer())
        .observing(DependencyValues.current.sharedEvents.chatInfoPageLoadingStateUpdated.events) {
            ChatInfoPageReducer.Action.LoadingStateUpdated
        }.observing(DependencyValues.current.sharedEvents.currentConversationActivityChanged.events) {
            ChatInfoPageReducer.Action.CurrentConversationMetadataChanged
        }.observing(MessageDeliveryService.isSendingMessage) {
            ChatInfoPageReducer.Action.IsSendingMessageChanged(it)
        }

/**
 * Sets up the camera and photo-library launchers and, on each new
 * capture request, launches the chosen source, delivering the
 * compressed image data on selection.
 */
@Composable
private fun GroupPhotoCaptureEffect(
    request: ChatInfoPageReducer.PhotoCaptureSource?,
    viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onImageData: (ByteArray) -> Unit = { viewModel.send(ChatInfoPageReducer.Action.SelectedImageChanged(it)) }
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) {
                viewModel.send(ChatInfoPageReducer.Action.PhotoPickerDismissed(null))
                return@rememberLauncherForActivityResult
            }
            scope.launch { compressAndDeliverGroupPhoto(uri, onImageData) }
        }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraUri
            if (success && uri != null) {
                scope.launch { compressAndDeliverGroupPhoto(uri, onImageData) }
            } else {
                viewModel.send(ChatInfoPageReducer.Action.CameraPickerDismissed(null))
            }
        }
    LaunchedEffect(request) {
        val source = request ?: return@LaunchedEffect
        viewModel.send(ChatInfoPageReducer.Action.PhotoCaptureHandled(source))
        when (source) {
            ChatInfoPageReducer.PhotoCaptureSource.CAMERA -> {
                val uri = groupPhotoCaptureUri(context)
                cameraUri = uri
                cameraLauncher.launch(uri)
            }

            ChatInfoPageReducer.PhotoCaptureSource.LIBRARY ->
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

private suspend fun compressAndDeliverGroupPhoto(
    uri: Uri,
    onImageData: (ByteArray) -> Unit,
) {
    try {
        onImageData(MediaActionHandlerService.compressImageToKB(uri, GROUP_PHOTO_COMPRESSION_SIZE_KB))
    } catch (exception: Exception) {
        Logger.log(exception, with = AlertType.toast)
    }
}

private fun groupPhotoCaptureUri(context: Context): Uri {
    val file = File(context.filesDir, "$GROUP_PHOTO_DIRECTORY/$GROUP_PHOTO_CAPTURE_NAME")
    file.parentFile?.mkdirs()
    if (!file.exists()) file.createNewFile()
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private const val GROUP_PHOTO_COMPRESSION_SIZE_KB = 100
private const val GROUP_PHOTO_DIRECTORY = "media"
private const val GROUP_PHOTO_CAPTURE_NAME = "group-photo-capture.jpg"
