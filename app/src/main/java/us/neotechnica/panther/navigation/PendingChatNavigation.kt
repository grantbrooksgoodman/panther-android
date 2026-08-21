//
//  PendingChatNavigation.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import java.util.concurrent.atomic.AtomicReference

/**
 * Holds a conversation the app should open once it reaches the signed-in
 * content, set when a push notification is tapped.
 *
 * The splash flow [consume]s the pending key after resolving the current
 * user and pushes the corresponding chat.
 */
object PendingChatNavigation {
    /** The key of the conversation to open on the next opportunity. */
    const val CONVERSATION_ID_KEY_EXTRA = "conversationIDKey"

    private val pending = AtomicReference<String?>(null)

    /** Records [conversationIDKey] as the conversation to open next. */
    fun set(conversationIDKey: String?) {
        if (!conversationIDKey.isNullOrBlank()) pending.set(conversationIDKey)
    }

    /** Returns and clears the pending conversation key, if any. */
    fun consume(): String? = pending.getAndSet(null)
}
