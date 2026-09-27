//
//  PathState.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
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
 * A coarse classification of the current cellular radio access
 * technology, used only as a score-capping prior.
 */
internal enum class RadioTechnology {
    MODERN,
    INTERMEDIATE,
    LEGACY,
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
