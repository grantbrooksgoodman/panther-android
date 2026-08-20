//
//  SessionStoreChange.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.models

/**
 * A description of a mutation to the
 * [SessionStore][us.neotechnica.panther.networking.modules.session.services.SessionStore],
 * delivered through the `sessionStoreDidChange` shared event so the UI
 * can recompute affected views.
 */
sealed interface SessionStoreChange {
    /** Conversations were upserted or removed, keyed by conversation-ID key. */
    data class Conversations(
        val upsertedIDKeys: Set<String>,
        val removedIDKeys: Set<String>,
    ) : SessionStoreChange

    /** Messages were upserted or removed, keyed by message ID. */
    data class Messages(
        val upsertedIDs: Set<String>,
        val removedIDs: Set<String>,
    ) : SessionStoreChange

    /** Users were upserted or removed, keyed by user ID. */
    data class Users(
        val upsertedIDs: Set<String>,
        val removedIDs: Set<String>,
    ) : SessionStoreChange
}
