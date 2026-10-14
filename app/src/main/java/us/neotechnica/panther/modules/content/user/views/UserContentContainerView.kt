//
//  UserContentContainerView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.designsystem.modules.foundation.components.NavigationStackView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.views.changelanguagepageview.ChangeLanguagePageView
import us.neotechnica.panther.modules.content.user.views.chatinfopageview.ChatInfoPageView
import us.neotechnica.panther.modules.content.user.views.chatpageview.ChatPageView
import us.neotechnica.panther.modules.content.user.views.conversationspageview.ConversationsPageReducer
import us.neotechnica.panther.modules.content.user.views.conversationspageview.ConversationsPageView
import us.neotechnica.panther.modules.content.user.views.conversationspageview.buildConversationsPageViewModel
import us.neotechnica.panther.modules.content.user.views.newchatpageview.NewChatPageView
import us.neotechnica.panther.modules.content.user.views.settingspageview.SettingsPageView
import us.neotechnica.panther.modules.content.user.views.usercontentcontainerview.UserContentContainerReducer
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

/**
 * Hosts the signed-in user's content: the conversations list at the
 * root, pushing to the chat page and its siblings. The system back
 * gesture pops. The container rebuilds its content when the current
 * conversation's metadata changes.
 *
 * @param modifier The modifier for this container.
 */
@Composable
fun UserContentContainerView(modifier: Modifier = Modifier) {
    val navigation = remember { DependencyValues.current.navigation }
    val navigationState by navigation.state.collectAsState()
    val topPath = navigationState.userContent.stack.lastOrNull()
    val colors = LocalPantherColors.current

    val viewModel = remember { buildUserContentContainerViewModel() }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    val state by viewModel.state.collectAsState()

    BackHandler(enabled = topPath != null) {
        navigation.navigate(Route.UserContent(UserContentRoute.Pop))
    }

    // The conversations list's view-model and scroll state are hoisted here,
    // where they survive pushes to a chat, so returning lands where the user
    // left off (and without re-loading). The container is composed
    // for the whole signed-in session, so these outlive the pushed pages.
    val conversationsViewModel = remember { buildConversationsPageViewModel() }
    val conversationsListState = rememberLazyListState()
    DisposableEffect(Unit) {
        conversationsViewModel.send(ConversationsPageReducer.Action.ViewFirstAppeared)
        onDispose { conversationsViewModel.close() }
    }

    // The conversations list is always composed; map its coverage by a pushed
    // page to an appear/disappear lifecycle so returning to the root
    // refreshes it.
    LaunchedEffect(topPath == null) {
        conversationsViewModel.send(
            if (topPath == null) {
                ConversationsPageReducer.Action.ViewAppeared
            } else {
                ConversationsPageReducer.Action.ViewDisappeared
            },
        )
    }

    // Reading the identity rebuilds the content from the latest values
    // after a metadata change.
    state.objectID

    // The conversations list is the stack's persistent root, so it is never
    // disposed by a push and its scroll position survives; pushed pages
    // slide in over it. Drawn edge-to-edge (no system-bar padding here) so
    // the cell context menu's dim scrim can cover the whole screen; the page
    // insets its own content.
    NavigationStackView(
        path = navigationState.userContent.stack,
        modifier = modifier,
        isRootPersistent = true,
        root = { rootModifier ->
            ConversationsPageView(
                viewModel = conversationsViewModel,
                listState = conversationsListState,
                isCovered = topPath != null,
                modifier = rootModifier,
            )
        },
    ) { path ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .pointerInput(Unit) {
                        // Claim every touch on this opaque page – including the
                        // navigation-bar and input-bar negative space – so none falls
                        // through to the always-composed conversations layer behind it;
                        // hit testing stops at the topmost layer that handles pointer
                        // input. The touches are left unconsumed: consuming them here, as
                        // an ancestor of the page's content, would cancel any scroll in
                        // the page that has yet to pass its touch slop.
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent()
                        }
                    },
        ) {
            DestinationView(path)
        }
    }
}

// MARK: - Auxiliary

@Composable
private fun DestinationView(path: UserContentNavigatorState.SeguePaths) {
    // Every destination is drawn edge-to-edge so its background bleeds past
    // the system bars (and the context-menu scrim covers them); each page
    // insets its own content.
    when (path) {
        is UserContentNavigatorState.SeguePaths.Chat ->
            ChatPageView(
                conversationIDKey = path.conversationIDKey,
                configuration = ChatPageView.Configuration.Default(focusedMessageID = path.focusedMessageID),
                modifier = Modifier.fillMaxSize(),
            )

        is UserContentNavigatorState.SeguePaths.ChatInfo ->
            ChatInfoPageView(path.conversationIDKey, Modifier.fillMaxSize())

        UserContentNavigatorState.SeguePaths.NewChat ->
            NewChatPageView(Modifier.fillMaxSize())

        UserContentNavigatorState.SeguePaths.Settings ->
            SettingsPageView(Modifier.fillMaxSize())

        UserContentNavigatorState.SeguePaths.ChangeLanguage ->
            ChangeLanguagePageView(Modifier.fillMaxSize())
    }
}

private fun buildUserContentContainerViewModel(): ViewModel<UserContentContainerReducer.State, UserContentContainerReducer.Action> =
    ViewModel(UserContentContainerReducer.State(), UserContentContainerReducer())
        .observing(DependencyValues.current.sharedEvents.currentConversationMetadataChanged.events) {
            UserContentContainerReducer.Action.ConversationMetadataChanged
        }
