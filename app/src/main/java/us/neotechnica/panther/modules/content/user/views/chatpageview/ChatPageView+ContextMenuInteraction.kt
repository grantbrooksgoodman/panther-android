//
//  ChatPageView+ContextMenuInteraction.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull
import us.neotechnica.panther.designsystem.modules.componentkit.components.LocalContextMenuController
import kotlin.time.Duration.Companion.seconds

// How long after a context menu leaves the screen the list follows the
// returning keyboard.
private val KEYBOARD_RETURN_DURATION = 1.seconds

/**
 * Keeps the newest message in view as the keyboard returns after a
 * context menu.
 *
 * A context menu hides the keyboard while it is on screen. If the
 * list rested at its newest message when the menu hid the keyboard,
 * the list follows the keyboard back up once the menu finishes
 * animating away, rather than leaving the newest message behind it.
 *
 * @param listState The message list's scroll state.
 * @param lastIndex The index of the newest message.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContextMenuInteractionEffect(
    listState: LazyListState,
    lastIndex: Int,
) {
    val controller = LocalContextMenuController.current
    val currentLastIndex by rememberUpdatedState(lastIndex)
    val isImeVisible by rememberUpdatedState(WindowInsets.isImeVisible)

    LaunchedEffect(controller, listState) {
        if (controller == null) return@LaunchedEffect
        var scrollsToLastItem = false
        snapshotFlow { controller.isDisplayed }.collectLatest { isMenuDisplayed ->
            if (isMenuDisplayed) {
                scrollsToLastItem = isImeVisible && !listState.canScrollForward
                return@collectLatest
            }

            if (!scrollsToLastItem) return@collectLatest
            scrollsToLastItem = false

            // Re-pin as the rising keyboard shrinks the list, unless the
            // user is scrolling it.
            withTimeoutOrNull(KEYBOARD_RETURN_DURATION) {
                snapshotFlow { listState.layoutInfo.viewportSize.height }.collect {
                    if (listState.isScrollInProgress || currentLastIndex < 0) return@collect
                    listState.scrollToItem(currentLastIndex)
                }
            }
        }
    }
}
