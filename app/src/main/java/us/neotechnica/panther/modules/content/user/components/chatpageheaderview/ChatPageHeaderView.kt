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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageHeaderViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageHeaderViewStrings
import us.neotechnica.panther.modules.content.user.extensions.chatPageHeaderLabelText
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

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
    val colors = LocalPantherColors.current

    val conversation = state.conversation
    val isGroup = conversation.participants.size > 2
    val title = conversation.chatPageHeaderLabelText ?: state.cellViewData?.title ?: ""

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.horizontalPadding, vertical = Floats.verticalPadding),
    ) {
        CircleChipButton(
            systemName = Strings.BACK_BUTTON_IMAGE_SYSTEM_NAME,
            contentDescription = "Back",
            onClick = { viewModel.send(ChatPageHeaderReducer.Action.BackButtonTapped) },
            modifier = Modifier.align(Alignment.CenterStart),
            tint = colors.titleText,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .clickable { viewModel.send(ChatPageHeaderReducer.Action.ChatInfoButtonTapped) }
                    .semantics { contentDescription = "Conversation info" },
        ) {
            AvatarImageView(
                modifier = Modifier.size(Floats.avatarSize).zIndex(1f),
                imageData = conversation.metadata.imageData,
                fallbackSymbol = if (isGroup) Strings.GROUP_AVATAR_FALLBACK_SYMBOL else Strings.AVATAR_FALLBACK_SYMBOL,
                glyphSize = Floats.avatarGlyphSize,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .offset(y = -Floats.avatarPillOverlap)
                        .clip(RoundedCornerShape(Floats.pillCornerRadius))
                        .background(colors.groupedContentBackground)
                        .padding(
                            start = Floats.pillStartPadding,
                            end = Floats.pillEndPadding,
                            top = Floats.pillVerticalPadding,
                            bottom = Floats.pillVerticalPadding,
                        ),
            ) {
                Components.Text(title.ifBlank { " " }, color = colors.titleText, font = Font.systemSemibold())
                Components.Symbol(
                    Strings.CHAT_INFO_CHEVRON_IMAGE_SYSTEM_NAME,
                    color = colors.subtitleText,
                    modifier = Modifier.size(Floats.pillChevronSize).padding(start = Floats.pillChevronStartPadding),
                )
            }
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
