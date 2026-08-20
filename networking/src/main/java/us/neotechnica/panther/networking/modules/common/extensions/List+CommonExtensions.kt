//
//  List+CommonExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

/**
 * The sentinel value used to represent a logically empty string
 * list whose key must nonetheless be preserved on the wire.
 *
 * The database drops keys whose values are empty.
 * [bangQualifiedEmptyList] preserves the key while signaling
 * that the list is logically empty. Use [isBangQualifiedEmpty]
 * to test whether a list carries this sentinel.
 */
val bangQualifiedEmptyList: List<String>
    get() = listOf(BANG_QUALIFIED_EMPTY)

/**
 * A Boolean value that indicates whether the list is empty or
 * contains only bang-qualified empty strings.
 */
val List<String>.isBangQualifiedEmpty: Boolean
    get() = isEmpty() || all { it.isBangQualifiedEmpty }
