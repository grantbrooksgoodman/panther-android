//
//  UserMutationService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.user.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore

/**
 * Writes the current user's push-token field.
 *
 * The `pushTokens` field is written as an incremental map diff (added
 * keys set to `true`, removed keys set to `null`). Every other user
 * field is now written through the `User` remotely-updatable path
 * (`User.update`/`User.updateValues`); the push-token members here
 * move to a dedicated `PushTokenService` in Phase 5.6.
 */
object UserMutationService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate
    private val currentTokenRef = LockIsolated<String?>(null)

    // MARK: - Computed Properties

    /** The device's current push token, or `null` if not yet set. */
    val currentToken: String?
        get() = currentTokenRef.wrappedValue

    // MARK: - Push Tokens

    /** Sets the device's current push token. */
    fun setCurrentToken(token: String?) {
        currentTokenRef.wrappedValue = token
    }

    /** Adds the device's current push token to the current user's record. */
    suspend fun updatePushTokensForCurrentUser() {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", isReportable = false, metadata = ExceptionMetadata(this))
        val token =
            currentToken
                ?: throw Exception("Push token has not been set.", isReportable = false, metadata = ExceptionMetadata(this))

        val tokens = currentUser.pushTokens ?: emptyList()
        if (token in tokens) return

        val updated = (tokens + token).distinct()
        writeMapFieldDiff("${userPath(currentUser.id)}/$PUSH_TOKENS_KEY", tokens, updated)
        SessionStore.upsertUser(currentUser.copy(pushTokens = updated))
    }

    /** Removes the current user's push tokens from every other user's record. */
    suspend fun prunePushTokensForCurrentUser() {
        val currentUser = UserSessionService.currentUser ?: return
        val currentUserTokens = currentUser.pushTokens?.toSet() ?: return
        if (currentUserTokens.isEmpty()) return

        val userData: Map<String, Any?> = database.getValues(NetworkPath.users.rawValue)
        val updates = mutableMapOf<String, Any?>()

        for ((userID, value) in userData.filterKeys { it != currentUser.id }) {
            val tokenMap = pushTokenMap(value) ?: continue
            tokenMap.keys
                .filter { it in currentUserTokens }
                .forEach { token -> updates["${userPath(userID)}/$PUSH_TOKENS_KEY/$token"] = null }
        }

        if (updates.isEmpty()) return
        database.commit(updates)
        Logger.log("Pruned push tokens for current user.")
    }

    /** Removes the given push token from every user that holds it. */
    suspend fun eraseStalePushToken(token: String) {
        val userData: Map<String, Any?> = database.getValues(NetworkPath.users.rawValue)
        val updates = mutableMapOf<String, Any?>()

        for ((userID, value) in userData) {
            val tokenMap = pushTokenMap(value)
            if (tokenMap != null && tokenMap[token] != null) {
                updates["${userPath(userID)}/$PUSH_TOKENS_KEY/$token"] = null
            }
        }

        if (updates.isEmpty()) return
        database.commit(updates)
        Logger.log("Erased stale push token for ${updates.size} users.")
    }

    // MARK: - Auxiliary

    private fun userPath(userID: String): String = "${NetworkPath.users.rawValue}/$userID"

    private fun pushTokenMap(value: Any?): Map<*, *>? {
        val map = value as? Map<*, *> ?: return null
        return map[PUSH_TOKENS_KEY] as? Map<*, *>
    }

    private suspend fun writeMapFieldDiff(
        path: String,
        current: List<String>,
        new: List<String>,
    ) {
        val currentSet = current.toSet()
        val newSet = new.toSet()
        if (currentSet == newSet) return

        val updates = mutableMapOf<String, Any?>()
        for (added in newSet - currentSet) updates["$path/$added"] = true
        for (removed in currentSet - newSet) updates["$path/$removed"] = null
        database.commit(updates)
    }

    // MARK: - Companion

    private const val PUSH_TOKENS_KEY = "pushTokens"
}
