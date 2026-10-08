//
//  StorageTransferProgress.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.models

/**
 * A snapshot of a storage transfer's progress.
 *
 * Progress-reporting storage operations emit a new snapshot
 * each time the underlying transfer advances:
 *
 * ```kotlin
 * storage
 *     .uploadWithProgress(
 *         imageData,
 *         metadata = HostedItemMetadata("images/photo.png"),
 *     ).collect { progress ->
 *         progressView.progress = progress.fractionCompleted.toFloat()
 *     }
 * ```
 *
 * Snapshots are immutable values; each one reflects the
 * state of the transfer at the moment it was reported.
 */
data class StorageTransferProgress(
    /** The number of bytes transferred so far. */
    val completedBytes: Long,
    /**
     * The total number of bytes expected to be transferred.
     *
     * This value may be `0` or negative before the transfer's
     * size is known.
     */
    val totalBytes: Long,
) {
    // MARK: - Computed Properties

    /**
     * The fraction of the transfer that has completed, in the
     * range `[0.0, 1.0]`.
     *
     * Returns `0` when [totalBytes] is not yet known.
     */
    val fractionCompleted: Double
        get() {
            if (totalBytes <= 0) return 0.0
            return (completedBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0)
        }
}
