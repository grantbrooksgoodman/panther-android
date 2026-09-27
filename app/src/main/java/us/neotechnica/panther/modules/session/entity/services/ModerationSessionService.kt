//
//  ModerationSessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.update

/**
 * Blocks, unblocks, and reports users.
 *
 * Blocking updates the current user's `blockedUserIDs`; reporting
 * increments a per-user counter under `reportedUsers` in a transaction.
 */
object ModerationSessionService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Block

    /** Blocks the users with the given identifiers for the current user. */
    suspend fun blockUsers(userIDs: List<String>) {
        val cleaned = userIDs.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) throw Exception("No user IDs provided.", metadata = ExceptionMetadata(this))

        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))
        val current = currentUser.blockedUserIDs ?: emptyList()
        currentUser.update(UserUpdatableKey.BLOCKED_USER_IDS, to = (current + cleaned).distinct())
    }

    /** Unblocks the users with the given identifiers for the current user. */
    suspend fun unblockUsers(userIDs: List<String>) {
        val cleaned = userIDs.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) throw Exception("No user IDs provided.", metadata = ExceptionMetadata(this))

        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))
        val current = currentUser.blockedUserIDs ?: emptyList()
        currentUser.update(UserUpdatableKey.BLOCKED_USER_IDS, to = current.filter { it !in cleaned })
    }

    // MARK: - Report

    /** Reports the users with the given identifiers, incrementing their report counts. */
    suspend fun reportUsers(userIDs: List<String>) {
        val cleaned = userIDs.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) throw Exception("No user IDs provided.", metadata = ExceptionMetadata(this))

        database.runTransaction(NetworkPath.reportedUsers.rawValue) { current ->
            @Suppress("UNCHECKED_CAST")
            val counts = (current as? Map<String, Any?>).orEmpty().toMutableMap()
            for (userID in cleaned) {
                counts[userID] = ((counts[userID] as? Number)?.toInt() ?: 0) + 1
            }
            counts
        }
    }
}
