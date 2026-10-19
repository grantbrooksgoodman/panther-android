//
//  DataSample.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.models

import us.neotechnica.panther.subsystem.modules.foundation.extensions.seconds
import java.util.Date
import kotlin.math.abs

/**
 * A cached database value together with the time it was captured
 * and the duration after which it should be considered stale.
 */
class DataSample(
    /** The cached value. */
    val data: Any,
    /**
     * The duration after which the sample expires, in
     * milliseconds.
     */
    val expiryThreshold: Long,
    /**
     * The epoch-millisecond timestamp at which the sample was
     * captured.
     */
    val date: Long = System.currentTimeMillis(),
) {
    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether the sample has
     * exceeded its expiry threshold.
     */
    val isExpired: Boolean
        get() = abs(Date().seconds(from = Date(date))) * MILLISECONDS_PER_SECOND > expiryThreshold
}

private const val MILLISECONDS_PER_SECOND = 1_000L
