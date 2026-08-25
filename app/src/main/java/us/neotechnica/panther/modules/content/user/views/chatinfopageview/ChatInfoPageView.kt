//
//  ChatInfoPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.user.components.MediaItemView
import us.neotechnica.panther.modules.content.user.components.MediaPreviewOverlay
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewStrings
import us.neotechnica.panther.modules.content.user.models.MediaItemViewData
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.extensions.users
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import androidx.compose.material3.Text as Material3Text

// MARK: - Constants Accessors

private typealias Floats = ChatInfoPageViewFloats
private typealias Colors = ChatInfoPageViewColors
private typealias Strings = ChatInfoPageViewStrings

/**
 * A conversation's info page: a large avatar, the conversation title, and
 * — for groups — a rename action, an expandable participants card, and a
 * leave action. Mirrors the iOS `ChatInfoPageView`.
 *
 * **Note:** adding participants (needs the contact selector) and shared
 * media attachments are deferred with their underlying layers.
 *
 * @param conversationIDKey The identifier key of the conversation.
 * @param modifier The modifier for this view.
 */
@Composable
fun ChatInfoPageView(
    conversationIDKey: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { ViewModel(ChatInfoPageReducer.State(), ChatInfoPageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(conversationIDKey) {
        viewModel.send(ChatInfoPageReducer.Action.ViewFirstAppeared(conversationIDKey))
    }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val conversation = state.conversation
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    val showMediaSegment = state.mediaItems.isNotEmpty() && state.selectedSegment == 1

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
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
                isGroup = state.isGroup,
                onDone = { viewModel.send(ChatInfoPageReducer.Action.BackTapped) },
            )

            if (state.isGroup && conversation != null) {
                Components.CapsuleButton(
                    Strings.CHANGE_NAME_AND_PHOTO,
                    onClick = { viewModel.send(ChatInfoPageReducer.Action.ChangeMetadataTapped) },
                    primary = true,
                    modifier =
                        Modifier
                            .padding(top = Floats.changeNameTopPadding)
                            .padding(bottom = Floats.changeNameBottomPadding),
                )
            }

            if (state.mediaItems.isNotEmpty()) {
                SegmentedControl(
                    titles = listOf(Strings.PARTICIPANTS_SEGMENT, Strings.ATTACHMENTS_SEGMENT),
                    selectedIndex = state.selectedSegment,
                    onSelect = { viewModel.send(ChatInfoPageReducer.Action.SegmentChanged(it)) }
                )
            }

            if (showMediaSegment) {
                MediaList(items = state.mediaItems, onTap = { previewIndex = it })
            } else if (state.isGroup && conversation != null) {
                ParticipantsCard(
                    participants = otherParticipants(conversation),
                    isExpanded = state.isExpanded,
                    onToggle = { viewModel.send(ChatInfoPageReducer.Action.ToggleExpanded) },
                    onAddContact = { /* Deferred: needs the contact selector page. */ },
                )

                LeaveRow(
                    enabled = otherParticipants(conversation).size > 2,
                    onClick = { viewModel.send(ChatInfoPageReducer.Action.LeaveTapped) },
                )
            } else if (conversation != null) {
                OneToOneActionsCard(
                    onBlock = { viewModel.send(ChatInfoPageReducer.Action.BlockTapped) },
                    onReport = { viewModel.send(ChatInfoPageReducer.Action.ReportTapped) },
                    onDelete = { viewModel.send(ChatInfoPageReducer.Action.DeleteTapped) },
                )
            }

            Spacer(modifier = Modifier.padding(bottom = Floats.bottomSpacerPadding))
        }

        previewIndex?.let { index ->
            MediaPreviewOverlay(
                mediaFiles = state.mediaItems.map { it.file },
                startIndex = index,
                onDismiss = { previewIndex = null },
            )
        }
    }
}

// MARK: - Header

@Composable
private fun ChatInfoHeader(
    conversation: Conversation?,
    isGroup: Boolean,
    onDone: () -> Unit,
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
            contentDescription = Strings.DONE,
            onClick = onDone,
            tint = colors.titleText,
            glyphSize = Floats.doneButtonGlyphSize,
        )
    }

    AvatarImageView(
        modifier = Modifier.padding(top = Floats.avatarTopPadding).size(Floats.avatarSize),
        imageData = conversation?.metadata?.imageData,
        fallbackSymbol = if (isGroup) "person.2" else "person",
        glyphSize = Floats.avatarGlyphSize,
    )

    Components.Text(
        conversation?.let { infoTitle(it) }.orEmpty(),
        color = colors.titleText,
        font = Font.systemBold(FontScale.Large),
        textAlign = TextAlign.Center,
        modifier =
            Modifier.padding(
                top = Floats.titleTopPadding,
                start = Floats.titleHorizontalPadding,
                end = Floats.titleHorizontalPadding,
            ),
    )
}

// MARK: - Segmented Control

