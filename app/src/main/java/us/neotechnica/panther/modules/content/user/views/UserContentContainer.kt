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
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import us.neotechnica.panther.modules.content.user.views.chatpageview.ChatPageView
import us.neotechnica.panther.modules.content.user.views.conversationspageview.ConversationsPageView
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

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

    BackHandler(enabled = topPath != null) {
        navigation.navigate(Route.UserContent(UserContentRoute.Pop))
    }

    AnimatedContent(
        contentKey = { it?.let { path -> path::class } },
        label = "UserContentContainer",
        modifier = modifier.fillMaxSize(),
        targetState = topPath,
        transitionSpec = {
            (slideInHorizontally(tween(TRANSITION_MILLIS)) { it } + fadeIn(tween(TRANSITION_MILLIS)))
                .togetherWith(
                    slideOutHorizontally(tween(TRANSITION_MILLIS)) { -it } + fadeOut(tween(TRANSITION_MILLIS)),
                ).using(SizeTransform(clip = false))
        },
    ) { path ->
        when (path) {
            is UserContentNavigatorState.SeguePath.Chat ->
                ChatPageView(path.conversationIDKey, Modifier.fillMaxSize())

            else -> ConversationsPageView(Modifier.fillMaxSize())
        }
    }
}

private const val TRANSITION_MILLIS = 250
