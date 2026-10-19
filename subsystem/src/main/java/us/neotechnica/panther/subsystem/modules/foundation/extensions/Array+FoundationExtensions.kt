//
//  Array+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

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
