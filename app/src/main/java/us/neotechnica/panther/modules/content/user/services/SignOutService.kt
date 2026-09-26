//
//  SignOutService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.modules.networking.user.services.UserMutationService
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore

/**
 * Signs the current user out: prunes this device's push tokens, stops
 * live observation, clears the current conversation and session store,
 * and forgets the persisted user identifier.
 */
object SignOutService {
    /** Signs the current user out, clearing local session state. */
    suspend fun signOut() {
        runCatching { UserMutationService.prunePushTokensForCurrentUser() }
            .onFailure { Logger.log("Failed to prune push tokens on sign-out: ${it.message}") }

        UserSessionService.stopObservingCurrentUserChanges()
        ConversationSessionService.setCurrentConversation(null)
        Persistent.setString(PersistentStorageKey.currentUserID, null)
        SessionStore.clearConversationArchive()
        SessionStore.clearMessageArchive()
        SessionStore.clearUserArchive()
    }
}
