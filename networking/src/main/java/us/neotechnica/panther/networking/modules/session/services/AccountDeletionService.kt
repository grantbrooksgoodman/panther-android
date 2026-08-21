//
//  AccountDeletionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.conversations
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Permanently deletes the current user's account and its data.
 *
 * Adds the user to the deleted-users registry, leaves group chats and
 * deletes one-to-one chats, clears the conversation list, then removes
 * the persisted identifier and the remote user record.
 *
 * **Note:** unlike iOS, this Phase 8 port does not run a database
 * integrity-repair pass; individual failures are accumulated and logged.
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

        runStep(exceptions) { addToDeletedUsers(currentUserID) }
        runStep(exceptions) { UserSessionService.resolveCurrentUser(setOf(UserSessionService.DataType.CONVERSATIONS)) }

        val conversations = UserSessionService.currentUser?.conversations ?: emptyList()
        for (conversation in conversations) {
            runStep(exceptions) {
                if (conversation.participants.size > 2) {
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

        runStep(exceptions) { UserMutationService.clearConversationIDsForCurrentUser() }

        Persistent.setString(PersistentStorageKey.currentUserID, null)
        runStep(exceptions) {
            database.setValue(value = null, key = "${NetworkPath.users.rawValue}/$currentUserID")
        }

        val first = exceptions.firstOrNull() ?: return
        throw Exception(
            "Account deletion completed with ${exceptions.size} error(s).",
            underlyingExceptions = exceptions,
            metadata = ExceptionMetadata(this),
        ).also { Logger.log(first) }
    }

    // MARK: - Auxiliary

    private suspend fun addToDeletedUsers(userID: String) {
        database.runTransaction(NetworkPath.deletedUsers.rawValue) { current ->
            val ids = (current as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
            ids.add(userID)
            ids.filter { it.isNotBlank() }.distinct()
        }
    }

    private suspend fun runStep(
        exceptions: MutableList<Exception>,
        step: suspend () -> Unit,
    ) {
        try {
            step()
        } catch (exception: Exception) {
            exceptions.add(exception)
            Logger.log(exception)
        }
    }
}
