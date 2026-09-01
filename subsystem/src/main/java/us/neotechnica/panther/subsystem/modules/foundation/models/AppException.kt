//
//  AppException.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * A catalogued error code that can be compared against live
 * [Exception] instances.
 *
 * Use [AppException] to define known error codes in a central
 * location so that error-handling logic can match exceptions by
 * code rather than by descriptor string:
 *
 * ```kotlin
 * val AppException.Companion.timedOut: AppException get() = AppException("801F")
 *
 * if (exception.isEqual(to = AppException.timedOut)) {
 *     retryRequest()
 * }
 * ```
 *
 * @property errorCode The error code this instance represents.
 */
data class AppException(
    val errorCode: String,
) {
    companion object
}
