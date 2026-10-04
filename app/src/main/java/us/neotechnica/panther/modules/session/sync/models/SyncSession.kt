//
//  SyncSession.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.sync.models

import us.neotechnica.panther.modules.session.sync.services.ConversationObserverService
import us.neotechnica.panther.modules.session.sync.services.ConversationSyncService

/**
 * The container for a session's conversation synchronization
 * services.
 */
object SyncSession {
    /** The service that observes a conversation for real-time updates. */
    val conversationObserver = ConversationObserverService

    /**
     * The service that synchronizes a conversation with its server
     * state.
     *
     * A fresh [ConversationSyncService] is vended per access; each
     * carries its own in-progress synchronization state while
     * sharing coalescing and failure-backoff bookkeeping across
     * instances.
     */
    val conversationSync: ConversationSyncService
        get() = ConversationSyncService()
}
