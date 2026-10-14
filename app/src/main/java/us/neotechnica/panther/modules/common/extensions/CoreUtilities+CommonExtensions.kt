//
//  CoreUtilities+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.modules.content.user.extensions.ignoredConversationIDKeys
import us.neotechnica.panther.modules.content.user.extensions.isConsentMessage
import us.neotechnica.panther.modules.content.user.extensions.isVisibleForCurrentUser
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.update
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.getValues
import us.neotechnica.panther.subsystem.modules.foundation.extensions.compiledException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities

// MARK: - Types

/** The set of conversations targeted by [deleteConversations]. */
enum class ConversationDeletionGranularity {
    /**
     * Every conversation associated with the current user, including
     * ignored conversations.
     */
    ALL_FOR_CURRENT_USER,

    /** Group conversations that have neither a name nor a photo. */
    GROUP_CHATS_WITHOUT_NAME_OR_PHOTO,

    /**
     * Conversations in which the message recipient consent flow has
     * been engaged.
     */
    MESSAGE_RECIPIENT_CONSENT_ENABLED,

    /**
     * Conversations that are not visible to the current user, including
     * ignored conversations.
     */
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
 * computes the set of conversation identifiers matching the
 * granularity, and deletes each one. While the operation runs, an
 * activity indicator overlay is displayed and all database and storage
 * caching is disabled.
 *
 * **Important:** This method stops current-user change observation and
 * does not restart it.
 *
 * @param granularity The set of conversations to delete.
 *
 * @throws Exception if the conversation identifiers cannot be
 *   resolved, or if deletion fails for any conversation.
 */
suspend fun CoreUtilities.deleteConversations(granularity: ConversationDeletionGranularity) {
    UserSessionService.resolveCurrentUser(
        setOf(
            User.DataType.CONVERSATIONS,
            User.DataType.MESSAGES,
        ),
    )

    val currentUser = UserSessionService.currentUser
    val ignoredConversationIDKeys = SessionStore.ignoredConversationIDKeys
    val conversationIDKeys =
        when (granularity) {
            ConversationDeletionGranularity.ALL_FOR_CURRENT_USER ->
                currentUser?.conversationIDs?.map { it.key }?.plus(ignoredConversationIDKeys)

            ConversationDeletionGranularity.NOT_VISIBLE_FOR_CURRENT_USER ->
                currentUser
                    ?.conversations
                    ?.filter { it.matches(granularity) }
                    ?.map { it.id.key }
                    ?.plus(ignoredConversationIDKeys)

            else -> currentUser?.conversations?.filter { it.matches(granularity) }?.map { it.id.key }
        } ?: throw Exception("Failed to resolve conversation ID keys.", metadata = ExceptionMetadata(this))

    val database = Networking.config.databaseDelegate
    val storage = Networking.config.storageDelegate

    try {
        Overlay.addOverlay(
            alpha = DELETION_OVERLAY_ALPHA,
            activityIndicator = Overlay.OverlayActivityIndicatorConfiguration.largeWhite,
        )

        database.setGlobalCacheStrategy(CacheStrategy.DISREGARD_CACHE)
        storage.setGlobalCacheStrategy(CacheStrategy.DISREGARD_CACHE)

        UserSessionService.stopObservingCurrentUserChanges()

        val exceptions = mutableListOf<Exception>()
        for (conversationIDKey in conversationIDKeys.distinct()) {
            try {
                val conversation =
                    SessionStore.getConversation(conversationIDKey)
                        ?: throw Exception("No conversation for ID key $conversationIDKey.", metadata = ExceptionMetadata(this))
                ConversationSessionService.deleteConversation(conversation, forced = true)
            } catch (exception: Exception) {
                exceptions.add(exception)
            }
        }

        exceptions.compiledException?.let { throw it }
    } finally {
        database.setGlobalCacheStrategy(null)
        storage.setGlobalCacheStrategy(null)
        Overlay.removeOverlay()
    }
}

/**
 * Removes the push notification tokens of every user in the remote
 * database.
 *
 * @throws Exception if any database operation fails.
 */
suspend fun CoreUtilities.resetPushTokens() {
    val database = Networking.config.databaseDelegate
    val userData: Map<String, Any?> =
        database.getValues<Map<String, Any?>>(
            NetworkPath.users.rawValue,
            cacheStrategy = CacheStrategy.DISREGARD_CACHE,
        )

    val updates =
        userData.keys.associate { userID ->
            listOf(NetworkPath.users.rawValue, userID, User.SerializableKey.PUSH_TOKENS.rawValue).joinToString("/") to null
        }

    if (updates.isNotEmpty()) database.commit(updates)
}

// MARK: - Auxiliary

internal fun Conversation.matches(granularity: ConversationDeletionGranularity): Boolean =
    when (granularity) {
        ConversationDeletionGranularity.ALL_FOR_CURRENT_USER -> true

        ConversationDeletionGranularity.GROUP_CHATS_WITHOUT_NAME_OR_PHOTO ->
            metadata.name.isBangQualifiedEmpty && metadata.imageData == null && participants.size > 2

        ConversationDeletionGranularity.MESSAGE_RECIPIENT_CONSENT_ENABLED ->
            messages?.any { it.isConsentMessage } == true || metadata.requiresConsentFromInitiator != null

        ConversationDeletionGranularity.NOT_VISIBLE_FOR_CURRENT_USER -> !isVisibleForCurrentUser

        ConversationDeletionGranularity.ONE_TO_ONE_AND_FEWER_THAN_FIVE_MESSAGES ->
            messageIDs.size < ONE_TO_ONE_MINIMUM_MESSAGE_COUNT && participants.size == ONE_TO_ONE_PARTICIPANT_COUNT
    }

private const val DELETION_OVERLAY_ALPHA = 0.5f
private const val ONE_TO_ONE_MINIMUM_MESSAGE_COUNT = 5
private const val ONE_TO_ONE_PARTICIPANT_COUNT = 2
