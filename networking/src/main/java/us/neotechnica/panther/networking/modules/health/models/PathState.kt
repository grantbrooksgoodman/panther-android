//
//  PathState.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

/** The kind of network interface currently carrying traffic. */
internal enum class NetworkInterfaceType {
    CELLULAR,
    WIFI,
    WIRED_ETHERNET,
    OTHER,
    LOOPBACK,
    UNKNOWN,
}

/**
 * A point-in-time snapshot of the current network path's
 * properties.
 */
internal data class PathState(
    val interfaceType: NetworkInterfaceType? = null,
    val isConstrained: Boolean = false,
    val isExpensive: Boolean = false,
    val radioTechnology: RadioTechnology = RadioTechnology.UNKNOWN,
)
