//
//  PushTokenService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.update
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.getValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Manages the device push notification tokens registered for users.
 *
 * Each user's remote record holds the push tokens of the devices on
 * which they are signed in.
 */
object PushTokenService {
    // MARK: - Properties

    private val currentTokenRef = LockIsolated<String?>(null)

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Computed Properties

    /** The device's current push token, or `null` if one has not been set. */
    val currentToken: String?
        get() = currentTokenRef.wrappedValue

    // MARK: - Set Push Token

    /**
     * Sets the device's current push token.
     *
     * @param currentToken The token to set; pass `null` to clear it.
     */
    fun setCurrentToken(currentToken: String?) {
        currentTokenRef.wrappedValue = currentToken
    }

    // MARK: - Erase Stale Push Token

    /**
     * Removes the given push token from every user that holds it.
     *
     * All removals are committed as a single batched update. If no
     * user holds the token, this method returns immediately.
     *
     * @param pushToken The stale token to remove.
     *
     * @throws Exception if fetching user data or committing the
     *   update fails.
     */
    suspend fun eraseStalePushToken(pushToken: String) {
        val userData: Map<String, Any?> = database.getValues<Map<String, Any?>>(NetworkPath.users.rawValue)

        // Build a single fan-out that deletes the stale token from
        // every user that has it.
        val updates = mutableMapOf<String, Any?>()
        for ((userID, value) in userData) {
            val pushTokenMap = pushTokenMap(value)
            if (pushTokenMap != null && pushTokenMap[pushToken] != null) {
                updates["${userPath(userID)}/$PUSH_TOKENS_KEY/$pushToken"] = null
            }
        }

        if (updates.isEmpty()) return
        database.commit(updates)
        Logger.log("Erased stale push token for ${updates.size} users.")
    }

    // MARK: - Update Push Tokens for Current User

    /**
     * Adds the device's current push token to the current user's
     * remote record.
     *
     * @throws Exception if the current user or token has not been
     *   set, if the token is already registered, or if the update
     *   fails.
     */
    suspend fun updatePushTokensForCurrentUser() {
        val currentUser = UserSessionService.currentUser
        val token = currentToken
        if (currentUser == null || token == null) {
            throw Exception(
                "Either current user or push token has not been set.",
                isReportable = false,
                metadata = ExceptionMetadata(this),
            )
        }

        val pushTokens = currentUser.pushTokens ?: emptyList()
        if (pushTokens.contains(token)) {
            throw Exception(
                "Push tokens already up to date.",
                isReportable = false,
                metadata = ExceptionMetadata(this),
            )
        }

        currentUser.update(UserUpdatableKey.PUSH_TOKENS, to = (pushTokens + token).distinct())
    }

    // MARK: - Prune Push Tokens for Current User

    /**
     * Removes the current user's push tokens from every other user's
     * remote record.
     *
     * All removals are committed as a single batched update. If the
     * current user has no push tokens, this method returns
     * immediately.
     *
     * @throws Exception if fetching user data or committing the
     *   update fails.
     */
    suspend fun prunePushTokensForCurrentUser() {
        val currentUser = UserSessionService.currentUser ?: return
        val currentUserPushTokens = currentUser.pushTokens?.toSet() ?: return
        if (currentUserPushTokens.isEmpty()) return

        val userData: Map<String, Any?> = database.getValues<Map<String, Any?>>(NetworkPath.users.rawValue)

        // Build a single fan-out that removes the current user's
        // tokens from all other users.
        val updates = mutableMapOf<String, Any?>()
        for ((userID, value) in userData.filterKeys { it != currentUser.id }) {
            val pushTokenMap = pushTokenMap(value) ?: continue
            pushTokenMap.keys
                .filter { it in currentUserPushTokens }
                .forEach { token -> updates["${userPath(userID)}/$PUSH_TOKENS_KEY/$token"] = null }
        }

        if (updates.isEmpty()) return
        database.commit(updates)
        Logger.log("Pruned push tokens for current user.")
    }

    // MARK: - Auxiliary

    private fun pushTokenMap(value: Any?): Map<*, *>? {
        val map = value as? Map<*, *> ?: return null
        return map[PUSH_TOKENS_KEY] as? Map<*, *>
    }

    private fun userPath(userID: String): String = "${NetworkPath.users.rawValue}/$userID"

    private const val PUSH_TOKENS_KEY = "pushTokens"
}
