//
//  CoreUtilities+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.update
import us.neotechnica.panther.modules.session.entity.extensions.conversations
import us.neotechnica.panther.modules.session.entity.extensions.isVisibleForCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.realMessageIDs
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities

// MARK: - Types

/** The set of conversations targeted by [deleteConversations]. */
enum class ConversationDeletionGranularity {
    /** Every conversation associated with the current user. */
    ALL_FOR_CURRENT_USER,

    /** Group conversations that have neither a name nor a photo. */
    GROUP_CHATS_WITHOUT_NAME_OR_PHOTO,

    /** Conversations that are not visible to the current user. */
    NOT_VISIBLE_FOR_CURRENT_USER,

    /** One-to-one conversations containing fewer than five messages. */
    ONE_TO_ONE_AND_FEWER_THAN_FIVE_MESSAGES,
}

// MARK: - Methods

/**
 * Resets the current user's previous language code history to an empty
 * state.
 *
 * @throws Exception if the current user has not been set, or if the
 *   update fails.
 */
suspend fun CoreUtilities.clearPreviousLanguageCodes() {
    val currentUser =
        UserSessionService.currentUser
            ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))
    currentUser.update(UserUpdatableKey.PREVIOUS_LANGUAGE_CODES, to = bangQualifiedEmptyList)
}

/**
 * Deletes the current user's conversations matching the given
 * granularity.
 *
 * This method resolves the current user's conversations and messages,
 * computes the set of conversations matching the granularity, and
 * deletes each one. Current-user change observation is stopped and not
 * restarted.
 *
 * @param granularity The set of conversations to delete.
 *
 * @throws Exception if the conversations cannot be resolved, or if
 *   deletion fails.
 */
suspend fun CoreUtilities.deleteConversations(granularity: ConversationDeletionGranularity) {
    UserSessionService.resolveCurrentUser(
        setOf(
            UserSessionService.DataType.CONVERSATIONS,
            UserSessionService.DataType.MESSAGES,
        ),
    )

    val conversations =
        UserSessionService.currentUser?.conversations
            ?: throw Exception("Failed to resolve conversation ID keys.", metadata = ExceptionMetadata(this))

    val targets = conversations.filter { it.matches(granularity) }
    UserSessionService.stopObservingCurrentUserChanges()
    targets.forEach { ConversationSessionService.deleteConversation(it, forced = true) }
}

/**
 * Removes the push notification tokens of every user in the remote
 * database.
 *
 * @throws Exception if any database operation fails.
 */
suspend fun CoreUtilities.resetPushTokens() {
    val database = Networking.config.databaseDelegate
    val userData: Map<String, Any?> = database.getValues(NetworkPath.users.rawValue, cacheStrategy = CacheStrategy.DISREGARD_CACHE)
    val updates =
        userData.keys.associate { userID ->
            listOf(NetworkPath.users.rawValue, userID, PUSH_TOKENS_KEY).joinToString("/") to null
        }

    if (updates.isNotEmpty()) database.commit(updates)
}

// MARK: - Auxiliary

internal fun Conversation.matches(granularity: ConversationDeletionGranularity): Boolean =
    when (granularity) {
        ConversationDeletionGranularity.ALL_FOR_CURRENT_USER -> true

        ConversationDeletionGranularity.GROUP_CHATS_WITHOUT_NAME_OR_PHOTO ->
            metadata.name.isBangQualifiedEmpty && metadata.imageData == null && participants.size > 2

        ConversationDeletionGranularity.NOT_VISIBLE_FOR_CURRENT_USER -> !isVisibleForCurrentUser

        ConversationDeletionGranularity.ONE_TO_ONE_AND_FEWER_THAN_FIVE_MESSAGES ->
            realMessageIDs.size < ONE_TO_ONE_MINIMUM_MESSAGE_COUNT && participants.size == ONE_TO_ONE_PARTICIPANT_COUNT
    }

private const val ONE_TO_ONE_MINIMUM_MESSAGE_COUNT = 5
private const val ONE_TO_ONE_PARTICIPANT_COUNT = 2
private const val PUSH_TOKENS_KEY = "pushTokens"
