//
//  AppSubsystem.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem

import us.neotechnica.panther.subsystem.modules.foundation.interfaces.CacheDomainListDelegate
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ErrorReportDelegate
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ExceptionMetadataDelegate
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.LoggerDomainSubscriptionDelegate
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.PermanentPersistentStorageKeyDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * A foundational framework for building apps with structured state
 * management, dependency injection, and reactive observation.
 *
 * Default behavior can be extended by registering delegates on
 * [delegates]. Register delegates once at launch – for example,
 * from the application's entry point.
 */
object AppSubsystem {
    // MARK: - Properties

    /**
     * A registry of app-level delegates that customize the
     * subsystem's behavior.
     */
    val delegates = Delegates

    // MARK: - Delegates

    /**
     * A registry of app-level delegates that customize the
     * subsystem's behavior.
     *
     * Access the shared registry through [AppSubsystem.delegates].
     * Optional delegates start as `null` and enable opt-in
     * functionality; set them when your app requires the
     * corresponding feature.
     *
     * **Note:** all delegate access is safe to perform from any
     * thread.
     */
    object Delegates {
        // MARK: - Properties

        private val _cacheDomainList = LockIsolated<CacheDomainListDelegate?>(null)

        private val _errorReport = LockIsolated<ErrorReportDelegate?>(null)

        private val _exceptionMetadata = LockIsolated<ExceptionMetadataDelegate?>(null)

        private val _loggerDomainSubscription = LockIsolated<LoggerDomainSubscriptionDelegate?>(null)

        private val _permanentPersistentStorageKeys = LockIsolated<PermanentPersistentStorageKeyDelegate?>(null)

        // MARK: - Computed Properties

        /**
         * The delegate that supplies the app's cache domains.
         *
         * When this property is `null`, only the subsystem's
         * built-in cache domains are available.
         */
        val cacheDomainList: CacheDomainListDelegate?
            get() = _cacheDomainList.wrappedValue

        /**
         * The delegate that files automatic error reports.
         *
         * The logger forwards reportable exceptions through this
         * delegate when the logger is configured to report errors
         * automatically. When this property is `null`, automatic
         * error reporting is disabled.
         */
        val errorReport: ErrorReportDelegate?
            get() = _errorReport.wrappedValue

        /**
         * The delegate that provides app-specific metadata for
         * exception handling.
         *
         * Use this delegate to control which exceptions are
         * reportable and to supply user-facing descriptions for
         * known error conditions. When this property is `null`,
         * all exceptions are reportable and no user-facing
         * descriptors are available.
         */
        val exceptionMetadata: ExceptionMetadataDelegate?
            get() = _exceptionMetadata.wrappedValue

        /**
         * The delegate that specifies which logger domains the app
         * subscribes to at launch.
         *
         * When this property is `null`, output for every domain is
         * produced and nothing is excluded from the session record.
         */
        val loggerDomainSubscription: LoggerDomainSubscriptionDelegate?
            get() = _loggerDomainSubscription.wrappedValue

        /**
         * The delegate that declares which persistent storage keys
         * survive a reset.
         *
         * When this property is `null`, only subsystem keys and any
         * explicitly specified keys are preserved during a reset.
         */
        val permanentPersistentStorageKeys: PermanentPersistentStorageKeyDelegate?
            get() = _permanentPersistentStorageKeys.wrappedValue

        // MARK: - Methods

        /**
         * Registers the specified cache domain list delegate.
         *
         * @param cacheDomainListDelegate The delegate to register.
         */
        fun registerCacheDomainListDelegate(cacheDomainListDelegate: CacheDomainListDelegate) {
            _cacheDomainList.wrappedValue = cacheDomainListDelegate
        }

        /**
         * Registers the specified error report delegate.
         *
         * @param errorReportDelegate The delegate to register.
         */
        fun registerErrorReportDelegate(errorReportDelegate: ErrorReportDelegate) {
            _errorReport.wrappedValue = errorReportDelegate
        }

        /**
         * Registers the specified exception metadata delegate.
         *
         * @param exceptionMetadataDelegate The delegate to
         *   register.
         */
        fun registerExceptionMetadataDelegate(exceptionMetadataDelegate: ExceptionMetadataDelegate) {
            _exceptionMetadata.wrappedValue = exceptionMetadataDelegate
        }

        /**
         * Registers the specified logger domain subscription
         * delegate.
         *
         * @param loggerDomainSubscriptionDelegate The delegate to
         *   register.
         */
        fun registerLoggerDomainSubscriptionDelegate(
            loggerDomainSubscriptionDelegate: LoggerDomainSubscriptionDelegate,
        ) {
            _loggerDomainSubscription.wrappedValue = loggerDomainSubscriptionDelegate
        }

        /**
         * Registers the specified permanent persistent storage key
         * delegate.
         *
         * @param permanentPersistentStorageKeyDelegate The delegate
         *   to register.
         */
        fun registerPermanentPersistentStorageKeyDelegate(
            permanentPersistentStorageKeyDelegate: PermanentPersistentStorageKeyDelegate,
        ) {
            _permanentPersistentStorageKeys.wrappedValue = permanentPersistentStorageKeyDelegate
        }
    }
}
