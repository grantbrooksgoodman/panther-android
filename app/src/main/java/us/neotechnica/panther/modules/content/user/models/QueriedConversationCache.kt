//
//  QueriedConversationCache.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * Manages the in-memory queried conversation cache.
 *
 * Caches the conversations matching each search term so repeated
 * queries for the same term do not re-filter the conversation list.
 */
object QueriedConversationCache {
    // MARK: - Properties

    private val cachedConversationsForSearchTerms = LockIsolated<Map<String, List<Conversation>>?>(null)

    // MARK: - Methods

    /** Removes every cached conversation. */
    fun clearCache() {
        cachedConversationsForSearchTerms.wrappedValue = null
    }

    internal fun cachedValue(searchTerm: String): List<Conversation>? = cachedConversationsForSearchTerms.wrappedValue?.get(searchTerm)

    internal fun cache(
        searchTerm: String,
        conversations: List<Conversation>,
    ) {
        val cache = (cachedConversationsForSearchTerms.wrappedValue ?: emptyMap()).toMutableMap()
        cache[searchTerm] = conversations
        cachedConversationsForSearchTerms.wrappedValue = cache
    }
}
