//
//  PersistentStorageKey.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * A key identifying a value in [Persistent][us.neotechnica.panther.subsystem.modules.foundation.services.Persistent]
 * storage.
 *
 * Keys are declared as companion constants, mirroring the iOS
 * `PersistentStorageKey` cases; the [rawValue] is the underlying
 * preferences key.
 */
@JvmInline
value class PersistentStorageKey(
    /** The underlying preferences key. */
    val rawValue: String,
) {
    // MARK: - Companion

    companion object {
        /** The signed-in user's identifier. */
        val currentUserID = PersistentStorageKey("currentUserID")

        /** The persisted message outbox archive (JSON). */
        val messageOutbox = PersistentStorageKey("messageOutbox")

        /** The persisted contact-match archive (JSON). */
        val contactArchive = PersistentStorageKey("contactArchive")

        /** Whether the build-info overlay is hidden. */
        val hidesBuildInfoOverlay = PersistentStorageKey("hidesBuildInfoOverlay")
    }
}
