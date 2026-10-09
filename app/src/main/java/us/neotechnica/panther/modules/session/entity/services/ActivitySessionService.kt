//
//  ActivitySessionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.conversation.models.MessageRecipientConsentAcknowledgementData
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.conversation.models.PenPalsSharingData
import us.neotechnica.panther.modules.networking.conversation.remotelyupdatable.ConversationUpdatableKey
import us.neotechnica.panther.modules.networking.conversation.remotelyupdatable.updateValues
import us.neotechnica.panther.modules.networking.conversation.services.ConversationService
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * The service that adds and removes conversation participants,
 * recording each change as an activity.
 */
object ActivitySessionService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Add User to Conversation

    /**
     * Adds the user with the given identifier to the given
     * conversation, recording the change as an activity.
     *
     * @param userID The identifier of the user to add.
     * @param conversation The conversation to add the user to.
     *
     * @throws Exception if the activity cannot be synthesized or the
     *   write fails.
     */
    suspend fun addToConversation(
        userID: String,
        conversation: Conversation,
    ) {
        val activity =
            Activity.from(Activity.Action.AddedToConversation(userID))
                ?: throw Exception(
                    "Failed to synthesize activity.",
                    metadata = ExceptionMetadata(this),
                )

        val newMessageRecipientConsentAcknowledgementData =
            conversation.metadata.messageRecipientConsentAcknowledgementData +
                MessageRecipientConsentAcknowledgementData(
                    userID = userID,
                    consentAcknowledged = conversation.metadata.requiresConsentFromInitiator == null,
                )

        val newPenPalsSharingData =
            conversation.metadata.penPalsSharingData + PenPalsSharingData(userID = userID)

        val newMetadata =
            conversation.metadata.copyWith(
                messageRecipientConsentAcknowledgementData = newMessageRecipientConsentAcknowledgementData,
                penPalsSharingData = newPenPalsSharingData,
            )

        val newActivities = ((conversation.activities ?: emptyList()) + activity).filter { it != Activity.empty }
        val newParticipants = conversation.participants + Participant(userID = userID)

        val updatedConversation =
            conversation.updateValues(
                mapOf(
                    ConversationUpdatableKey.ACTIVITIES to newActivities,
                    ConversationUpdatableKey.METADATA to newMetadata,
                    ConversationUpdatableKey.PARTICIPANTS to newParticipants,
                ),
            )

        addUserToConversation(
            userID = userID,
            conversationID = updatedConversation.id,
        )
    }

    private suspend fun addUserToConversation(
        userID: String,
        conversationID: ConversationID,
    ) {
        val path =
            listOf(
                NetworkPath.users.rawValue,
                userID,
                User.SerializableKey.CONVERSATION_IDS.rawValue,
                conversationID.key,
            ).joinToString("/")

        database.commit(mapOf(path to conversationID.hash))
    }

    // MARK: - Remove User from Conversation

    /**
     * Removes the user with the given identifier from the given
     * conversation, recording the change as an activity.
     *
     * @param userID The identifier of the user to remove.
     * @param conversation The conversation to remove the user from.
     * @param removeFromUser A Boolean value that determines whether to
     *   also remove the conversation from the user's record.
     *
     * @throws Exception if the activity cannot be synthesized or the
     *   write fails.
     */
    suspend fun removeFromConversation(
        userID: String,
        conversation: Conversation,
        removeFromUser: Boolean = true,
    ) {
        val action =
            if (userID == User.currentUserID) {
                Activity.Action.LeftConversation
            } else {
                Activity.Action.RemovedFromConversation(userID)
            }

        val activity =
            Activity.from(action)
                ?: throw Exception(
                    "Failed to synthesize activity.",
                    metadata = ExceptionMetadata(this),
                )

        val newActivities = ((conversation.activities ?: emptyList()) + activity).filter { it != Activity.empty }
        val newParticipants = conversation.participants.filter { it.userID != userID }
        val newMetadata =
            conversation.metadata.copyWith(
                // A group shrinking to two participants clears its name (D-IV-6).
                name = if (newParticipants.size == ONE_TO_ONE_PARTICIPANT_COUNT) BANG_QUALIFIED_EMPTY else null,
                messageRecipientConsentAcknowledgementData =
                    conversation.metadata.messageRecipientConsentAcknowledgementData.filter { it.userID != userID },
                penPalsSharingData = conversation.metadata.penPalsSharingData.filter { it.userID != userID },
                nilImageData = newParticipants.size == ONE_TO_ONE_PARTICIPANT_COUNT,
                nilRequiresConsentFromInitiator = conversation.metadata.requiresConsentFromInitiator == userID,
            )

        val updatedConversation =
            conversation.updateValues(
                mapOf(
                    ConversationUpdatableKey.ACTIVITIES to newActivities,
                    ConversationUpdatableKey.METADATA to newMetadata,
                    ConversationUpdatableKey.PARTICIPANTS to newParticipants,
                ),
            )

        if (removeFromUser) {
            ConversationService.removeConversationFromUsers(
                userIDs = listOf(userID),
                conversationIDKey = updatedConversation.id.key,
            )
        }
    }

    // MARK: - Update Metadata

    /**
     * Applies the given metadata to the given conversation, recording
     * the given action as an activity.
     *
     * @param conversation The conversation to update.
     * @param action The action describing the metadata change.
     * @param newMetadata The metadata to apply.
     *
     * @return The updated conversation.
     *
     * @throws Exception if the activity cannot be synthesized or the
     *   write fails.
     */
    suspend fun updateMetadata(
        conversation: Conversation,
        action: Activity.Action,
        newMetadata: ConversationMetadata,
    ): Conversation {
        val activity =
            Activity.from(action)
                ?: throw Exception(
                    "Failed to synthesize activity.",
                    metadata = ExceptionMetadata(this),
                )

        val newActivities = ((conversation.activities ?: emptyList()) + activity).filter { it != Activity.empty }
        return conversation.updateValues(
            mapOf(
                ConversationUpdatableKey.ACTIVITIES to newActivities,
                ConversationUpdatableKey.METADATA to newMetadata,
            ),
        )
    }

    // MARK: - Companion

    private const val ONE_TO_ONE_PARTICIPANT_COUNT = 2
}
