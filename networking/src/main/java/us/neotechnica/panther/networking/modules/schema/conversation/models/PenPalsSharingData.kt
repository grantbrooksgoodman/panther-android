//
//  PenPalsSharingData.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.conversation.models

import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder

/**
 * A record of which users a participant shares PenPals data with.
 *
 * Serializes as `"<userID>: <id1>, <id2>, …"`, or `"<userID>: !"`
 * when the participant shares with no one.
 */
data class PenPalsSharingData(
    /** The identifier of the sharing user. */
    val userID: String,
    /** The identifiers of the users shared with, or `null` if none. */
    val sharesDataWithUserIDs: List<String>?,
) : Serializable<String> {
    // MARK: - Computed Properties

    /** The serialized representation of the sharing record. */
    override val encoded: String
        get() {
            val list =
                sharesDataWithUserIDs
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(", ")
                    ?: BANG_QUALIFIED_EMPTY
            return "$userID: $list"
        }

    // MARK: - Companion

    companion object : SerializableDecoder<PenPalsSharingData, String> {
        override fun canDecode(data: String): Boolean {
            val components = data.split(": ")
            return components.size == 2 && components.all { it.isNotBlank() }
        }

        override suspend fun decode(data: String): PenPalsSharingData {
            if (!canDecode(data)) throw decodingFailure(this, data)

            val components = data.split(": ")
            val sharesDataWithUserIDs = components[1].split(", ")

            return PenPalsSharingData(
                userID = components[0],
                sharesDataWithUserIDs =
                    if (sharesDataWithUserIDs.isBangQualifiedEmpty) {
                        null
                    } else {
                        sharesDataWithUserIDs
                    },
            )
        }
    }
}
