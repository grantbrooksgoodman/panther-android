//
//  TranslationDataSample.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.models

/**
 * A snapshot of the hosted translation archive tree together with the
 * time it was captured and the duration after which it is stale.
 *
 * The [data] maps a language pair's string (`<from>-<to>`) to that
 * pair's archived translations, keyed by the encoded hash of the input.
 */
internal class TranslationDataSample(
    /** The snapshotted archive tree, keyed by language pair. */
    val data: Map<String, Any>,
    /** The duration after which the snapshot expires, in milliseconds. */
    val expiryThresholdMillis: Long,
    /** The epoch-millisecond timestamp at which the snapshot was captured. */
    val capturedAtMillis: Long = System.currentTimeMillis(),
) {
    // MARK: - Computed Properties

    /** A Boolean value that indicates whether the snapshot holds no data. */
    val isEmpty: Boolean
        get() = data.isEmpty()

    /** A Boolean value that indicates whether the snapshot has exceeded its expiry threshold. */
    val isExpired: Boolean
        get() = System.currentTimeMillis() - capturedAtMillis > expiryThresholdMillis

    // MARK: - Companion

    companion object {
        /** An empty snapshot. */
        val empty = TranslationDataSample(emptyMap(), 0L)
    }
}
