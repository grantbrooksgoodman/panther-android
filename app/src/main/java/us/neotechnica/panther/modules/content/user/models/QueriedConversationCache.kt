//
//  QueriedConversationCache.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * Manages the in-memory queried conversation cache.
 *
 * Caches the conversations matching each search term so repeated
 * queries for the same term do not re-filter the conversation list.
 */
object QueriedConversationCache {
    // MARK: - Properties

    private val mutableCachedConversationsForSearchTerms = LockIsolated<Map<String, List<Conversation>>?>(null)

    /** The conversations matching each search term. */
    var cachedConversationsForSearchTerms: Map<String, List<Conversation>>?
        get() = mutableCachedConversationsForSearchTerms.wrappedValue
        set(value) {
            mutableCachedConversationsForSearchTerms.wrappedValue = value
        }

    // MARK: - Clear Cache

    /** Removes every cached conversation. */
    fun clearCache() {
        cachedConversationsForSearchTerms = null
    }
}
