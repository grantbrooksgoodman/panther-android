//
//  AppConstants+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.common.constants

// MARK: - Float

/** The values of the common networking extensions. */
object CommonNetworkingExtensionsFloats {
    /** The values of the transfer timeout. */
    object Duration {
        /** The number of bytes in a megabyte, for transfer timeouts. */
        const val TRANSFER_TIMEOUT_BYTES_PER_MEGABYTE = 1_048_576.0

        /** The minimum transfer timeout, in seconds. */
        const val TRANSFER_TIMEOUT_FLOOR_SECONDS = 30.0

        /** The maximum transfer timeout, in seconds. */
        const val TRANSFER_TIMEOUT_MAX_SECONDS = 300.0

        /** The transfer timeout allowed per megabyte, in seconds. */
        const val TRANSFER_TIMEOUT_SECONDS_PER_MEGABYTE = 10.0
    }
}
