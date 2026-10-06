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
 * The app's top-level paths are declared as companion constants
 * on this type.
 */
@JvmInline
value class NetworkPath(
    /** The string representation of the path. */
    val rawValue: String,
) {
    // MARK: - Companion

    companion object {
        val audioMessageInputs = NetworkPath("audioMessageInputs")
        val audioTranslations = NetworkPath("audioTranslations")
        val conversations = NetworkPath("conversations")
        val deletedUsers = NetworkPath("deletedUsers")
        val invalidatedCaches = NetworkPath("invalidatedCaches")
        val media = NetworkPath("media")
        val messages = NetworkPath("messages")
        val reportedUsers = NetworkPath("reportedUsers")
        val shared = NetworkPath("shared")
        val translations = NetworkPath("translations")
        val users = NetworkPath("users")
    }
}
