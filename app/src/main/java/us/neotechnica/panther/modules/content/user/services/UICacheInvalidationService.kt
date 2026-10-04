//
//  UICacheInvalidationService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.modules.common.constants.NotificationExtensionConstants
import us.neotechnica.panther.modules.content.user.extensions.UserDisplayNameCache
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewDataCache
import us.neotechnica.panther.modules.networking.message.models.ReadReceiptCache
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import kotlin.time.Duration.Companion.milliseconds

/**
 * Invalidates display caches in response to session store changes.
 *
 * Keeps derived display data – conversation cell view data, read
 * receipts, user display names, and the group conversation name map
 * read by the notification delivery path – consistent with the session
 * store. Changes are handled after a short delay; rapid successive
 * changes coalesce into a single invalidation.
 */
object UICacheInvalidationService {
    // MARK: - Types

    private enum class TaskID(
        val rawValue: String,
    ) {
        CONVERSATION_INVALIDATION("conversationInvalidation"),
        MESSAGE_INVALIDATION("messageInvalidation"),
        NOTIFICATION_EXTENSION_NAME_MAP("notificationExtensionNameMap"),
        USER_INVALIDATION("userInvalidation"),
    }

    // MARK: - Properties

    private val observationJob = LockIsolated<Job?>(null)
    private val pendingConversationIDKeys = LockIsolated(setOf<String>())
    private val pendingUserIDs = LockIsolated(setOf<String>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // MARK: - Methods

    /** Mirrors the current group conversation names to persistent storage. */
    fun refreshNotificationExtensionNameMap() {
        persistValuesForNotificationExtension()
    }

    /** Begins observing session store changes, if not already observing. */
    fun startObserving() {
        observationJob.withValue {
            if (it.value != null) return@withValue
            it.value =
                scope.launch {
                    DependencyValues.current.sharedEvents.sessionStoreDidChange.events.collect { change ->
                        handleChange(change)
                    }
                }
        }
    }

    // MARK: - Auxiliary

    private fun handleChange(change: SessionStoreChange) {
        when (change) {
            is SessionStoreChange.Conversations ->
                handleConversationsChange(change.upsertedIDKeys + change.removedIDKeys)

            is SessionStoreChange.Messages -> handleMessagesChange()

            is SessionStoreChange.Users ->
                handleUsersChange(change.upsertedIDs + change.removedIDs)
        }
    }

    private fun handleConversationsChange(affectedIDKeys: Set<String>) {
        if (affectedIDKeys.isEmpty()) return

        pendingConversationIDKeys.withValue { it.value = it.value + affectedIDKeys }
        Task.debounced("$SENDER/${TaskID.CONVERSATION_INVALIDATION.rawValue}", INVALIDATION_DELAY) {
            Logger.log("Invalidating caches for conversation changes.", domain = LoggerDomain.uiCacheInvalidation)
            val idKeys = pendingConversationIDKeys.withValue { current -> current.value.also { current.value = emptySet() } }
            ConversationCellViewDataCache.removeValues(idKeys)
        }

        Task.debounced("$SENDER/${TaskID.NOTIFICATION_EXTENSION_NAME_MAP.rawValue}", NAME_MAP_DELAY) {
            persistValuesForNotificationExtension()
        }
    }

    private fun handleMessagesChange() {
        Task.debounced("$SENDER/${TaskID.MESSAGE_INVALIDATION.rawValue}", INVALIDATION_DELAY) {
            Logger.log("Invalidating caches for message changes.", domain = LoggerDomain.uiCacheInvalidation)
            ConversationCellViewDataCache.clearCache()
            ReadReceiptCache.clearCache()
        }
    }

    private fun handleUsersChange(affectedIDs: Set<String>) {
        pendingUserIDs.withValue { it.value = it.value + affectedIDs }
        Task.debounced("$SENDER/${TaskID.USER_INVALIDATION.rawValue}", INVALIDATION_DELAY) {
            Logger.log("Invalidating caches for user changes.", domain = LoggerDomain.uiCacheInvalidation)
            val ids = pendingUserIDs.withValue { current -> current.value.also { current.value = emptySet() } }
            ConversationCellViewDataCache.clearCache()
            UserDisplayNameCache.removeValues(ids)
        }
    }

    private fun persistValuesForNotificationExtension() {
        val json = JSONObject()
        SessionStore.conversations.values
            .filter { it.participants.size > GROUP_PARTICIPANT_THRESHOLD }
            .forEach { json.put(it.id.key, ConversationCellViewData.title(it)) }

        Persistent.setString(
            PersistentStorageKey(NotificationExtensionConstants.CONVERSATION_NAME_MAP_KEY),
            json.toString(),
        )
    }

    private const val GROUP_PARTICIPANT_THRESHOLD = 2
    private val INVALIDATION_DELAY = 250.milliseconds
    private val NAME_MAP_DELAY = 500.milliseconds
    private const val SENDER = "UICacheInvalidationService"
}
