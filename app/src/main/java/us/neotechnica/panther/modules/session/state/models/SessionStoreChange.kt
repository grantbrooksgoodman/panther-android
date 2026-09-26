//
//  SessionStoreChange.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.models

/**
 * A description of a mutation to the
 * [SessionStore][us.neotechnica.panther.modules.session.state.services.SessionStore],
 * delivered through the `sessionStoreDidChange` shared event so the UI
 * can recompute affected views.
 */
sealed interface SessionStoreChange {
    // MARK: - Types

    /** The category of contents a change affects. */
    enum class Kind {
        /** The conversations in the store. */
        CONVERSATIONS,

        /** The messages in the store. */
        MESSAGES,

        /** The users in the store. */
        USERS,
    }

    // MARK: - Computed Properties

    /** The category of contents the change affects. */
    val kind: Kind

    // MARK: - Cases

    /** Conversations were upserted or removed, keyed by conversation-ID key. */
    data class Conversations(
        val upsertedIDKeys: Set<String>,
        val removedIDKeys: Set<String>,
    ) : SessionStoreChange {
        override val kind: Kind get() = Kind.CONVERSATIONS
    }

    /** Messages were upserted or removed, keyed by message ID. */
    data class Messages(
        val upsertedIDs: Set<String>,
        val removedIDs: Set<String>,
    ) : SessionStoreChange {
        override val kind: Kind get() = Kind.MESSAGES
    }

    /** Users were upserted or removed, keyed by user ID. */
    data class Users(
        val upsertedIDs: Set<String>,
        val removedIDs: Set<String>,
    ) : SessionStoreChange {
        override val kind: Kind get() = Kind.USERS
    }
}
