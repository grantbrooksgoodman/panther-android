//
//  Array+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * Performs an operation on each element of the list concurrently,
 * running at most [maxConcurrentOperations] at a time.
 *
 * The first failure cancels the remaining operations and propagates.
 *
 * @param maxConcurrentOperations The maximum number of operations to
 *   execute simultaneously. The default is 10.
 * @param perform The operation to perform on each element.
 */
suspend fun <Element> List<Element>.forEachConcurrently(
    maxConcurrentOperations: Int = DEFAULT_MAX_CONCURRENT_OPERATIONS,
    perform: suspend (Element) -> Unit,
) {
    val semaphore = Semaphore(maxConcurrentOperations)
    coroutineScope {
        map { element -> async { semaphore.withPermit { perform(element) } } }.awaitAll()
    }
}

private const val DEFAULT_MAX_CONCURRENT_OPERATIONS = 10

/**
 * Returns the list unchanged, or throws when it is empty.
 *
 * Decoders that map every element of a collection call this first,
 * so an empty collection fails as a decoding error rather than
 * silently producing an empty result.
 *
 * @return The receiver.
 *
 * @throws Exception if the list is empty.
 */
fun <Element> List<Element>.failingForEmptyCollection(): List<Element> {
    if (isEmpty()) {
        throw Exception(
            "Collection is empty.",
            metadata = ExceptionMetadata(this),
        )
    }

    return this
}

/**
 * A single exception compiled from the list, formed by appending each
 * exception as an underlying exception of the final element.
 *
 * Returns `null` when the list is empty, or the sole element when the
 * list contains one exception.
 */
val List<Exception>.compiledException: Exception?
    get() {
        if (isEmpty()) return null
        var finalException = last()
        if (size <= 1) return finalException
        dropLast(1).reversed().distinct().forEach {
            finalException = finalException.appending(underlyingException = it)
        }
        return finalException
    }
