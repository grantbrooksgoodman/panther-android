//
//  Duration+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

import us.neotechnica.panther.modules.networking.common.constants.CommonNetworkingExtensionsFloats
import java.io.File
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// MARK: - Constants Accessors

private typealias Floats = CommonNetworkingExtensionsFloats.Duration

/**
 * Returns a transfer timeout scaled to the size of the given file.
 *
 * The timeout scales linearly with file size, assuming a worst-case
 * sustained transfer rate of roughly 1 Mbps, and falls back to the
 * maximum when the file size cannot be determined.
 *
 * @param forItemAt The file to compute a timeout for.
 *
 * @return The transfer timeout.
 */
fun Duration.Companion.transferTimeout(forItemAt: File): Duration {
    val fileSizeBytes = forItemAt.takeIf { it.exists() }?.length() ?: return Floats.TRANSFER_TIMEOUT_MAX_SECONDS.seconds

    return min(
        Floats.TRANSFER_TIMEOUT_MAX_SECONDS,
        Floats.TRANSFER_TIMEOUT_FLOOR_SECONDS +
            Floats.TRANSFER_TIMEOUT_SECONDS_PER_MEGABYTE * (fileSizeBytes.toDouble() / Floats.TRANSFER_TIMEOUT_BYTES_PER_MEGABYTE),
    ).seconds
}
