//
//  Map+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.common.extensions

// MARK: - Methods

/**
 * Returns the list of strings stored under the given key, or `null` if
 * the value is missing or is not a list of strings.
 *
 * @param key The key of the value to read.
 */
fun Map<String, Any?>.stringList(key: String): List<String>? = stringList(this[key])

/**
 * Returns the given value as a list of strings, or `null` if it is not
 * one.
 *
 * @param value The decoded value to read.
 */
@Suppress("UNCHECKED_CAST")
fun stringList(value: Any?): List<String>? =
    (value as? List<*>)
        ?.takeIf { list -> list.all { it is String } }
        ?.map { it as String }
