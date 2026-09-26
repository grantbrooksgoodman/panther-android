//
//  SyncSession.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.sync.models

import us.neotechnica.panther.modules.session.sync.services.ConversationObserverService

/**
 * The container for a session's conversation synchronization
 * services.
 */
object SyncSession {
    /** The service that observes a conversation for real-time updates. */
    val conversationObserver = ConversationObserverService

    // `conversationSync` (ConversationSyncService) arrives with Phase 3.
}
