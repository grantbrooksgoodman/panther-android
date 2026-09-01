//
//  ExceptionMetadataDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

/**
 * A type that provides app-specific metadata for exception
 * handling.
 *
 * Conform to [ExceptionMetadataDelegate] to control which
 * exceptions are reported and to supply user-facing descriptions
 * for known error conditions. Register the conforming instance
 * through
 * [AppSubsystem.delegates][us.neotechnica.panther.subsystem.AppSubsystem.Delegates].
 *
 * **Important:** [isReportable] and [userFacingDescriptor] may be
 * invoked from any thread while an [Exception] is being created;
 * their implementations must be thread-safe and must not construct
 * exceptions themselves.
 */
interface ExceptionMetadataDelegate {
    /**
     * Returns a Boolean value indicating whether the exception
     * with the given error code should be reported.
     *
     * @param errorCode The exception's error code.
     *
     * @return `true` if the exception can be reported; otherwise,
     *   `false`.
     */
    fun isReportable(errorCode: String): Boolean

    /**
     * Returns a localized, user-facing description for the given
     * developer-facing descriptor, or `null` if no mapping exists.
     *
     * @param descriptor The exception's developer-facing
     *   descriptor.
     *
     * @return A user-appropriate string, or `null`.
     */
    fun userFacingDescriptor(descriptor: String): String?
}
