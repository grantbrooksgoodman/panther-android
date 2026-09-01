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

        /** The hosted app share link. */
        val appShareLink = PersistentStorageKey("appShareLink")

        /** The hosted App Store build number. */
        val appStoreBuildNumber = PersistentStorageKey("appStoreBuildNumber")

        /** Whether the app should force an update. */
        val shouldForceUpdate = PersistentStorageKey("shouldForceUpdate")

        /** The build number the user was last forced to update from. */
        val buildNumberWhenLastForcedToUpdate = PersistentStorageKey("buildNumberWhenLastForcedToUpdate")

        /** The relaunch count since a postponed update. */
        val relaunchesSinceLastPostponedUpdate = PersistentStorageKey("relaunchesSinceLastPostponedUpdate")

        /** The epoch of the first postponed update. */
        val firstPostponedUpdate = PersistentStorageKey("firstPostponedUpdate")
    }
}
