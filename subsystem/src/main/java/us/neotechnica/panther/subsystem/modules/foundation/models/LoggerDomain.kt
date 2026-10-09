//
//  LoggerDomain.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * A named category that groups related log output.
 *
 * Logger domains partition log messages into logical channels so
 * that you can subscribe to only the output you care about. Declare
 * app-specific domains as companion extension properties:
 *
 * ```kotlin
 * val LoggerDomain.Companion.networking: LoggerDomain
 *     get() = LoggerDomain("networking")
 * ```
 *
 * The subsystem provides several built-in domains – including
 * [general], [alertKit], [caches], [concurrency], [localization],
 * and [translation] – that cover its own internal logging.
 */
@JvmInline
value class LoggerDomain(
    /** The domain's raw string identifier. */
    val rawValue: String,
) {
    // MARK: - Companion

    companion object {
        /** The domain for the alert and toast presentation layer. */
        val alertKit = LoggerDomain("alertKit")

        /** The domain for cache lifecycle output. */
        val caches = LoggerDomain("caches")

        /** The domain for concurrency and task scheduling. */
        val concurrency = LoggerDomain("concurrency")

        /** The default domain for uncategorized output. */
        val general = LoggerDomain("general")

        /** The domain for localization and translation resolution. */
        val localization = LoggerDomain("localization")

        /** The domain for string translation. */
        val translation = LoggerDomain("translation")

        // MARK: - Networking

        /** The framework-layer networking domains. */
        val Networking = NetworkingDomains

        /** The framework-layer networking domains. */
        object NetworkingDomains {
            /** The domain for authentication. */
            val auth = LoggerDomain("auth")

            /** The domain for the database layer. */
            val database = LoggerDomain("database")

            /** The domain for network-health probing. */
            val health = LoggerDomain("health")

            /** The domain for hosted translation. */
            val hostedTranslation = LoggerDomain("hostedTranslation")

            /** The domain for the storage layer. */
            val storage = LoggerDomain("storage")
        }
    }
}
