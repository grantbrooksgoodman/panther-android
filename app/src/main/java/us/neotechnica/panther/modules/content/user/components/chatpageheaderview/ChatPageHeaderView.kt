//
//  ChatPageHeaderView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.chatpageheaderview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.zIndex
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.designsystem.R
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.AvatarImageView
import us.neotechnica.panther.modules.content.user.constants.ChatPageHeaderViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageHeaderViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageHeaderViewStrings
import us.neotechnica.panther.modules.content.user.extensions.chatPageHeaderLabelText
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

private typealias Colors = ChatPageHeaderViewColors
private typealias Floats = ChatPageHeaderViewFloats
private typealias Strings = ChatPageHeaderViewStrings

/**
 * The header displayed at the top of the chat page.
 *
 * Shows the current conversation's avatar and title with a back
 * button and a chat info button, reloading automatically when the
 * session store reports a change affecting the conversation.
 *
 * @param conversationIDKey The identifier key of the conversation.
 * @param modifier The modifier for this view.
 */
@Composable
fun ChatPageHeaderView(
    conversationIDKey: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember(conversationIDKey) { buildChatPageHeaderViewModel(conversationIDKey) }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    LaunchedEffect(conversationIDKey) { viewModel.send(ChatPageHeaderReducer.Action.ReloadData) }

    val state by viewModel.state.collectAsState()

    // Reading the identity re-evaluates the content after a reload.
    state.viewID

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = Floats.contentViewVerticalPadding),
    ) {
        ChatInfoButton(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter),
            onClick = { viewModel.send(ChatPageHeaderReducer.Action.ChatInfoButtonTapped) },
        )

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Floats.backButtonViewHorizontalPadding)) {
            BackButton(onClick = { viewModel.send(ChatPageHeaderReducer.Action.BackButtonTapped) })
        }
    }
}

internal fun buildChatPageHeaderViewModel(
    conversationIDKey: String,
): ViewModel<ChatPageHeaderReducer.State, ChatPageHeaderReducer.Action> =
    ViewModel(ChatPageHeaderReducer.State(conversationIDKey = conversationIDKey), ChatPageHeaderReducer())
        .observing(DependencyValues.current.sharedEvents.sessionStoreDidChange.events) {
            ChatPageHeaderReducer.Action.SessionStoreDidChange(it)
        }

// MARK: - Back Button

// A plain back chevron at the leading edge of its tap target.
@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            Modifier
                .size(Floats.backButtonFrameSize)
                .clickable(
                    indication = null,
                    interactionSource = null,
                    onClick = onClick,
                ),
    ) {
        Icon(
            contentDescription = "Back",
            painter = painterResource(R.drawable.back_chevron),
            tint = LocalPantherColors.current.navigationBarButton,
        )
    }
}

// MARK: - Chat Info Button

@Composable
private fun ChatInfoButton(
    state: ChatPageHeaderReducer.State,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val conversation = state.conversation
    val cellViewData = state.cellViewData
    val badgeCount = if (conversation.participants.size > Floats.AVATAR_IMAGE_VIEW_BADGE_COUNT_COMPARATOR) -1 else 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .clickable(onClick = onClick)
                .semantics { contentDescription = "Conversation info" },
    ) {
        AvatarImageView(
            remember(cellViewData.thumbnailImage) { cellViewData.thumbnailImage?.asImageBitmap() },
            badgeCount = badgeCount,
            size = DpSize(Floats.avatarImageSize, Floats.avatarImageSize),
            modifier =
                Modifier
                    .background(Colors.avatarImageViewBackground, CircleShape)
                    .zIndex(1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .offset(y = Floats.avatarViewSpacing)
                    .padding(horizontal = Floats.avatarViewNameLabelViewHorizontalPadding)
                    .clip(CircleShape)
                    .background(colors.groupedContentBackground)
                    .padding(Floats.avatarViewNameLabelGlassEffectPadding),
        ) {
            Components.Text(
                conversation.chatPageHeaderLabelText ?: cellViewData.titleLabelText,
                foregroundColor = colors.titleText,
                font = Font.systemSemibold(FontScale.Custom(Floats.AVATAR_VIEW_NAME_LABEL_FONT_SIZE)),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Floats.avatarViewNameLabelTextHorizontalPadding),
            )

            Components.Symbol(
                Strings.AVATAR_VIEW_NAME_LABEL_CHEVRON_SYMBOL_IMAGE_SYSTEM_NAME,
                foregroundColor = Colors.avatarViewNameLabelChevronSymbolForeground,
                weight = FontWeight.Black,
                usesIntrinsicSize = false,
                modifier =
                    Modifier
                        .padding(start = Floats.avatarViewNameLabelSpacing)
                        .size(Floats.avatarViewNameLabelChevronSymbolFrameSize),
            )
        }
    }
}
