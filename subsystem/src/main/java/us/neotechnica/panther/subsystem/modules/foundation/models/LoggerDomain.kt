//
//  LoggerDomain.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * A category that classifies log output.
 *
 * Use the predefined domains for general subsystem logging, or
 * declare app-specific domains as top-level constants:
 *
 * ```kotlin
 * val CONVERSATION_LOGGER_DOMAIN = LoggerDomain("conversation")
 * ```
 */
@JvmInline
value class LoggerDomain(
    /** The domain's raw string identifier. */
    val rawValue: String,
) {
    // MARK: - Companion

    companion object {
        /** The domain for analytics event logging. */
        val analytics = LoggerDomain("analytics")

        /** The domain for caught exceptions. */
        val exception = LoggerDomain("exception")

        /** The default domain for uncategorized output. */
        val general = LoggerDomain("general")
    }
}