@Composable
private fun SegmentedControl(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalPantherColors.current
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
                        .background(if (isSelected) colors.background else Color.Transparent)
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
private fun ParticipantsCard(
    participants: List<ParticipantRowData>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddContact: () -> Unit,
) {
    InfoCard {
        ParticipantsHeaderRow(
            count = participants.size,
            subtitle = participants.joinToString(Strings.PARTICIPANTS_SEPARATOR) { it.displayName },
            isExpanded = isExpanded,
            onToggle = onToggle,
        )
        if (isExpanded) {
            participants.forEach { participant ->
                CardDivider()
                ParticipantRow(participant)
            }
            CardDivider()
            AddContactRow(onAddContact)
        }
    }
}

@Composable
private fun ParticipantsHeaderRow(
    count: Int,
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
                "$count ${LocalizedStringKey.People.localized()}",
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

@Composable
private fun ParticipantRow(participant: ParticipantRowData) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.cardHorizontalPadding, vertical = Floats.rowVerticalPadding),
    ) {
        AvatarImageView(
            modifier = Modifier.size(Floats.rowAvatarSize),
            initials = participant.initials,
            glyphSize = Floats.rowAvatarGlyphSize,
            initialsFont = Font.systemSemibold(FontScale.Small),
        )
        Components.Text(
            participant.displayName,
            color = colors.titleText,
            font = Font.systemSemibold(),
            modifier = Modifier.padding(start = Floats.rowTextStartPadding),
        )
        participant.languageCode?.let { LanguageBadge(it, participant.regionCode) }
        Spacer(modifier = Modifier.weight(1f))
        Components.Symbol("chevron.right", color = colors.subtitleText, modifier = Modifier.size(Floats.chevronGlyphSize))
    }
}

@Composable
private fun LanguageBadge(
    languageCode: String,
    regionCode: String?,
) {
    val colors = LocalPantherColors.current
    val flag = regionCode?.let { RegionDetailService.emojiFlag(it) }.orEmpty()
    val label = languageCode.uppercase() + if (flag.isNotEmpty()) " $flag" else ""
    Box(
        modifier =
            Modifier
                .padding(start = Floats.languageBadgeStartPadding)
                .clip(RoundedCornerShape(Floats.languageBadgeCornerRadius))
                .background(colors.groupedContentBackground)
                .padding(
                    horizontal = Floats.languageBadgeHorizontalPadding,
                    vertical = Floats.languageBadgeVerticalPadding,
                ),
    ) {
        Components.Text(label, color = colors.subtitleText, font = Font.systemMedium(FontScale.Small))
    }
}

@Composable
private fun AddContactRow(onClick: () -> Unit) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Floats.cardHorizontalPadding, vertical = Floats.rowVerticalPadding),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(Floats.rowAvatarSize).clip(CircleShape).background(colors.groupedContentBackground),
        ) {
            Components.Symbol("plus", color = colors.accent, modifier = Modifier.size(Floats.rowAvatarGlyphSize))
        }
        Components.Text(Strings.ADD_CONTACT, color = colors.accent, modifier = Modifier.padding(start = Floats.rowTextStartPadding))
    }
}

// MARK: - Actions

@Composable
private fun LeaveRow(
    enabled: Boolean,
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
                .background(colors.background)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(Floats.cardPadding),
    ) {
        Components.Text(
            Strings.LEAVE_CONVERSATION,
            color = if (enabled) Colors.destructive else colors.subtitleText,
        )
    }
}

@Composable
private fun OneToOneActionsCard(
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit,
) {
    InfoCard {
        ActionCardRow(Strings.BLOCK, onBlock)
        CardDivider()
        ActionCardRow(Strings.REPORT, onReport)
        CardDivider()
        ActionCardRow(Strings.DELETE_CONVERSATION, onDelete)
    }
}

@Composable
private fun ActionCardRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Floats.cardPadding),
    ) {
        Components.Text(title, color = Colors.destructive)
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
                .background(colors.background),
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

private data class ParticipantRowData(
    val userID: String,
    val displayName: String,
    val initials: String,
    val languageCode: String?,
    val regionCode: String?,
)

private fun otherParticipants(conversation: Conversation): List<ParticipantRowData> =
    conversation.participants
        .filter { it.userID != User.currentUserID }
        .map { participant ->
            val match = ContactService.match(participant.userID)
            val user = conversation.users?.firstOrNull { it.id == participant.userID } ?: SessionStore.users[participant.userID]
            ParticipantRowData(
                userID = participant.userID,
                displayName = match?.fullName ?: user?.phoneNumber?.formattedString() ?: participant.userID,
                initials = match?.initials.orEmpty(),
                languageCode = user?.languageCode,
                regionCode = user?.phoneNumber?.regionCode,
            )
        }

private fun infoTitle(conversation: Conversation): String {
    val name = conversation.metadata.name
    if (!name.isBangQualifiedEmpty && name.isNotBlank()) return name

    val users = conversation.users.orEmpty()
    val first = users.firstOrNull() ?: return Strings.UNKNOWN
    val base = ContactService.match(first.id)?.fullName ?: first.phoneNumber.formattedString()
    return if (users.size > 1) "$base${Strings.TITLE_ADDITIONAL_SEPARATOR}${users.size - 1}" else base
}
