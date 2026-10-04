//
//  RemotelyUpdatableProtocol.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.notSerialized
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable

/**
 * Encodes [value] for a remote write using the standard encoding
 * ladder: a [Serializable], a list of [Serializable], or a raw
 * encodable value, in that order.
 */
internal fun encodeForWrite(
    sender: Any,
    key: String,
    value: Any,
): Any =
    when {
        value is Serializable<*> -> value.encoded ?: throw notSerialized(sender, mapOf(key to value))
        value is List<*> && value.all { it is Serializable<*> } ->
            value.filterIsInstance<Serializable<*>>().mapNotNull { it.encoded }.ifEmpty { bangQualifiedEmptyList }
        Networking.config.databaseDelegate.isEncodable(value) -> value
        else -> throw notSerialized(sender, mapOf(key to value))
    }
