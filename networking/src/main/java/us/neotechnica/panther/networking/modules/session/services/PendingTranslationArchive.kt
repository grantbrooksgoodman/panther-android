//
//  PendingTranslationArchive.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * Holds hosted-archive fan-out entries for deferred-archival
 * translations until the message commit that carries them drains them
 * into its payload.
 *
 * Recording is idempotent – re-recording a hosting key overwrites the
 * previous entry. Entries drained into a commit that fails are
 * re-recorded by the retry's translate pass.
 */
object PendingTranslationArchive {
    // MARK: - Properties

    private val entries = LockIsolated(mapOf<String, Pair<String, Any>>())

    // MARK: - Methods

    /**
     * Removes and returns the archive entry recorded for the given
     * hosting key, if any.
     *
     * @param hostingKey The hosting key whose entry to drain.
     *
     * @return The archive entry (path to value), or `null` if none is
     *   recorded.
     */
    fun drain(hostingKey: String): Pair<String, Any>? =
        entries.withValue { reference ->
            val entry = reference.value[hostingKey]
            reference.value = reference.value - hostingKey
            entry
        }

    /**
     * Records the given archive entry for the given hosting key,
     * replacing any existing entry.
     *
     * @param entry The archive entry (path to value) to record.
     * @param hostingKey The hosting key to record the entry for.
     */
    fun record(
        entry: Pair<String, Any>,
        hostingKey: String,
    ) {
        entries.withValue { reference ->
            reference.value = reference.value + (hostingKey to entry)
        }
    }
}
