//
//  ActivitySessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.schema.conversation.models.Activity
import us.neotechnica.panther.networking.modules.schema.conversation.models.ActivityAction
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.conversation.models.ConversationMetadata
import us.neotechnica.panther.networking.modules.schema.conversation.models.MessageRecipientConsentAcknowledgementData
import us.neotechnica.panther.networking.modules.schema.conversation.models.Participant
import us.neotechnica.panther.networking.modules.schema.conversation.models.PenPalsSharingData
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.commitFieldUpdates
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import java.util.Date

/**
 * Adds and removes conversation participants and renames conversations,
 * recording each change as an activity.
 */
object ActivitySessionService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Add User

    /**
     * Adds [userID] to [conversation], recording an activity and
     * seeding the user's consent and PenPals records.
     *
     * @return The updated conversation.
     *
     * @throws Exception if the current user is unset or the write fails.
     */
    suspend fun addToConversation(
        userID: String,
        conversation: Conversation,
    ): Conversation {
        val activity = activity(ActivityAction.AddedToConversation(userID))

        val newConsentData =
            conversation.metadata.messageRecipientConsentAcknowledgementData +
                MessageRecipientConsentAcknowledgementData(
                    userID = userID,
                    consentAcknowledged = conversation.metadata.requiresConsentFromInitiator == null,
                )
        val newPenPalsData =
            conversation.metadata.penPalsSharingData + PenPalsSharingData(userID = userID, sharesDataWithUserIDs = null)

        val updated =
            conversation.copy(
                activities = ((conversation.activities ?: emptyList()) + activity).filter { it != Activity.empty },
                metadata =
                    conversation.metadata.copyWith(
                        messageRecipientConsentAcknowledgementData = newConsentData,
                        penPalsSharingData = newPenPalsData,
                    ),
                participants = conversation.participants + Participant(userID = userID),
            )

        val committed = conversation.commitFieldUpdates(updated, CHANGED_KEYS)
        database.commit(
            mapOf("$USERS_PATH/$userID/$OPEN_CONVERSATIONS_KEY/${committed.id.key}" to committed.id.hash),
        )
        return committed
    }

    // MARK: - Remove User

    /**
     * Removes [userID] from [conversation], recording an activity and
     * pruning the user's consent and PenPals records.
     *
     * @param removeFromUser Whether to also clear the user's own token.
     *
     * @return The updated conversation.
     *
     * @throws Exception if the current user is unset or the write fails.
     */
    suspend fun removeFromConversation(
        userID: String,
        conversation: Conversation,
        removeFromUser: Boolean = true,
    ): Conversation {
        val action =
            if (userID == User.currentUserID) {
                ActivityAction.LeftConversation
            } else {
                ActivityAction.RemovedFromConversation(userID)
            }
        val activity = activity(action)

        val newParticipants = conversation.participants.filter { it.userID != userID }
        val updated =
            conversation.copy(
                activities = ((conversation.activities ?: emptyList()) + activity).filter { it != Activity.empty },
                metadata =
                    conversation.metadata.copyWith(
                        name = if (newParticipants.size == 2) BANG_QUALIFIED_EMPTY else conversation.metadata.name,
                        imageData = if (newParticipants.size == 2) null else conversation.metadata.imageData,
                        messageRecipientConsentAcknowledgementData =
                            conversation.metadata.messageRecipientConsentAcknowledgementData.filter { it.userID != userID },
                        penPalsSharingData = conversation.metadata.penPalsSharingData.filter { it.userID != userID },
                        requiresConsentFromInitiator =
                            if (conversation.metadata.requiresConsentFromInitiator == userID) {
                                null
                            } else {
                                conversation.metadata.requiresConsentFromInitiator
                            },
                    ),
                participants = newParticipants,
            )

        val committed = conversation.commitFieldUpdates(updated, CHANGED_KEYS)
        if (removeFromUser) {
            database.commit(mapOf("$USERS_PATH/$userID/$OPEN_CONVERSATIONS_KEY/${committed.id.key}" to null))
        }
        return committed
    }

    // MARK: - Update Metadata

    /**
     * Applies [newMetadata] to [conversation], recording [action] as an
     * activity.
     *
     * @return The updated conversation.
     *
     * @throws Exception if the current user is unset or the write fails.
     */
    suspend fun updateMetadata(
        conversation: Conversation,
        action: ActivityAction,
        newMetadata: ConversationMetadata,
    ): Conversation {
        val activity = activity(action)
        val updated =
            conversation.copy(
                activities = ((conversation.activities ?: emptyList()) + activity).filter { it != Activity.empty },
                metadata = newMetadata,
            )
        return conversation.commitFieldUpdates(updated, CHANGED_KEYS)
    }

    // MARK: - Auxiliary

    private fun activity(action: ActivityAction): Activity {
        val currentUserID =
            User.currentUserID
                ?: throw Exception("Current user ID has not been set.", metadata = ExceptionMetadata(this))
        return Activity(action = action, date = Date(), userID = currentUserID)
    }

    // MARK: - Companion

    private const val USERS_PATH = "users"
    private const val OPEN_CONVERSATIONS_KEY = "openConversations"
    private const val BANG_QUALIFIED_EMPTY = "!"
    private val CHANGED_KEYS = setOf("activities", "metadata", "participants")
}
