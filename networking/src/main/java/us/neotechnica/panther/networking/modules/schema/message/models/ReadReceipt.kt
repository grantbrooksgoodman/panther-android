//
//  ReadReceipt.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.message.models

import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import java.util.Date

/**
 * A record of when a user read a message.
 *
 * Serializes as `"<userID> | <timestamp>"`.
 */
data class ReadReceipt(
    /** The identifier of the user who read the message. */
    val userID: String,
    /** The date the user read the message. */
    val readDate: Date,
) : Serializable<String> {
    // MARK: - Computed Properties

    /** The serialized representation of the read receipt. */
    override val encoded: String
        get() = "$userID | ${DependencyValues.current.timestampDateFormatter.format(readDate)}"

    // MARK: - Companion

    companion object : SerializableDecoder<ReadReceipt, String> {
        override fun canDecode(data: String): Boolean {
            val components = data.split(" | ")
            return components.size == 2 &&
                !components[0].isBangQualifiedEmpty &&
                DependencyValues.current.timestampDateFormatter.parse(components[1]) != null
        }

        override suspend fun decode(data: String): ReadReceipt {
            val components = data.split(" | ")
            val readDate =
                components.getOrNull(1)?.let {
                    DependencyValues.current.timestampDateFormatter.parse(it)
                }

            if (components.size != 2 || components[0].isBangQualifiedEmpty || readDate == null) {
                throw decodingFailure(this, data)
            }

            return ReadReceipt(
                userID = components[0],
                readDate = readDate,
            )
        }
    }
}
