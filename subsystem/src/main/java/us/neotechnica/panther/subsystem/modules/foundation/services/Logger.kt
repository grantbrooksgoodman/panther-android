//
//  Logger.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import android.util.Log
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain

/**
 * The subsystem's logging façade.
 *
 * Use [Logger] rather than platform logging primitives so log
 * output carries a [LoggerDomain] and a consistent format:
 *
 * ```kotlin
 * Logger.log(
 *     "Session restored.",
 *     domain = LoggerDomain.general,
 * )
 * ```
 *
 * Exceptions log through their own overload, which records the
 * descriptor, code, and source location:
 *
 * ```kotlin
 * Logger.log(exception)
 * ```
 */
object Logger {
    // MARK: - Properties

    private const val TAG = "AppSubsystem"

    // MARK: - Methods

    /**
     * Logs the given exception under the exception domain.
     *
     * @param exception The exception to log.
     */
    fun log(exception: Exception) {
        log(
            "${exception.descriptor} (${exception.code}) " +
                "[${exception.metadata.fileName}:${exception.metadata.line}]",
            domain = LoggerDomain.exception,
        )
    }

    /**
     * Logs the given message under the specified domain.
     *
     * @param message The message to log.
     * @param domain The domain to classify the output under.
     *   Defaults to the general domain.
     */
    fun log(
        message: String,
        domain: LoggerDomain = LoggerDomain.general,
    ) {
        val composed = "[${domain.rawValue}] $message"

        // android.util.Log is unavailable in local unit tests;
        // fall back to standard output.
        runCatching { Log.d(TAG, composed) }
            .onFailure { println("$TAG: $composed") }
    }
}
