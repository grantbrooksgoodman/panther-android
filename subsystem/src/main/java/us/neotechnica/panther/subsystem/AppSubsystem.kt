//
//  AppSubsystem.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem

import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ExceptionMetadataDelegate
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

        private val _exceptionMetadata = LockIsolated<ExceptionMetadataDelegate?>(null)

        // MARK: - Computed Properties

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

        // MARK: - Methods

        /**
         * Registers the specified exception metadata delegate.
         *
         * @param exceptionMetadataDelegate The delegate to
         *   register.
         */
        fun registerExceptionMetadataDelegate(exceptionMetadataDelegate: ExceptionMetadataDelegate) {
            _exceptionMetadata.wrappedValue = exceptionMetadataDelegate
        }
    }
}
