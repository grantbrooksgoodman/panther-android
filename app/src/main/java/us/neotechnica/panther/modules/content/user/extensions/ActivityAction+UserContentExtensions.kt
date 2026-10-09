//
//  ActivityAction+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.user.models.User

/**
 * A Boolean value that indicates whether the action added the
 * current user to a conversation.
 */
val Activity.Action.isCurrentUserAdded: Boolean
    get() =
        when (this) {
            is Activity.Action.AddedToConversation -> userID == User.currentUserID
            else -> false
        }
