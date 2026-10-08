//
//  NetworkPath.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.models

/**
 * A value that represents a path to a resource on the network
 * backend.
 *
 * Use [NetworkPath] to create a type-safe reference to a backend
 * resource location:
 *
 * ```kotlin
 * val path = NetworkPath("users/profile")
 * ```
 *
 * The app's top-level paths are declared as companion extension
 * properties on this type.
 */
@JvmInline
value class NetworkPath(
    /** The string representation of the path. */
    val rawValue: String,
) {
    // MARK: - Companion

    companion object
}
