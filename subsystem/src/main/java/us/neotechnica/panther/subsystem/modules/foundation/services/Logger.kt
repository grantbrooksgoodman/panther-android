//
//  Logger.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import android.util.Log
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.DefaultLoggerDomainSubscriptionDelegate
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.LoggerDomainSubscriptionDelegate
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.LoggerPresentationDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import java.io.File
import java.util.UUID
import kotlin.math.abs

/**
 * A centralized, thread-safe logging service that writes structured
 * diagnostic output to the console and to an on-disk session record.
 *
 * Use [Logger] rather than platform logging primitives so each entry
 * carries a [LoggerDomain], a timestamp, and the caller's file,
 * function, and line number:
 *
 * ```kotlin
 * Logger.log(
 *     "Sync completed successfully.",
 *     domain = LoggerDomain.general,
 * )
 * ```
 *
 * Log an exception directly to capture its error code, descriptor,
 * and any attached user info:
 *
 * ```kotlin
 * Logger.log(exception, domain = LoggerDomain.general)
 * ```
 *
 * ## Domain Subscription
 *
 * The logger only produces output for domains it is subscribed to.
 * Subscribe or unsubscribe at runtime with [subscribe] and
 * [unsubscribe]. To configure the initial set of subscribed domains
 * at launch, register a [LoggerDomainSubscriptionDelegate]; when none
 * is registered, [DefaultLoggerDomainSubscriptionDelegate] supplies
 * the built-in subsystem domains.
 *
 * ## Filtering
 *
 * Apply a [Filter] with [setFilter] to narrow output – for example,
 * to see only exceptions or to restrict output to specific source
 * files.
 *
 * ## Session Record
 *
 * Every entry is appended to an on-disk file for the current launch,
 * accessible through [sessionRecordFilePath]. Domains listed in
 * [domainsExcludedFromSessionRecord] are omitted from that file.
 */
object Logger {
    // MARK: - Types

    /**
     * A rule that restricts which log entries are recorded.
     *
     * Apply a filter with [setFilter] to narrow the logger's output.
     * Only entries that satisfy the active filter are printed to the
     * console and written to the session record.
     */
    sealed interface Filter {
        /**
         * Restricts output to entries originating from the specified
         * source files.
         */
        data class ByFileNames(
            val fileNames: Set<String>,
        ) : Filter

        /**
         * Restricts output to exception entries only, ignoring
         * plain-text messages.
         */
        data object ExceptionsOnly : Filter

        /** Restricts output to reportable exception entries only. */
        data object ReportableExceptionsOnly : Filter
    }

    // MARK: - Properties

    private const val TAG = "AppSubsystem"

    private val sessionID = UUID.randomUUID().toString()

    private val ioLock = Any()

    private val currentTimeLastCalled = LockIsolated(System.currentTimeMillis())

    private val _domainsExcludedFromSessionRecord = LockIsolated<List<LoggerDomain>?>(null)

    private val _filter = LockIsolated<Filter?>(null)

    private val _subscribedDomains = LockIsolated<List<LoggerDomain>?>(null)

    private var presentationDelegate: LoggerPresentationDelegate? = null

    // MARK: - Computed Properties

    /**
     * A Boolean value indicating whether reportable exceptions are
     * filed automatically.
     *
     * When `true`, [log] forwards every reportable exception through
     * the registered error report delegate. The default is `false`.
     */
    var reportsErrorsAutomatically = false
        private set

    /**
     * The file of the on-disk session record for the current launch.
     *
     * A new file is created for each launch and is not persisted
     * across sessions. Returns `null` before
     * [FileStore][us.neotechnica.panther.subsystem.modules.foundation.services.FileStore]
     * has been initialized.
     */
    val sessionRecordFilePath: File?
        get() = FileStore.resolve("$sessionID.txt")

    /**
     * The domains whose output is excluded from the on-disk session
     * record.
     *
     * Messages logged to these domains still appear in the console,
     * but are not written to the session record file.
     */
    val domainsExcludedFromSessionRecord: List<LoggerDomain>
        get() = _domainsExcludedFromSessionRecord.wrappedValue ?: activeSubscription.domainsExcludedFromSessionRecord

    /** The currently active filter, or `null` if no filter is applied. */
    val filter: Filter?
        get() = _filter.wrappedValue

    /**
     * The domains the logger is currently subscribed to.
     *
     * Only messages logged to a subscribed domain produce output.
     */
    val subscribedDomains: List<LoggerDomain>
        get() = _subscribedDomains.wrappedValue ?: activeSubscription.subscribedDomains

