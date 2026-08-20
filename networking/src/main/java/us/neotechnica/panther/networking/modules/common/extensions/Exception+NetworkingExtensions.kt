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
