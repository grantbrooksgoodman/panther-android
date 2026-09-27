//
//  UserContentContainer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.UserContentContainerFloats
import us.neotechnica.panther.modules.content.user.constants.UserContentContainerStrings
import us.neotechnica.panther.modules.content.user.views.changelanguagepageview.ChangeLanguagePageView
import us.neotechnica.panther.modules.content.user.views.chatinfopageview.ChatInfoPageView
import us.neotechnica.panther.modules.content.user.views.chatpageview.ChatPageView
import us.neotechnica.panther.modules.content.user.views.conversationspageview.ConversationsPageReducer
import us.neotechnica.panther.modules.content.user.views.conversationspageview.ConversationsPageView
import us.neotechnica.panther.modules.content.user.views.conversationspageview.buildConversationsPageViewModel
import us.neotechnica.panther.modules.content.user.views.newchatpageview.NewChatPageView
import us.neotechnica.panther.modules.content.user.views.settingspageview.SettingsPageView
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Constants Accessors

private typealias Floats = UserContentContainerFloats
private typealias Strings = UserContentContainerStrings

/**
 * Hosts the signed-in content stack: the conversations list at the root,
 * pushing to the chat page. The system back gesture pops.
 *
 * @param modifier The modifier for this container.
 */
@Composable
fun UserContentContainer(modifier: Modifier = Modifier) {
    val navigation = remember { DependencyValues.current.navigation }
    val state by navigation.state.collectAsState()
    val topPath = state.userContent.stack.lastOrNull()
    val colors = LocalPantherColors.current

    BackHandler(enabled = topPath != null) {
        navigation.navigate(Route.UserContent(UserContentRoute.Pop))
    }

    val stackDepth = state.userContent.stack.size
    val previousStackDepth = remember { mutableIntStateOf(stackDepth) }
    val isPush = stackDepth >= previousStackDepth.intValue
    SideEffect { previousStackDepth.intValue = stackDepth }

    // The conversations list's view-model and scroll state are hoisted here,
    // where they survive pushes to a chat, so returning lands where the user
    // left off (and without re-loading), as on iOS. The container is composed
    // for the whole signed-in session, so these outlive the pushed pages.
    val conversationsViewModel = remember { buildConversationsPageViewModel() }
    val conversationsListState = rememberLazyListState()
    DisposableEffect(Unit) {
        conversationsViewModel.send(ConversationsPageReducer.Action.ViewFirstAppeared)
        onDispose { conversationsViewModel.close() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // The conversations list is the always-composed root layer, so it is
        // never disposed by a push and its scroll position survives, as on iOS.
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
                val enter =
                    slideInHorizontally(tween(Floats.TRANSITION_MILLIS)) { width -> if (isPush) width else -width } +
                        fadeIn(tween(Floats.TRANSITION_MILLIS))
                val exit =
                    slideOutHorizontally(tween(Floats.TRANSITION_MILLIS)) { width -> if (isPush) -width else width } +
                        fadeOut(tween(Floats.TRANSITION_MILLIS))
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
                Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
                    when (path) {
                        // Drawn edge-to-edge so the context-menu scrim covers the system bars; the page
                        // insets its own content.
                        is UserContentNavigatorState.SeguePath.Chat ->
                            ChatPageView(path.conversationIDKey, Modifier.fillMaxSize())

                        // Drawn edge-to-edge so its grouped background bleeds past the system bars,
                        // matching iOS; the page insets its own content.
                        is UserContentNavigatorState.SeguePath.ChatInfo ->
                            ChatInfoPageView(path.conversationIDKey, Modifier.fillMaxSize())

                        // Drawn edge-to-edge so its grouped background bleeds past the system bars,
                        // matching iOS; the page insets its own content.
                        UserContentNavigatorState.SeguePath.NewChat ->
                            NewChatPageView(Modifier.fillMaxSize())

                        // Drawn edge-to-edge so its grouped background bleeds past the system bars,
                        // matching iOS; the page insets its own content.
                        UserContentNavigatorState.SeguePath.Settings ->
                            SettingsPageView(Modifier.fillMaxSize())

                        // Drawn edge-to-edge so its grouped background bleeds past the system bars,
                        // matching iOS; the page insets its own content.
                        UserContentNavigatorState.SeguePath.ChangeLanguage ->
                            ChangeLanguagePageView(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