    private val activeSubscription: LoggerDomainSubscriptionDelegate
        get() = AppSubsystem.delegates.loggerDomainSubscription ?: DefaultLoggerDomainSubscriptionDelegate

    // MARK: - Domain Subscription

    /**
     * Subscribes the logger to the given domain.
     *
     * After subscribing, messages logged to this domain appear in the
     * console and are written to the session record. Subscribing to a
     * domain that is already subscribed has no effect.
     *
     * @param domain The domain to subscribe to.
     */
    fun subscribe(domain: LoggerDomain) {
        _subscribedDomains.wrappedValue = (subscribedDomains + domain).distinct()
    }

    /**
     * Subscribes the logger to each of the given domains.
     *
     * @param domains The domains to subscribe to.
     */
    fun subscribe(domains: List<LoggerDomain>) {
        domains.forEach { subscribe(it) }
    }

    /**
     * Unsubscribes the logger from the given domain.
     *
     * After unsubscribing, messages logged to this domain are
     * silently ignored. Unsubscribing from a domain that is not
     * currently subscribed has no effect.
     *
     * @param domain The domain to unsubscribe from.
     */
    fun unsubscribe(domain: LoggerDomain) {
        _subscribedDomains.wrappedValue = subscribedDomains.filter { it != domain }
    }

    /**
     * Unsubscribes the logger from each of the given domains.
     *
     * @param domains The domains to unsubscribe from.
     */
    fun unsubscribe(domains: List<LoggerDomain>) {
        domains.forEach { unsubscribe(it) }
    }

    // MARK: - Setters

    /**
     * Sets the domains whose output is excluded from the on-disk
     * session record.
     *
     * @param domainsExcludedFromSessionRecord The domains to exclude.
     */
    fun setDomainsExcludedFromSessionRecord(domainsExcludedFromSessionRecord: List<LoggerDomain>) {
        _domainsExcludedFromSessionRecord.wrappedValue = domainsExcludedFromSessionRecord
    }

    /**
     * Sets the active log filter.
     *
     * Pass `null` to remove any existing filter and allow all entries
     * through.
     *
     * @param filter The filter to apply, or `null` to clear the
     *   current filter.
     */
    fun setFilter(filter: Filter?) {
        _filter.wrappedValue = filter
    }

    /**
     * Sets whether reportable exceptions are filed automatically.
     *
     * When enabled, [log] forwards every reportable exception through
     * the registered error report delegate without presenting a prompt
     * to the user.
     *
     * @param reportsErrorsAutomatically Whether to file reportable
     *   exceptions automatically.
     */
    fun setReportsErrorsAutomatically(reportsErrorsAutomatically: Boolean) {
        this.reportsErrorsAutomatically = reportsErrorsAutomatically
    }

    /**
     * Registers the delegate that presents the logger's user-visible
     * alerts.
     *
     * Call this once at launch, before any log entry requests an
     * alert. The subsystem cannot reach the design system's alert and
     * toast components directly, so it forwards presentation requests
     * through this delegate.
     *
     * @param delegate The delegate to register, or `null` to suppress
     *   alert presentation.
     */
    fun setPresentationDelegate(delegate: LoggerPresentationDelegate?) {
        presentationDelegate = delegate
    }

    // MARK: - Logging

    /**
     * Logs an exception.
     *
     * The entry includes the exception's descriptor, error code, user
     * info (if any), source location, and a timestamp. When the
     * exception is reportable and [reportsErrorsAutomatically] is
     * enabled, the exception is also forwarded to the registered error
     * report delegate.
     *
     * @param exception The exception to log.
     * @param domain The domain to log to. Defaults to the general
     *   domain.
     * @param with The alert to present after logging, or `null` to log
     *   silently.
     */
    fun log(
        exception: Exception,
        domain: LoggerDomain = LoggerDomain.general,
        with: AlertType? = null,
    ) {
        if (filter == Filter.ReportableExceptionsOnly && !exception.isReportable) return

        val fileName = exception.metadata.fileName
        (filter as? Filter.ByFileNames)?.let { if (!it.fileNames.contains(fileName)) return }

        val headerAffix = if (exception.isReportable) "🛑" else ""
        val headerDelimiter = if (headerAffix.isEmpty()) "" else " "
        val headerInfix = "$fileName | ${domain.rawValue.camelCaseToHumanReadable.uppercase()} | ${timestamp()}"
        val header = "----- $headerAffix$headerDelimiter$headerInfix$headerDelimiter$headerAffix -----"
        val footer = "-".repeat(header.length)

        val exceptionString =
            exception.userInfo
                ?.let { "${exception.descriptor} (${exception.code})\n${format(it)}" }
                ?: "${exception.descriptor} (${exception.code})"

        val functionName = exception.metadata.function.substringBefore("(")
        write(
            "\n$header\n" +
                "${exception.metadata.sender}.$functionName() [${exception.metadata.line}]${elapsedTime()}\n" +
                "$exceptionString\n" +
                "$footer\n",
            domain = domain,
        )

        currentTimeLastCalled.wrappedValue = System.currentTimeMillis()

        if (reportsErrorsAutomatically && exception.isReportable) {
            AppSubsystem.delegates.errorReport?.fileReport(exception)
        }

        with?.let { presentationDelegate?.present(it, exception, null) }
    }

