//
//  NetworkEnvironment.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.models

/**
 * The server environment used for network operations.
 *
 * The active environment determines which backend endpoints the
 * app communicates with. Use
 * [Networking.Config.setEnvironment][us.neotechnica.panther.networking.Networking.Config.setEnvironment]
 * to change the active environment at runtime.
 */
enum class NetworkEnvironment(
    /** The raw string identifier of the environment. */
    val rawValue: String,
) {
    // MARK: - Cases

    /** The development environment. */
    DEVELOPMENT("development"),

    /** The staging environment. */
    STAGING("staging"),

    /** The production environment. */
    PRODUCTION("production"),
    ;

    // MARK: - Computed Properties

    /**
     * An abbreviated label for the environment, such as `"dev"`,
     * `"stage"`, or `"prod"`. Database and storage paths are
     * prefixed with this value.
     */
    val shortString: String
        get() =
            when (this) {
                DEVELOPMENT -> "dev"
                STAGING -> "stage"
                PRODUCTION -> "prod"
            }

    // MARK: - Companion

    companion object {
        /**
         * Returns the environment with the given raw value, or
         * [PRODUCTION] if no environment matches.
         *
         * @param rawValue The raw string identifier to resolve.
         *
         * @return The matching environment, or [PRODUCTION].
         */
        fun from(rawValue: String): NetworkEnvironment =
            entries.firstOrNull {
                it.rawValue == rawValue
            } ?: PRODUCTION
    }
}
