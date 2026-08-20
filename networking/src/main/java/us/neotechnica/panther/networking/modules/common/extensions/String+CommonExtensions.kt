//
//  String+CommonExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

/**
 * The sentinel value used to represent a logically empty string
 * whose key must nonetheless be preserved on the wire.
 *
 * The database drops keys whose values are empty.
 * [BANG_QUALIFIED_EMPTY] instead preserves the key while
 * signaling that its value is logically empty. Use
 * [isBangQualifiedEmpty] to test whether a string carries this
 * sentinel.
 */
const val BANG_QUALIFIED_EMPTY: String = "!"

/**
 * A Boolean value that indicates whether the string is blank or
 * equal to [BANG_QUALIFIED_EMPTY].
 */
val String.isBangQualifiedEmpty: Boolean
    get() = isBlank() || this == BANG_QUALIFIED_EMPTY

/**
 * The string reduced to its decimal digit characters.
 */
val String.digits: String
    get() = filter { it.isDigit() }
