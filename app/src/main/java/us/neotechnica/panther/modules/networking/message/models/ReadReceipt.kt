//
//  ReadReceipt.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.models

import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
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

        override fun decode(data: String): ReadReceipt {
            ReadReceiptCache.cachedValue(data)?.let { return it }

            val components = data.split(" | ")
            val readDate =
                components.getOrNull(1)?.let {
                    DependencyValues.current.timestampDateFormatter.parse(it)
                }

            if (components.size != 2 || components[0].isBangQualifiedEmpty || readDate == null) {
                throw decodingFailure(this, data)
            }

            val readReceipt =
                ReadReceipt(
                    userID = components[0],
                    readDate = readDate,
                )
            ReadReceiptCache.cache(data, readReceipt)
            return readReceipt
        }
    }
}

/**
 * Manages the in-memory read-receipt cache, keyed by encoded string.
 * Mirrors the iOS `ReadReceiptCache`.
 */
object ReadReceiptCache {
    // MARK: - Properties

    private val cachedReadReceiptsForEncodedStrings = LockIsolated<Map<String, ReadReceipt>?>(null)

    // MARK: - Methods

    /** Removes every cached read receipt. */
    fun clearCache() {
        cachedReadReceiptsForEncodedStrings.wrappedValue = null
    }

    internal fun cachedValue(data: String): ReadReceipt? = cachedReadReceiptsForEncodedStrings.wrappedValue?.get(data)

    internal fun cache(
        data: String,
        readReceipt: ReadReceipt,
    ) {
        val cache = (cachedReadReceiptsForEncodedStrings.wrappedValue ?: emptyMap()).toMutableMap()
        cache[data] = readReceipt
        cachedReadReceiptsForEncodedStrings.wrappedValue = cache
    }
}
