//
//  ConversationCellView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.conversationcellview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.bundle.conversationsPageReappeared
import us.neotechnica.panther.bundle.conversationsSearchQuery
import us.neotechnica.panther.bundle.reloadingConversationIDKeys
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageContextMenu
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.TextFit
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.AvatarImageView
import us.neotechnica.panther.modules.content.user.components.UserInfoBadgeView
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewColors
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewFloats
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewStrings
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates
import kotlin.math.roundToInt
import androidx.compose.material3.Text as Material3Text

// MARK: - Constants Accessors

private typealias CellColors = ConversationCellViewColors
private typealias CellFloats = ConversationCellViewFloats
private typealias CellStrings = ConversationCellViewStrings

// MARK: - Types

private enum class SwipeAnchor {
    LEADING,
    SETTLED,
    TRAILING,
}

/**
 * A list cell summarizing a conversation on the conversations page.
 *
 * Displays a conversation's avatar, title, latest message preview,
 * and date, with an unread indicator. Tapping the cell opens the chat
 * page, and a long press shows a menu offering to delete the
 * conversation or block or report its users; swiping reveals the same
 * actions. The cell redacts its content – and disables itself – while
 * messages hydrate or the conversation reloads.
 *
 * @param viewModel The view model that drives the cell.
 * @param modifier The modifier for this cell.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationCellView(
    viewModel: ViewModel<ConversationCellReducer.State, ConversationCellReducer.Action>,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    LaunchedEffect(viewModel) { viewModel.send(ConversationCellReducer.Action.ViewAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val isShowingRedactedContent = state.isShowingRedactedContent

    val density = LocalDensity.current
    val swipeActionButtonWidth = with(density) { CellFloats.swipeActionButtonWidth.toPx() }
    val draggableState =
        remember {
            AnchoredDraggableState(
                initialValue = SwipeAnchor.SETTLED,
                anchors =
                    DraggableAnchors {
                        SwipeAnchor.LEADING at swipeActionButtonWidth * LEADING_SWIPE_ACTION_COUNT
                        SwipeAnchor.SETTLED at 0f
                        SwipeAnchor.TRAILING at -swipeActionButtonWidth
                    },
            )
        }
    val swipeOffset = draggableState.offset.takeIf { !it.isNaN() } ?: 0f

    Box(modifier = modifier.fillMaxWidth().height(CellFloats.frameHeight)) {
        SwipeActionButtons(
            state = state,
            swipeOffset = swipeOffset,
            onBlock = { viewModel.send(ConversationCellReducer.Action.BlockUsersButtonTapped) },
            onReport = { viewModel.send(ConversationCellReducer.Action.ReportUsersButtonTapped) },
            onDelete = { viewModel.send(ConversationCellReducer.Action.DeleteConversationButtonTapped) },
        )

        Box(
            modifier =
                Modifier
                    .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                    .background(colors.background)
                    .anchoredDraggable(draggableState, Orientation.Horizontal, enabled = !isShowingRedactedContent),
        ) {
            MessageContextMenu(
                liftedBackground = colors.background,
                menuLeadingOffset = CellFloats.contextMenuLeadingOffset,
                actions =
                    listOf(
                        ContextMenuAction(
                            title = state.deleteConversationButtonText,
                            systemImageName = CellStrings.DELETE_CONVERSATION_BUTTON_IMAGE_SYSTEM_NAME,
                            isDestructive = true,
                        ) { viewModel.send(ConversationCellReducer.Action.DeleteConversationButtonTapped) },
                        ContextMenuAction(
                            title = state.blockUsersButtonText,
                            systemImageName = CellStrings.BLOCK_USERS_BUTTON_IMAGE_SYSTEM_NAME,
                        ) { viewModel.send(ConversationCellReducer.Action.BlockUsersButtonTapped) },
                        ContextMenuAction(
                            title = state.reportUsersButtonText,
                            systemImageName = CellStrings.REPORT_USERS_BUTTON_IMAGE_SYSTEM_NAME,
                        ) { viewModel.send(ConversationCellReducer.Action.ReportUsersButtonTapped) },
                    ),
                alignment = ContextMenuAlignment.LEADING,
                reactionChoices = emptyList(),
                liftScale = 0f,
                onTap = if (isShowingRedactedContent) null else ({ viewModel.send(ConversationCellReducer.Action.CellTapped) }),
            ) {
                CellView(
                    state = state,
                    onUserInfoBadgeTapped = { viewModel.send(ConversationCellReducer.Action.UserInfoBadgeTapped) },
                )
            }
        }
    }
}

/**
 * Builds a conversation cell view model for the conversation with the
 * given ID key.
 *
 * The cell observes the following for the lifetime of the view model:
 * conversations page reappearance and session store changes, which
 * trigger reloads; changes to the shared search query; and changes to
 * the set of reloading conversations.
 *
 * @param conversationIDKey The ID key of the conversation the cell
 *   describes.
 * @param searchQuery The search query active when the cell is
 *   created.
 *
 * @return The observing view model.
 */
