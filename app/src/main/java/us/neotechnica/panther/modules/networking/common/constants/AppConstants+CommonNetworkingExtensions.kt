//
//  AppConstants+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common.constants

// MARK: - Float

object CommonNetworkingExtensionsFloats {
    object Duration {
        const val TRANSFER_TIMEOUT_BYTES_PER_MEGABYTE = 1_048_576.0
        const val TRANSFER_TIMEOUT_FLOOR_SECONDS = 30.0
        const val TRANSFER_TIMEOUT_MAX_SECONDS = 300.0
        const val TRANSFER_TIMEOUT_SECONDS_PER_MEGABYTE = 10.0
    }
}
