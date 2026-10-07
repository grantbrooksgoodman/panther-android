//
//  AccountDeletionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.bundle.currentUserID
import us.neotechnica.panther.designsystem.modules.alertkit.models.ProgressAlert
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
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
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.localization.models.localized

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
 * A determinate progress bar tracks the per-conversation work. The two database integrity-repair passes are out of scope
 * (D-III-10).
 */
object AccountDeletionService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate
    private val completedUnits = LockIsolated(0.0)
    private var progressAlert: ProgressAlert? = null

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
        completedUnits.wrappedValue = 0.0
        Overlay.addOverlay(alpha = OVERLAY_ALPHA)
        val progressAlert =
            ProgressAlert(
                title = LocalizedStringKey.DeletingData.localized(),
                message = LocalizedStringKey.PleaseWait.localized(),
            )
        this.progressAlert = progressAlert
        progressAlert.present(translating = emptyList())

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
            val groupChats = conversations.filter { it.participants.size > GROUP_PARTICIPANT_THRESHOLD }
            val oneToOneChats = conversations.filter { it.participants.size == ONE_TO_ONE_PARTICIPANT_COUNT }
            val totalUnits = (groupChats.size + oneToOneChats.size).toDouble()

            // Remove from group chats, delete one-to-one chats, in parallel.
            coroutineScope {
                val groupTasks =
                    groupChats.map { conversation ->
                        async {
                            runCatchingException {
                                ActivitySessionService.removeFromConversation(
                                    userID = currentUserID,
                                    conversation = conversation,
                                    removeFromUser = false,
                                )
                            }
                        }
                    }
                val oneToOneTasks =
                    oneToOneChats.map { conversation ->
                        async { runCatchingException { ConversationSessionService.deleteConversation(conversation, forced = true) } }
                    }

                (groupTasks + oneToOneTasks).forEach { task ->
                    task.await()?.let(exceptions::add)
                    incrementProgress(totalUnits)
                }
            }

            // Zero-out conversation IDs after all conversation operations
            // complete to avoid a self-race where a concurrent didWrite
            // fan-out re-adds entries.
            runCatchingException {
                UserSessionService.currentUser?.update(UserUpdatableKey.CONVERSATION_IDS, to = emptyList<ConversationID>())
            }?.let(exceptions::add)

            // The two database integrity-repair passes are out of scope
            // (D-III-10).

            progressAlert.updateProgress(1.0)
            Persistent.setString(PersistentStorageKey.currentUserID, null)
            runCatchingException {
                database.setValue(value = null, key = "${NetworkPath.users.rawValue}/$currentUserID")
            }?.let(exceptions::add)
        } finally {
            this.progressAlert = null
            progressAlert.dismiss()
            Overlay.removeOverlay()
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

    private fun incrementProgress(total: Double) {
        val percent =
            completedUnits.withValue { reference ->
                reference.value += 1
                reference.value / maxOf(total, 1.0)
            }
        progressAlert?.updateProgress(percent)
    }

    private const val GROUP_PARTICIPANT_THRESHOLD = 2
    private const val ONE_TO_ONE_PARTICIPANT_COUNT = 2
    private const val OVERLAY_ALPHA = 0.5f
}
