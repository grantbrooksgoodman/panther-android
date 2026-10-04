//
//  User+RemotelyUpdatable.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.user.remotelyupdatable

import us.neotechnica.panther.modules.networking.common.encodeForWrite
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.typeMismatch
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.common.models.WriteAction
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import java.util.Date

// MARK: - Types

/** The remotely updatable keys of a user. */
enum class UserUpdatableKey(
    val rawValue: String,
) {
    AI_ENHANCED_TRANSLATIONS_ENABLED("aiEnhancedTranslationsEnabled"),
    BLOCKED_USER_IDS("blockedUserIDs"),
    CONVERSATION_IDS("openConversations"),
    DEVICE_ID("deviceID"),
    LANGUAGE_CODE("languageCode"),
    MESSAGE_RECIPIENT_CONSENT_REQUIRED("messageRecipientConsentRequired"),
    PREVIOUS_LANGUAGE_CODES("previousLanguageCodes"),
    PUSH_TOKENS("pushTokens"),
}

// MARK: - Modify Key

/**
 * Returns a copy of the user with the given key set to the given
 * value, or `null` if the value's type does not match the key.
 */
@Suppress("UNCHECKED_CAST")
fun User.modifyKey(
    key: UserUpdatableKey,
    value: Any,
): User? =
    when (key) {
        UserUpdatableKey.AI_ENHANCED_TRANSLATIONS_ENABLED ->
            (value as? Boolean)?.let { copy(aiEnhancedTranslationsEnabled = it) }
        UserUpdatableKey.BLOCKED_USER_IDS ->
            (value as? List<*>)?.filterIsInstance<String>()?.let { copy(blockedUserIDs = it) }
        UserUpdatableKey.CONVERSATION_IDS ->
            (value as? List<*>)?.filterIsInstance<ConversationID>()?.let { copy(conversationIDs = it) }
        UserUpdatableKey.DEVICE_ID ->
            (value as? String)?.let { copy(deviceID = it) }
        UserUpdatableKey.LANGUAGE_CODE ->
            (value as? String)?.let { copy(languageCode = it) }
        UserUpdatableKey.MESSAGE_RECIPIENT_CONSENT_REQUIRED ->
            (value as? Boolean)?.let { copy(messageRecipientConsentRequired = it) }
        UserUpdatableKey.PREVIOUS_LANGUAGE_CODES ->
            (value as? List<*>)?.filterIsInstance<String>()?.let { copy(previousLanguageCodes = it) }
        UserUpdatableKey.PUSH_TOKENS ->
            (value as? List<*>)?.filterIsInstance<String>()?.let { copy(pushTokens = it) }
    }

// MARK: - Update

/** Writes the value for the given key to the server and returns the updated user. */
suspend fun User.update(
    key: UserUpdatableKey,
    to: Any,
): User {
    val newValue = modifyKey(key, to) ?: throw typeMismatch(this, key.rawValue, to)
    val valueKeyPath = "${NetworkPath.users.rawValue}/$id/${key.rawValue}"

    return when (val action = willWrite(to, key, newValue)) {
        is WriteAction.Encoded -> {
            Networking.config.databaseDelegate.setValue(action.value, valueKeyPath)
            didWrite(newValue, key)
        }
        is WriteAction.Handled -> didWrite(action.updated, key)
        WriteAction.Proceed -> {
            Networking.config.databaseDelegate.setValue(encodeForWrite(this, key.rawValue, to), valueKeyPath)
            didWrite(newValue, key)
        }
    }
}

// MARK: - Update Values

/**
 * Applies the given key updates to the user and writes the changed
 * fields to the user node in a single atomic update. Upserts the
 * updated user into the session store.
 *
 * This is the multi-field analog of [update]. Fields written this
 * way bypass the per-field [willWrite] incremental-diff handling, so it
 * is intended for whole-value scalar and list fields rather than the
 * `blockedUserIDs` or `pushTokens` diff fields.
 */
suspend fun User.updateValues(data: Map<UserUpdatableKey, Any>): User {
    var updated = this
    val childValues = mutableMapOf<String, Any?>()

    for ((key, value) in data) {
        updated = updated.modifyKey(key, value) ?: throw typeMismatch(this, key.rawValue, value)
        childValues[key.rawValue] =
            (value as? Date)?.let { DependencyValues.current.timestampDateFormatter.format(it) }
                ?: encodeForWrite(this, key.rawValue, value)
    }

    Networking.config.databaseDelegate.updateChildValues(
        key = "${NetworkPath.users.rawValue}/$id",
        data = childValues,
    )
    SessionStore.upsertUser(updated)
    return updated
}

// MARK: - Will Write

/**
 * Prepares a single-field remote update before it is written. For the
 * blocked-user and push-token fields, commits an incremental add-and-
 * remove update rather than replacing the field. For date-valued
 * fields, encodes the date to its wire format.
 */
suspend fun User.willWrite(
    value: Any,
    key: UserUpdatableKey,
    updated: User,
): WriteAction<User> =
    when (key) {
        UserUpdatableKey.BLOCKED_USER_IDS -> {
            val updates =
                merge(
                    updates = updated.blockedUserIDs,
                    currentData = blockedUserIDs,
                    at = "${NetworkPath.users.rawValue}/$id/${key.rawValue}",
                )
            if (updates != null) Networking.config.databaseDelegate.commit(updates)
            WriteAction.Handled(updated)
        }
        UserUpdatableKey.PUSH_TOKENS -> {
            val updates =
                merge(
                    updates = updated.pushTokens,
                    currentData = pushTokens,
                    at = "${NetworkPath.users.rawValue}/$id/${key.rawValue}",
                )
            if (updates != null) Networking.config.databaseDelegate.commit(updates)
            WriteAction.Handled(updated)
        }
        else ->
            (value as? Date)?.let {
                WriteAction.Encoded(DependencyValues.current.timestampDateFormatter.format(it))
            } ?: WriteAction.Proceed
    }

// MARK: - Did Write

/** Applies a completed single-field remote update, upserting the updated user into the session store. */
@Suppress("UnusedParameter") // `key` matches the shared updatable-type write signature.
fun User.didWrite(
    updated: User,
    key: UserUpdatableKey,
): User {
    SessionStore.upsertUser(updated)
    return updated
}

// MARK: - Auxiliary

private fun merge(
    updates: List<String>?,
    currentData: List<String>?,
    at: String,
): Map<String, Any?>? {
    val current = (currentData ?: emptyList()).toSet()
    val new = (updates ?: emptyList()).toSet()
    if (current == new) return null

    val result = mutableMapOf<String, Any?>()
    for (added in new - current) result["$at/$added"] = true
    for (removed in current - new) result["$at/$removed"] = null
    return result.ifEmpty { null }
}
