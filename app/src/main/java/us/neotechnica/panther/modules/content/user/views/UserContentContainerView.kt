//
//  UserContentContainerView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.UserContentContainerViewFloats
import us.neotechnica.panther.modules.content.user.constants.UserContentContainerViewStrings
import us.neotechnica.panther.modules.content.user.views.changelanguagepageview.ChangeLanguagePageView
import us.neotechnica.panther.modules.content.user.views.chatinfopageview.ChatInfoPageView
import us.neotechnica.panther.modules.content.user.views.chatpageview.ChatPageView
import us.neotechnica.panther.modules.content.user.views.conversationspageview.ConversationsPageReducer
import us.neotechnica.panther.modules.content.user.views.conversationspageview.ConversationsPageView
import us.neotechnica.panther.modules.content.user.views.conversationspageview.buildConversationsPageViewModel
import us.neotechnica.panther.modules.content.user.views.newchatpageview.NewChatPageView
import us.neotechnica.panther.modules.content.user.views.settingspageview.SettingsPageView
import us.neotechnica.panther.modules.content.user.views.usercontentcontainerview.UserContentContainerReducer
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

private typealias Floats = UserContentContainerViewFloats
private typealias Strings = UserContentContainerViewStrings

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

    val stackDepth = navigationState.userContent.stack.size
    val previousStackDepth = remember { mutableIntStateOf(stackDepth) }
    val isPush = stackDepth >= previousStackDepth.intValue
    SideEffect { previousStackDepth.intValue = stackDepth }

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

    Box(modifier = modifier.fillMaxSize()) {
        // The conversations list is the always-composed root layer, so it is
        // never disposed by a push and its scroll position survives.
        // Pushed pages animate in over it and are opaque.
        // Drawn edge-to-edge (no system-bar padding here) so the cell context
        // menu's dim scrim can cover the whole screen; the page insets its own
        // content below.
        ConversationsPageView(
            viewModel = conversationsViewModel,
            listState = conversationsListState,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedContent(
            contentKey = { it?.let { path -> path::class } },
            label = Strings.ANIMATION_LABEL,
            modifier = Modifier.fillMaxSize(),
            targetState = topPath,
            transitionSpec = {
                // A pure horizontal slide (no fade): the outgoing and incoming
                // pages are opaque and meet edge-to-edge, so they fully cover the
                // screen throughout the transition. Fading them would briefly make
                // both translucent, revealing the always-composed conversations
                // layer behind the overlay.
                val enter = slideInHorizontally(tween(Floats.TRANSITION_MILLIS)) { width -> if (isPush) width else -width }
                val exit = slideOutHorizontally(tween(Floats.TRANSITION_MILLIS)) { width -> if (isPush) -width else width }
                enter.togetherWith(exit).using(SizeTransform(clip = false))
            },
        ) { path ->
            // The overlay stays full-size in both states so `SizeTransform` never
            // shrinks the exiting page into a box – the pop is a clean horizontal
            // slide. A pushed page fills an opaque background to cover the base
            // layer; at the root the overlay is a transparent, non-interactive
            // pass-through so the conversations base layer shows and stays tappable.
            if (path == null) {
                Box(modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(colors.background)
                            .pointerInput(Unit) {
                                // Consume any touch the pushed page's own content leaves
                                // unhandled – the navigation-bar and input-bar negative space –
                                // so it never falls through to the always-composed conversations
                                // layer behind this opaque overlay. The page's own widgets still
                                // receive input first (children run before this parent on the main
                                // pass); only otherwise-unhandled touches are swallowed here.
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent().changes.forEach { it.consume() }
                                    }
                                }
                            },
                ) {
                    DestinationView(path)
                }
            }
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
            ChatPageView(path.conversationIDKey, path.focusedMessageID, Modifier.fillMaxSize())

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
