//
//  RemotelyUpdatableProtocol.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

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
        value is Serializable<*> ->
            value.encoded ?: throw Exception.Networking.notSerialized(
                mapOf(key to value),
                ExceptionMetadata(sender),
            )
        value is List<*> && value.all { it is Serializable<*> } ->
            value.filterIsInstance<Serializable<*>>().mapNotNull { it.encoded }.ifEmpty { bangQualifiedEmptyList }
        Networking.config.databaseDelegate.isEncodable(value) -> value
        else -> throw Exception.Networking.notSerialized(
            mapOf(key to value),
            ExceptionMetadata(sender),
        )
    }
