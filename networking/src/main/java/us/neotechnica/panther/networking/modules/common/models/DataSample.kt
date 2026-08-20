//
//  DataSample.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.models

/**
 * A cached database value together with the time it was captured
 * and the duration after which it should be considered stale.
 */
class DataSample(
    /** The cached value. */
    val data: Any,
    /** The duration after which the sample expires, in milliseconds. */
    val expiryThresholdMillis: Long,
    /** The epoch-millisecond timestamp at which the sample was captured. */
    val capturedAtMillis: Long = System.currentTimeMillis(),
) {
    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether the sample has
     * exceeded its expiry threshold.
     */
    val isExpired: Boolean
        get() = System.currentTimeMillis() - capturedAtMillis > expiryThresholdMillis
}
