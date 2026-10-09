//
//  PersistentStorageKey+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey

// MARK: - Types

/** The persistent storage keys scoped to the session store. */
enum class SessionStoreStorageKey(
    val rawValue: String,
) {
    CONVERSATION_ARCHIVE("conversationArchive"),
    MESSAGE_ARCHIVE("messageArchive"),
    MESSAGE_OUTBOX("messageOutbox"),
    USER_ARCHIVE("userArchive"),
}

// MARK: - Methods

/**
 * Returns the persistent storage key for the given session store key.
 *
 * @param key The session store key.
 *
 * @return The corresponding persistent storage key.
 */
fun PersistentStorageKey.Companion.sessionStore(key: SessionStoreStorageKey): PersistentStorageKey = PersistentStorageKey(key.rawValue)
