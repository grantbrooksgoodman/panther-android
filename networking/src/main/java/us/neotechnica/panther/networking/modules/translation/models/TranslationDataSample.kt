//
//  TranslationDataSample.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.models

import us.neotechnica.panther.subsystem.modules.foundation.extensions.seconds
import java.util.Date
import kotlin.math.abs

/**
 * A snapshot of the hosted translation archive tree together with
 * the time it was captured and the duration after which it is
 * stale.
 *
 * The [data] maps a language pair's string (`<from>-<to>`) to
 * that pair's archived translations, keyed by the encoded hash of
 * the input.
 */
internal class TranslationDataSample(
    /** The snapshotted archive tree, keyed by language pair. */
    val data: Map<String, Any>,
    /**
     * The duration after which the snapshot expires, in
     * milliseconds.
     */
    val expiryThreshold: Long,
    /**
     * The epoch-millisecond timestamp at which the snapshot was
     * captured.
     */
    val date: Long = System.currentTimeMillis(),
    /**
     * A Boolean value that indicates whether the snapshot was
     * captured from the archive, as opposed to the placeholder
     * [empty] snapshot that precedes the first capture.
     */
    val isPopulated: Boolean = true,
) {
    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether the snapshot holds
     * no data.
     */
    val isEmpty: Boolean
        get() = data.isEmpty()

    /**
     * A Boolean value that indicates whether the snapshot has
     * exceeded its expiry threshold.
     */
    val isExpired: Boolean
        get() = abs(Date().seconds(from = Date(date))) * MILLISECONDS_PER_SECOND > expiryThreshold

    // MARK: - Companion

    companion object {
        /** An empty snapshot. */
        val empty = TranslationDataSample(emptyMap(), 0L, date = 0L, isPopulated = false)
    }
}

private const val MILLISECONDS_PER_SECOND = 1_000L
