//
//  Array+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

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
