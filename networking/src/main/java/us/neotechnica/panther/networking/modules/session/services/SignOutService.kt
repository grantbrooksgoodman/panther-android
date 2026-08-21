//
//  SignOutService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

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
        SessionStore.clear()
    }
}
