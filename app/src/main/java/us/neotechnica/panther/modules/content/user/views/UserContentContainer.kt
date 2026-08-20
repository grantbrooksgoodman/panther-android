//
//  UserContentContainer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.neotechnica.panther.modules.content.user.views.conversationspageview.ConversationsPageView

/**
 * The signed-in user's content shell.
 *
 * For this phase it hosts the conversations list; later phases add the
 * chat, settings, and new-chat destinations.
 *
 * @param modifier The modifier for this container.
 */
@Composable
fun UserContentContainer(modifier: Modifier = Modifier) {
    ConversationsPageView(modifier = modifier.fillMaxSize())
}
