//
//  SynchronizationRecord.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.sync.models

import java.util.Date
import kotlin.math.abs
import kotlin.math.min

/**
 * A record of a failed conversation synchronization attempt, used to
 * apply a backoff cooldown before retrying.
 */
class SynchronizationRecord(
    /** The identifier key of the conversation the attempt applies to. */
    val conversationIDKey: String,
    /** The number of the synchronization attempt. */
    val attempt: Int = 1,
    /** The date of the attempt. */
    val date: Date = Date(),
) {
    // MARK: - Computed Properties

    /** Whether the record's cooldown has elapsed. */
    val isExpired: Boolean
        get() = abs((Date().time - date.time) / MILLIS_PER_SECOND) >= COOLDOWNS[min(attempt - 1, COOLDOWNS.size - 1)]

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean =
        other is SynchronizationRecord && other.conversationIDKey == conversationIDKey

    override fun hashCode(): Int = conversationIDKey.hashCode()

    // MARK: - Companion

    private companion object {
        /** Cooldown durations for exponential backoff (seconds). */
        val COOLDOWNS = listOf(3L, 15L, 60L)

        const val MILLIS_PER_SECOND = 1_000L
    }
}
