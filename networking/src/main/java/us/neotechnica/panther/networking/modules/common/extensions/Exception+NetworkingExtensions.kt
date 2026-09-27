//
//  Exception+NetworkingExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * Returns an exception describing a failure to decode a
 * serialized value.
 *
 * @param sender The instance reporting the failure, used to
 *   capture source-location metadata.
 * @param data The serialized data that could not be decoded.
 *
 * @return The decoding-failure exception.
 */
fun decodingFailure(
    sender: Any,
    data: Any?,
): Exception =
    Exception(
        "Failed to decode the serialized data.",
        userInfo = mapOf("Data" to data.toString()),
        metadata = ExceptionMetadata(sender),
    )

/** Returns an exception describing a failure to decode the given data. */
fun decodingFailed(
    sender: Any,
    data: Any?,
): Exception =
    Exception(
        "Decoding failed.",
        userInfo = mapOf("Data" to data.toString()),
        metadata = ExceptionMetadata(sender),
    )

/** Returns an exception describing a value that could not be serialized. */
fun notSerialized(
    sender: Any,
    data: Any?,
): Exception =
    Exception(
        "Type value must be serialized.",
        userInfo = mapOf("Data" to data.toString()),
        metadata = ExceptionMetadata(sender),
    )

/** Returns an exception describing a serialization key that is not remotely updatable. */
fun notRemotelyUpdatable(
    sender: Any,
    key: String,
): Exception =
    Exception(
        "The specified serialization key is not updatable.",
        userInfo = mapOf("Key" to key),
        metadata = ExceptionMetadata(sender),
    )

/** Returns an exception describing a value whose type does not match its serialization key. */
fun typeMismatch(
    sender: Any,
    key: String,
    type: Any?,
): Exception =
    Exception(
        "Type mismatch for serialization key.",
        userInfo =
            mapOf(
                "Key" to key,
                "MisatchedType" to (type?.let { it::class.simpleName } ?: "null"),
            ),
        metadata = ExceptionMetadata(sender),
    )
