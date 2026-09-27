//
//  WriteAction.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.models

/**
 * The action to take when writing a value during an `update(key, to)`
 * call.
 *
 * Return one of these cases from a type's `willWrite` to control how
 * the default write is handled. To abort the update entirely, throw
 * an exception from `willWrite` instead of returning a write action.
 */
sealed interface WriteAction<out T> {
    /**
     * Write the specified pre-encoded value directly, bypassing the
     * standard encoding ladder.
     */
    data class Encoded(val value: Any?) : WriteAction<Nothing>

    /**
     * The conformer has already performed the database write. The
     * default implementation skips its own write and proceeds
     * directly to `didWrite` with the given updated instance.
     */
    data class Handled<T>(val updated: T) : WriteAction<T>

    /** Proceed with the standard encoding ladder. */
    data object Proceed : WriteAction<Nothing>
}
