//
//  ConversationsPageReducer+Lifecycle.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState

// MARK: - Coverage

/**
 * Returns whether the given pushed page covers the conversations list
 * for lifecycle purposes: only a chat does, as Settings and New Chat
 * are presented as sheets on iOS and leave the list's lifecycle alone.
 *
 * @param topPath The top of the user-content stack, or `null`.
 */
internal fun isCoveredByPush(topPath: UserContentNavigatorState.SeguePaths?): Boolean =
    topPath is UserContentNavigatorState.SeguePaths.Chat