fun buildConversationCellViewModel(
    conversationIDKey: String,
    searchQuery: String,
): ViewModel<ConversationCellReducer.State, ConversationCellReducer.Action> =
    ViewModel(
        ConversationCellReducer.State(conversationIDKey = conversationIDKey, searchQuery = searchQuery),
        ConversationCellReducer(),
    ).observing(DependencyValues.current.sharedEvents.conversationsPageReappeared.events) {
        ConversationCellReducer.Action.ReloadData
    }.observing(DependencyValues.current.sharedStates.conversationsSearchQuery.changes) {
        // The replayed current value is deduplicated by the reducer; cells
        // are created with the query already active at that moment.
        ConversationCellReducer.Action.SearchQueryChanged(it)
    }.observing(DependencyValues.current.sharedStates.reloadingConversationIDKeys.changes) {
        // The replayed current value is kept so that cells appearing
        // mid-refresh redact immediately.
        ConversationCellReducer.Action.ReloadingConversationsChanged(it)
    }.observing(DependencyValues.current.sharedEvents.sessionStoreDidChange.events) {
        ConversationCellReducer.Action.SessionStoreDidChange(it)
    }

// MARK: - Swipe Action Buttons

@Composable
private fun SwipeActionButtons(
    state: ConversationCellReducer.State,
    swipeOffset: Float,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        if (swipeOffset > 0f) {
            SwipeActionButton(
                systemName = CellStrings.BLOCK_USERS_BUTTON_IMAGE_SYSTEM_NAME,
                tint = CellColors.blockUsersButtonImageTint,
                contentDescription = state.blockUsersButtonText,
                onClick = onBlock,
            )
            SwipeActionButton(
                systemName = CellStrings.REPORT_USERS_BUTTON_IMAGE_SYSTEM_NAME,
                tint = CellColors.reportUsersButtonImageTint,
                contentDescription = state.reportUsersButtonText,
                onClick = onReport,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (swipeOffset < 0f) {
            SwipeActionButton(
                systemName = CellStrings.DELETE_CONVERSATION_BUTTON_IMAGE_SYSTEM_NAME,
                tint = CellColors.deleteConversationButtonImageTint,
                contentDescription = state.deleteConversationButtonText,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun SwipeActionButton(
    systemName: String,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .width(CellFloats.swipeActionButtonWidth)
                .fillMaxHeight()
                .background(tint)
                .clickable(onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
    ) {
        Components.Symbol(systemName, foregroundColor = Color.White)
    }
}

// MARK: - Cell View

@Composable
private fun CellView(
    state: ConversationCellReducer.State,
    onUserInfoBadgeTapped: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val cellViewData = state.cellViewData
    val isShowingRedactedContent = state.isShowingRedactedContent
    val contentAlpha = if (isShowingRedactedContent) REDACTED_CONTENT_ALPHA else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CellFloats.frameHeight)
                .padding(start = CellFloats.rowStartPadding, end = CellFloats.rowEndPadding),
    ) {
        Box(modifier = Modifier.width(0.dp)) {
            Box(
                modifier =
                    Modifier
                        .offset(x = CellFloats.unreadIndicatorViewXOffset, y = CellFloats.unreadIndicatorViewYOffset)
                        .size(width = CellFloats.unreadIndicatorViewFrameWidth, height = CellFloats.unreadIndicatorViewFrameHeight)
                        .clip(CircleShape)
                        .background(CellColors.unreadIndicatorViewForeground)
                        .alpha(if (cellViewData.isShowingUnreadIndicator && !isShowingRedactedContent) 1f else 0f),
            )
        }

        AvatarImageView(
            remember(cellViewData.thumbnailImage) { cellViewData.thumbnailImage?.asImageBitmap() },
            badgeCount = if (isShowingRedactedContent) 0 else state.conversation.participants.size - 1,
            isRedacted = isShowingRedactedContent,
            modifier = Modifier.padding(top = CellFloats.avatarImageViewTopPadding),
        )

        Spacer(modifier = Modifier.width(CellFloats.avatarContentSpacing))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Components.FittedText(
                        cellViewData.titleLabelText,
                        foregroundColor = colors.titleText,
                        fit =
                            TextFit(
                                maxLines = 1,
                                minimumScaleFactor = CellFloats.TITLE_LABEL_MINIMUM_SCALE_FACTOR,
                                overflow = TextOverflow.Ellipsis,
                            ),
                        font =
                            Font.systemSemibold(
                                FontScale.Custom(
                                    if (isShowingRedactedContent) {
                                        CellFloats.REDACTED_TITLE_LABEL_SYSTEM_FONT_SIZE
                                    } else {
                                        CellFloats.TITLE_LABEL_SYSTEM_FONT_SIZE
                                    },
                                ),
                            ),
                        modifier = Modifier.weight(1f, fill = false).padding(bottom = CellFloats.titleLabelBottomPadding),
                    )

                    cellViewData.otherUser?.let { otherUser ->
                        Box(modifier = Modifier.alpha(if (isShowingRedactedContent) 0f else 1f)) {
                            UserInfoBadgeView(user = otherUser, action = onUserInfoBadgeTapped)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CellFloats.chevronImageAndDateLabelHStackSpacing),
                ) {
                    Components.Text(
                        state.dateLabelText,
                        foregroundColor = colors.subtitleText,
                        font = Font.system(FontScale.Custom(CellFloats.DATE_LABEL_SYSTEM_FONT_SIZE)),
                        modifier = Modifier.padding(end = CellFloats.dateLabelPaddingTrailing).alpha(contentAlpha),
                    )

                    Components.Symbol(
                        CellStrings.CHEVRON_IMAGE_SYSTEM_NAME,
                        foregroundColor = state.chevronImageForegroundColor(colors.titleText, isDarkModeActive),
                        weight = FontWeight.SemiBold,
                        usesIntrinsicSize = false,
                        modifier =
                            Modifier
                                .size(width = CellFloats.chevronImageFrameMaxWidth, height = CellFloats.chevronImageFrameMaxHeight)
                                .alpha(if (isShowingRedactedContent) 0f else 1f),
                    )
                }
            }

            Material3Text(
                state.subtitleLabelText,
                color = state.subtitleLabelTextForegroundColor(colors.subtitleText),
                style = Font.system(FontScale.Custom(CellFloats.SUBTITLE_LABEL_SYSTEM_FONT_SIZE)).textStyle,
                minLines = CellFloats.SUBTITLE_LABEL_LINE_LIMIT,
                maxLines = CellFloats.SUBTITLE_LABEL_LINE_LIMIT,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .offset(x = CellFloats.subtitleLabelXOffset, y = CellFloats.subtitleLabelYOffset)
                        .alpha(contentAlpha),
            )
        }
    }
}

private const val LEADING_SWIPE_ACTION_COUNT = 2
private const val REDACTED_CONTENT_ALPHA = 0.5f
