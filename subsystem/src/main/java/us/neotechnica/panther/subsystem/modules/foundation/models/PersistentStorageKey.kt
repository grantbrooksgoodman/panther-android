//
//  PersistentStorageKey.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * A key identifying a value in [Persistent][us.neotechnica.panther.subsystem.modules.foundation.services.Persistent]
 * storage.
 *
 * Keys are declared as companion constants; the [rawValue] is the underlying
 * preferences key.
 */
@JvmInline
value class PersistentStorageKey(
    /** The underlying preferences key. */
    val rawValue: String,
) {
    // MARK: - Companion

    companion object {
        /** The persisted translation archive (JSON). */
        val translationArchive = PersistentStorageKey("translationArchive")

        /** Whether the build-info overlay is hidden. */
        val hidesBuildInfoOverlay = PersistentStorageKey("hidesBuildInfoOverlay")

        /** Whether developer mode is enabled. */
        val isDeveloperModeEnabled = PersistentStorageKey("isDeveloperModeEnabled")

        /** The keys owned by the subsystem, preserved across resets. */
        val subsystemKeys: List<PersistentStorageKey> =
            listOf(
                hidesBuildInfoOverlay,
                isDeveloperModeEnabled,
                translationArchive,
            )
    }
}
