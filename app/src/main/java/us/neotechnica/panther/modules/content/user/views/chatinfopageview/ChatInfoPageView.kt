//
//  ChatInfoPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.services.RegionDetailService
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

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState()),
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            ) {
                DoneButton(onClick = { viewModel.send(ChatInfoPageReducer.Action.BackTapped) })
            }

            InfoAvatar(imageData = conversation?.metadata?.imageData, isGroup = state.isGroup)

            Components.Text(
                conversation?.let { infoTitle(it) }.orEmpty(),
                color = colors.titleText,
                font = Font.systemBold(FontScale.Large),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp),
            )

            if (state.isGroup && conversation != null) {
                Components.CapsuleButton(
                    "Change name and photo",
                    onClick = { viewModel.send(ChatInfoPageReducer.Action.ChangeMetadataTapped) },
                    primary = true,
                    modifier = Modifier.padding(top = 12.dp).padding(bottom = 5.dp),
                )

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

            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

// MARK: - Header

@Composable
private fun DoneButton(onClick: () -> Unit) {
    val colors = LocalPantherColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.background)
                .clickable(onClick = onClick),
    ) {
        Components.Symbol("checkmark", color = colors.titleText, modifier = Modifier.size(20.dp))
    }
}

// MARK: - Avatar

@Composable
private fun InfoAvatar(
    imageData: ByteArray?,
    isGroup: Boolean,
) {
    val colors = LocalPantherColors.current
    val image =
        remember(imageData) {
            imageData?.takeIf { it.isNotEmpty() }?.let { bytes ->
                runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
            }
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .padding(top = 20.dp)
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(AVATAR_BACKGROUND),
    ) {
        AvatarContent(image, isGroup, colors.background)
    }
}

@Composable
private fun AvatarContent(
    image: ImageBitmap?,
    isGroup: Boolean,
    glyphColor: Color,
) {
    when {
        image != null ->
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

        isGroup -> Components.Symbol("person.2", color = glyphColor, modifier = Modifier.size(AVATAR_GLYPH_SIZE))
        else -> Components.Symbol("person", color = glyphColor, modifier = Modifier.size(AVATAR_GLYPH_SIZE))
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
            subtitle = participants.joinToString(", ") { it.displayName },
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(CARD_PADDING),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Components.Text("$count ${LocalizedStringKey.People.localized()}", color = colors.titleText, font = Font.systemBold())
            Material3Text(
                subtitle,
                color = colors.subtitleText,
                style = Font.system(FontScale.Small).textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .padding(start = 8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.dp, colors.subtitleText, CircleShape),
        ) {
            Components.Symbol(
                if (isExpanded) "chevron.down" else "chevron.right",
                color = colors.subtitleText,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ParticipantRow(participant: ParticipantRowData) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = CARD_HORIZONTAL_PADDING, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ROW_AVATAR_SIZE).clip(CircleShape).background(AVATAR_BACKGROUND),
        ) {
            if (participant.initials.isNotBlank()) {
                Components.Text(participant.initials, color = colors.background, font = Font.systemSemibold(FontScale.Small))
            } else {
                Components.Symbol("person", color = colors.background, modifier = Modifier.size(ROW_AVATAR_GLYPH_SIZE))
            }
        }
        Components.Text(
            participant.displayName,
            color = colors.titleText,
            font = Font.systemSemibold(),
            modifier = Modifier.padding(start = 12.dp),
        )
        participant.languageCode?.let { LanguageBadge(it, participant.regionCode) }
        Spacer(modifier = Modifier.weight(1f))
        Components.Symbol("chevron.right", color = colors.subtitleText, modifier = Modifier.size(16.dp))
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
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.groupedContentBackground)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Components.Text(label, color = colors.subtitleText, font = Font.systemMedium(FontScale.Small))
    }
}

@Composable
private fun AddContactRow(onClick: () -> Unit) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = CARD_HORIZONTAL_PADDING, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ROW_AVATAR_SIZE).clip(CircleShape).background(colors.groupedContentBackground),
        ) {
            Components.Symbol("plus", color = colors.accent, modifier = Modifier.size(ROW_AVATAR_GLYPH_SIZE))
        }
        Components.Text("Add Contact", color = colors.accent, modifier = Modifier.padding(start = 12.dp))
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
                .padding(horizontal = CARD_MARGIN, vertical = 8.dp)
                .clip(RoundedCornerShape(CARD_CORNER))
                .background(colors.background)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(CARD_PADDING),
    ) {
        Components.Text(
            "Leave this Conversation",
            color = if (enabled) DESTRUCTIVE_COLOR else colors.subtitleText,
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
        ActionCardRow("Block", onBlock)
        CardDivider()
        ActionCardRow("Report", onReport)
        CardDivider()
        ActionCardRow("Delete this Conversation", onDelete)
    }
}

@Composable
private fun ActionCardRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(CARD_PADDING),
    ) {
        Components.Text(title, color = DESTRUCTIVE_COLOR)
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
                .padding(horizontal = CARD_MARGIN, vertical = 8.dp)
                .clip(RoundedCornerShape(CARD_CORNER))
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
        modifier = Modifier.padding(start = CARD_HORIZONTAL_PADDING),
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
    val first = users.firstOrNull() ?: return "Unknown"
    val base = ContactService.match(first.id)?.fullName ?: first.phoneNumber.formattedString()
    return if (users.size > 1) "$base + ${users.size - 1}" else base
}

private val AVATAR_SIZE = 100.dp
private val AVATAR_GLYPH_SIZE = 52.dp
private val ROW_AVATAR_SIZE = 40.dp
private val ROW_AVATAR_GLYPH_SIZE = 22.dp
private val CARD_MARGIN = 16.dp
private val CARD_CORNER = 16.dp
private val CARD_PADDING = 16.dp
private val CARD_HORIZONTAL_PADDING = 16.dp
private val AVATAR_BACKGROUND = Color(0xFFC7C7CC)
private val DESTRUCTIVE_COLOR = Color(0xFFFF3B30)
