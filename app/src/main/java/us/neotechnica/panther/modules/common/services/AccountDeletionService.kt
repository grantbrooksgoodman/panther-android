//
//  AccountDeletionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.designsystem.modules.alertkit.models.ProgressAlert
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.update
import us.neotechnica.panther.modules.session.entity.extensions.conversations
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.services.ActivitySessionService
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Permanently deletes the current user's account and its data.
 *
 * While a progress alert and overlay are displayed, adds the user to
 * the deleted-users registry and resolves their conversations, leaves
 * group chats and deletes one-to-one chats, clears the conversation
 * list, then removes the persisted identifier and the remote user
 * record. Individual failures are accumulated and a single compiled
 * exception is thrown at the end.
 *
 * **Note:** the two database integrity-repair passes iOS performs are
 * absent – `IntegrityService` is deferred to a separate plan (D-II-2).
 * The Android progress alert is indeterminate, so no completion
 * percentage is reported.
 */
object AccountDeletionService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Delete Account

    /**
     * Deletes the current user's account.
     *
     * @throws Exception if the current user ID is unset, or if any step
     *   fails (a compiled exception is thrown after all steps run).
     */
    suspend fun deleteAccount() {
        val currentUserID =
            User.currentUserID
                ?: throw Exception("Current user ID has not been set.", metadata = ExceptionMetadata(this))

        UserSessionService.stopObservingCurrentUserChanges()

        val exceptions = mutableListOf<Exception>()
        Overlay.show()
        val progressAlert =
            ProgressAlert(
                title = LocalizedStringKey.DeletingData.localized(),
                message = LocalizedStringKey.PleaseWait.localized(),
            )
        progressAlert.present()

        try {
            // Add to deleted users + resolve conversations, in parallel.
            coroutineScope {
                listOf(
                    async { runCatchingException { addToDeletedUsers(currentUserID) } },
                    async {
                        runCatchingException {
                            UserSessionService.resolveCurrentUser(setOf(UserSessionService.DataType.CONVERSATIONS))
                        }
                    },
                ).awaitAll().forEach { exception -> exception?.let(exceptions::add) }
            }

            val conversations = UserSessionService.currentUser?.conversations ?: emptyList()

            // Remove from group chats, delete one-to-one chats, in parallel.
            coroutineScope {
                conversations
                    .map { conversation ->
                        async {
                            runCatchingException {
                                if (conversation.participants.size > GROUP_PARTICIPANT_THRESHOLD) {
                                    ActivitySessionService.removeFromConversation(
                                        userID = currentUserID,
                                        conversation = conversation,
                                        removeFromUser = false,
                                    )
                                } else {
                                    ConversationSessionService.deleteConversation(conversation, forced = true)
                                }
                            }
                        }
                    }.awaitAll()
                    .forEach { exception -> exception?.let(exceptions::add) }
            }

            // Zero-out conversation IDs after all conversation operations
            // complete to avoid a self-race where a concurrent didWrite
            // fan-out re-adds entries.
            runCatchingException {
                UserSessionService.currentUser?.update(UserUpdatableKey.CONVERSATION_IDS, to = emptyList<ConversationID>())
            }?.let(exceptions::add)

            // iOS repairs database integrity here; deferred per D-II-2.

            Persistent.setString(PersistentStorageKey.currentUserID, null)
            runCatchingException {
                database.setValue(value = null, key = "${NetworkPath.users.rawValue}/$currentUserID")
            }?.let(exceptions::add)

            // iOS repairs database integrity again on errors; deferred per D-II-2.
        } finally {
            progressAlert.dismiss()
            Overlay.hide()
        }

        val first = exceptions.firstOrNull() ?: return
        Logger.log(first)
        throw Exception(
            "Account deletion completed with ${exceptions.size} error(s).",
            underlyingExceptions = exceptions,
            metadata = ExceptionMetadata(this),
        )
    }

    // MARK: - Auxiliary

    private suspend fun addToDeletedUsers(userID: String) {
        database.runTransaction(NetworkPath.deletedUsers.rawValue) { current ->
            val ids = (current as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
            ids.add(userID)
            ids.filter { it.isNotBlank() }.distinct()
        }
    }

    private suspend fun runCatchingException(block: suspend () -> Unit): Exception? =
        try {
            block()
            null
        } catch (exception: Exception) {
            exception
        }

    private const val GROUP_PARTICIPANT_THRESHOLD = 2
}
