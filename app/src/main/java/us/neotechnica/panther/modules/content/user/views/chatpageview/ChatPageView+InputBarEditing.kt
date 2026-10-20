//
//  ChatPageView+InputBarEditing.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import us.neotechnica.panther.modules.content.user.dependencies.inputBarService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import kotlin.time.Duration.Companion.milliseconds

// How long after editing begins the list scrolls to the newest message,
// which leaves the keyboard time to rise.
private val BEGIN_EDITING_SCROLL_DELAY = 200.milliseconds

// How long after the text changes the list scrolls to the newest
// message, which leaves the input bar time to lay out the new text.
private val TEXT_CHANGE_SCROLL_DELAY = 50.milliseconds

/**
 * Scrolls the message list to the newest message as editing
 * begins in the input bar, and as the input bar's text changes.
 *
 * The list scrolls even if the user had scrolled back to earlier
 * messages. As editing begins, it waits for the keyboard to rise
 * before scrolling; a page opened at a focused message stays at
 * that message instead.
 *
 * @param listState The message list's scroll state.
 * @param lastIndex The index of the newest message.
 * @param scrollsToLastItemOnBeginEditing Whether the list scrolls
 *   to the newest message as editing begins.
 */
@Composable
internal fun InputBarEditingEffect(
    listState: LazyListState,
    lastIndex: Int,
    scrollsToLastItemOnBeginEditing: Boolean,
) {
    val inputBarService = remember { DependencyValues.current.inputBarService }
    val currentLastIndex by rememberUpdatedState(lastIndex)

    LaunchedEffect(listState, scrollsToLastItemOnBeginEditing) {
        if (!scrollsToLastItemOnBeginEditing) return@LaunchedEffect
        inputBarService.editingDidBegin.collectLatest {
            delay(BEGIN_EDITING_SCROLL_DELAY)
            listState.animateScrollToLastItem(currentLastIndex)
        }
    }

    // The text already in the input bar is not a change.
    LaunchedEffect(listState) {
        inputBarService.text.drop(1).collectLatest {
            delay(TEXT_CHANGE_SCROLL_DELAY)
            listState.animateScrollToLastItem(currentLastIndex)
        }
    }
}

// MARK: - Auxiliary

// A drag that begins during the scroll cancels it.
private suspend fun LazyListState.animateScrollToLastItem(lastIndex: Int) {
    if (lastIndex < 0 || !canScrollForward) return
    animateScrollToItem(lastIndex)
}
