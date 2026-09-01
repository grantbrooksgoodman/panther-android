//
//  Logger.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import android.util.Log
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.LoggerPresentationDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import java.io.File
import java.util.UUID

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

    private val sessionID = UUID.randomUUID().toString()

    private var presentationDelegate: LoggerPresentationDelegate? = null

    // MARK: - Computed Properties

    /**
     * The file of the on-disk session record for the current
     * launch.
     *
     * A new file is created for each launch and is not persisted
     * across sessions. Returns `null` before
     * [FileStore.initialize][FileStore] has run.
     */
    val sessionRecordFilePath: File?
        get() = FileStore.resolve("$sessionID.txt")

    // MARK: - Delegate Registration

    /**
     * Registers the delegate that presents the logger's
     * user-visible alerts.
     *
     * Call this once at launch, before any log entry requests an
     * alert. The subsystem cannot reach the design system's alert
     * and toast components directly, so it forwards presentation
     * requests through this delegate.
     *
     * @param delegate The delegate to register, or `null` to
     *   suppress alert presentation.
     */
    fun setPresentationDelegate(delegate: LoggerPresentationDelegate?) {
        presentationDelegate = delegate
    }

    // MARK: - Methods

    /**
     * Logs the given exception under the exception domain, then
     * presents the given alert.
     *
     * @param exception The exception to log.
     * @param with The alert to present after logging, or `null` to
     *   log silently.
     */
    fun log(
        exception: Exception,
        with: AlertType? = null,
    ) {
        log(
            "${exception.descriptor} (${exception.code}) " +
                "[${exception.metadata.fileName}:${exception.metadata.line}]",
            domain = LoggerDomain.exception,
        )

        with?.let { presentationDelegate?.present(it, exception, null) }
    }

    /**
     * Logs the given message under the specified domain, then
     * presents the given alert.
     *
     * @param message The message to log.
     * @param domain The domain to classify the output under.
     *   Defaults to the general domain.
     * @param with The alert to present after logging, or `null` to
     *   log silently.
     */
    fun log(
        message: String,
        domain: LoggerDomain = LoggerDomain.general,
        with: AlertType? = null,
    ) {
        val composed = "[${domain.rawValue}] $message"

        // android.util.Log is unavailable in local unit tests;
        // fall back to standard output.
        runCatching { Log.d(TAG, composed) }
            .onFailure { println("$TAG: $composed") }

        appendToSessionRecord(composed)
        with?.let { presentationDelegate?.present(it, null, message) }
    }

    // MARK: - Auxiliary

    private fun appendToSessionRecord(line: String) {
        runCatching {
            val file = sessionRecordFilePath ?: return
            file.parentFile?.mkdirs()
            file.appendText("$line\n")
        }
    }
}
