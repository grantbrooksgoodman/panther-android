//
//  SessionStore+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.session.entity.extensions.isVisibleForCurrentUser
import us.neotechnica.panther.modules.session.state.services.SessionStore

/** ID keys of archived conversations hidden from the current user. */
val SessionStore.ignoredConversationIDKeys: List<String>
    get() =
        conversations.values
            .filter { !it.isVisibleForCurrentUser }
            .map { it.id.key }
