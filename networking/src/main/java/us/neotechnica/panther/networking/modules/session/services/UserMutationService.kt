//
//  UserMutationService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Writes single fields of the user node.
 *
 * Mirrors the iOS `User` remotely-updatable path: the `pushTokens` and
 * `blockedUserIDs` fields are written as incremental map diffs (added
 * keys set to `true`, removed keys set to `null`); scalar fields are
 * written whole. Each write upserts the updated user into the store.
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

    // MARK: - Blocked Users

    /**
     * Replaces the current user's blocked-user set with [blockedUserIDs],
     * writing only the added and removed entries.
     */
    suspend fun setBlockedUserIDsForCurrentUser(blockedUserIDs: List<String>) {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        val current = currentUser.blockedUserIDs ?: emptyList()
        val updated = blockedUserIDs.distinct()
        writeMapFieldDiff("${userPath(currentUser.id)}/$BLOCKED_USER_IDS_KEY", current, updated)
        SessionStore.upsertUser(currentUser.copy(blockedUserIDs = updated.ifEmpty { null }))
    }

    // MARK: - Scalar Fields

    /**
     * Writes the current user's language and previous-language list in a
     * single atomic update on the user node.
     */
    suspend fun updateLanguageForCurrentUser(
        languageCode: String,
        previousLanguageCodes: List<String>,
    ) {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        database.updateChildValues(
            key = userPath(currentUser.id),
            data =
                mapOf(
                    LANGUAGE_CODE_KEY to languageCode,
                    PREVIOUS_LANGUAGE_CODES_KEY to previousLanguageCodes.ifEmpty { bangQualifiedEmptyList },
                ),
        )
        SessionStore.upsertUser(
            currentUser.copy(
                languageCode = languageCode,
                previousLanguageCodes = previousLanguageCodes.ifEmpty { null },
            ),
        )
    }

    /** Sets whether the current user requires message-recipient consent. */
    suspend fun setMessageRecipientConsentRequiredForCurrentUser(required: Boolean) {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))
        database.updateChildValues(userPath(currentUser.id), mapOf(CONSENT_REQUIRED_KEY to required))
        SessionStore.upsertUser(currentUser.copy(messageRecipientConsentRequired = required))
    }

    /** Sets whether the current user has AI-enhanced translations enabled. */
    suspend fun setAIEnhancedTranslationsEnabledForCurrentUser(enabled: Boolean) {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))
        database.updateChildValues(userPath(currentUser.id), mapOf(AI_ENHANCED_KEY to enabled))
        SessionStore.upsertUser(currentUser.copy(aiEnhancedTranslationsEnabled = enabled))
    }

    /** Clears the current user's conversation list (used during deletion). */
    suspend fun clearConversationIDsForCurrentUser() {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))
        database.updateChildValues(userPath(currentUser.id), mapOf(OPEN_CONVERSATIONS_KEY to emptyMap<String, String>()))
        SessionStore.upsertUser(currentUser.copy(conversationIDs = null))
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
    private const val BLOCKED_USER_IDS_KEY = "blockedUserIDs"
    private const val LANGUAGE_CODE_KEY = "languageCode"
    private const val PREVIOUS_LANGUAGE_CODES_KEY = "previousLanguageCodes"
    private const val CONSENT_REQUIRED_KEY = "messageRecipientConsentRequired"
    private const val AI_ENHANCED_KEY = "aiEnhancedTranslationsEnabled"
    private const val OPEN_CONVERSATIONS_KEY = "openConversations"
}
