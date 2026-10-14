//
//  ConversationsListMemo.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.util.UUID

/**
 * In-memory memo for the resolved conversation list, keyed by the
 * page's change token and search query. One entry suffices: the home
 * page is the sole resolver at any time.
 */
internal object ConversationsListMemo {
    private data class Entry(
        val token: UUID,
        val searchQuery: String,
        val value: List<Conversation>,
    )

    private val entry = LockIsolated<Entry?>(null)

    fun value(
        token: UUID,
        searchQuery: String,
    ): List<Conversation>? {
        val current = entry.wrappedValue ?: return null
        if (current.token != token || current.searchQuery != searchQuery) return null
        return current.value
    }

    fun store(
        value: List<Conversation>,
        token: UUID,
        searchQuery: String,
    ) {
        entry.wrappedValue = Entry(token, searchQuery, value)
    }
}