    /**
     * Logs a plain-text message.
     *
     * The entry includes the caller's file, function, and line number
     * alongside the message text.
     *
     * @param message The message to log.
     * @param domain The domain to log to. Defaults to the general
     *   domain.
     * @param with The alert to present after logging, or `null` to log
     *   silently.
     */
    fun log(
        message: String,
        domain: LoggerDomain = LoggerDomain.general,
        with: AlertType? = null,
    ) {
        if (filter == Filter.ExceptionsOnly || filter == Filter.ReportableExceptionsOnly) return

        val frame = callerFrame()
        val fileName = frame?.fileName ?: "Unknown"
        (filter as? Filter.ByFileNames)?.let { if (!it.fileNames.contains(fileName)) return }

        val sender = frame?.className?.substringAfterLast('.')?.substringBefore('$') ?: "Unknown"
        val functionName = (frame?.methodName ?: "Unknown").substringBefore("(")
        val header = "----- $fileName | ${domain.rawValue.camelCaseToHumanReadable.uppercase()} | ${timestamp()} -----"
        val footer = "-".repeat(header.length)

        write(
            "\n$header\n" +
                "$sender.$functionName() [${frame?.lineNumber ?: 0}]${elapsedTime()}\n" +
                "$message\n" +
                "$footer\n",
            domain = domain,
        )

        currentTimeLastCalled.wrappedValue = System.currentTimeMillis()

        with?.let { presentationDelegate?.present(it, null, message) }
    }

    // MARK: - Auxiliary

    private fun canLog(domain: LoggerDomain): Boolean = Build.loggingEnabled && subscribedDomains.contains(domain)

    private fun write(
        text: String,
        domain: LoggerDomain,
    ) {
        synchronized(ioLock) {
            if (canLog(domain)) {
                // android.util.Log is unavailable in local unit tests;
                // fall back to standard output.
                runCatching { Log.d(TAG, text) }
                    .onFailure { println("$TAG: $text") }
            }

            if (domainsExcludedFromSessionRecord.contains(domain)) return
            appendToSessionRecord(text)
        }
    }

    private fun appendToSessionRecord(text: String) {
        runCatching {
            val file = sessionRecordFilePath ?: return
            file.parentFile?.mkdirs()
            file.appendText("$text\n")
        }
    }

    private fun timestamp(): String {
        val formatter = java.text.SimpleDateFormat("H:mm:ss.SSSS", java.util.Locale.US)
        return formatter.format(java.util.Date())
    }

    private fun elapsedTime(): String {
        val seconds = abs(System.currentTimeMillis() - currentTimeLastCalled.wrappedValue) / MILLIS_PER_SECOND
        return if (seconds == 0L) "" else " @ ${seconds}s FLC"
    }

    private fun format(parameters: Map<String, Any>): String {
        if (parameters.isEmpty()) return ""
        if (parameters.size == 1) {
            val (key, value) = parameters.entries.first()
            return "[$key: $value]"
        }

        val keys = parameters.keys.sorted()
        val lines =
            keys.mapIndexed { index, key ->
                val value = parameters.getValue(key)
                when (index) {
                    0 -> "[$key: $value,"
                    keys.size - 1 -> "$key: $value]"
                    else -> "$key: $value,"
                }
            }
        return lines.joinToString("\n")
    }

    // Resolves the first stack frame outside the logger and foundation
    // models, so a logged message is attributed to its true call site.
    private fun callerFrame(): StackTraceElement? =
        Thread.currentThread().stackTrace.firstOrNull {
            !it.className.startsWith("java.lang.Thread") &&
                !it.className.startsWith("us.neotechnica.panther.subsystem.modules.foundation.models") &&
                it.className != Logger::class.java.name
        }

    private const val MILLIS_PER_SECOND = 1000L
}

// Inserts a space before each interior capital letter, yielding a
// human-readable rendering of a camel-case identifier.
private val String.camelCaseToHumanReadable: String
    get() = replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
