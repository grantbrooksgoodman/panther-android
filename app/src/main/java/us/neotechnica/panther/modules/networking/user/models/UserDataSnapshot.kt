//
//  UserDataSnapshot.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.user.models

import java.util.Date
import kotlin.math.abs
import kotlin.time.Duration

/**
 * A time-stamped snapshot of a user's serialized data, used for
 * short-lived caching.
 */
class UserDataSnapshot(
    /** The date the snapshot was captured. */
    val date: Date = Date(),
    /** The user's serialized data. */
    val data: Map<String, Any?>,
    /** The duration after which the snapshot is considered expired. */
    val expiryThreshold: Duration,
) {
    // MARK: - Computed Properties

    /** A Boolean value that indicates whether the snapshot has expired. */
    val isExpired: Boolean
        get() = abs(Date().time - date.time) > expiryThreshold.inWholeMilliseconds

    // MARK: - Companion

    companion object {
        /** An empty, already-expired snapshot. */
        val empty: UserDataSnapshot
            get() =
                UserDataSnapshot(
                    date = Date(0),
                    data = emptyMap(),
                    expiryThreshold = Duration.ZERO,
                )
    }
}
