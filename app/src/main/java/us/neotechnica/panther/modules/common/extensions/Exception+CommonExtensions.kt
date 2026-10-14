//
//  Exception+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

// MARK: - Methods

/**
 * Runs the given operation, returning the [Exception] it throws
 * instead of propagating it.
 *
 * @param operation The operation to run.
 *
 * @return The exception the operation threw, or `null` if it
 *   completed.
 */
suspend fun runCatchingException(operation: suspend () -> Unit): Exception? =
    try {
        operation()
        null
    } catch (exception: Exception) {
        exception
    }

/**
 * Returns this throwable as an [Exception], wrapping it with call-site
 * metadata when it is not one already.
 */
fun Throwable.toException(): Exception = this as? Exception ?: Exception.from(this, ExceptionMetadata(this))
