//
//  Database+Support.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database.services

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Returns a Boolean value that indicates whether the value can be
 * stored in Firebase Realtime Database.
 *
 * Encodable values are `null`, `String`, `Boolean`, `Number`, and
 * lists or maps whose elements are themselves encodable. Maps must
 * have `String` keys.
 */
internal fun isFirebaseEncodable(value: Any?): Boolean =
    when (value) {
        null, is String, is Boolean, is Number -> true
        is List<*> -> value.all { isFirebaseEncodable(it) }
        is Map<*, *> -> value.keys.all { it is String } && value.values.all { isFirebaseEncodable(it) }
        else -> false
    }

/**
 * Runs a Firebase operation under the network read/write gate,
 * activity indicator, and timeout, translating failures into
 * [Exception].
 *
 * @param timeout The maximum time to wait before timing out.
 * @param sender The instance running the operation, for
 *   source-location metadata.
 * @param operation The suspending Firebase work to perform.
 *
 * @return The value produced by `operation`.
 *
 * @throws Exception if the operation is disabled, times out, or
 *   fails.
 */
internal suspend fun <T> guardedFirebaseOperation(
    timeout: Duration,
    sender: Any,
    operation: suspend () -> T,
): T {
    if (!Networking.isReadWriteEnabled) {
        throw Exception(
            "Read/write access is currently disabled.",
            metadata = ExceptionMetadata(sender),
        )
    }

    Networking.config.activityIndicatorDelegate.show()
    return try {
        withTimeout(timeout) { operation() }
    } catch (exception: TimeoutCancellationException) {
        throw Exception(
            "The operation timed out.",
            underlyingExceptions = listOf(Exception.from(exception, ExceptionMetadata(sender))),
            metadata = ExceptionMetadata(sender),
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        throw (throwable as? Exception) ?: Exception.from(throwable, ExceptionMetadata(sender))
    } finally {
        Networking.config.activityIndicatorDelegate.hide()
    }
}
