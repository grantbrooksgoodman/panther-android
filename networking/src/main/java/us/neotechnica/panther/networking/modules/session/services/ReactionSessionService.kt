//
//  ReactionSessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.conversation.models.Reaction
import us.neotechnica.panther.networking.modules.schema.conversation.models.ReactionMetadata
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.commitFieldUpdates
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.extensions.isMock
import us.neotechnica.panther.networking.modules.session.extensions.isOutboxMessage
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * Applies and removes message reactions.
 *
 * **Note:** the iOS original tracks an `isReactingToMessage` flag with a
 * registry of effects (`addEffectUponIsReactingToMessage`) that gate the
 * UIKit context menu's re-entrancy; the Compose context menu dismisses on
 * selection instead, so this port omits that mechanism. Notifying the
 * message's sender of a reaction arrives with notifications (Phase R6).
 */
object ReactionSessionService {
    // MARK: - React to Message

    /**
     * Applies [reaction] to [message], or removes it when the same
     * reaction is already applied by the current user.
     *
     * Reactions to mock or outbox messages are ignored.
     *
     * @throws Exception if the required values cannot be resolved or the
     *   write fails.
     */
    suspend fun react(
        reaction: Reaction,
        message: Message,
    ) {
        if (message.isMock || message.isOutboxMessage) return
        val conversation =
            ConversationSessionService.currentConversation
                ?: throw Exception("Failed to resolve required values.", metadata = ExceptionMetadata(this))
        val currentUserID =
            User.currentUserID
                ?: throw Exception("Failed to resolve required values.", metadata = ExceptionMetadata(this))

        val alreadyApplied =
            (conversation.reactionMetadata ?: emptyList())
                .filter { it.messageID == message.id }
                .flatMap { it.reactions }
                .filter { it.userID == currentUserID }
                .any { it.style == reaction.style }
        if (alreadyApplied) return removeReaction(message)

        updateConversation(conversation, message, reaction)
    }

    // MARK: - Remove Reaction

    private suspend fun removeReaction(message: Message) {
        if (message.isMock || message.isOutboxMessage) return
        val conversation =
            ConversationSessionService.currentConversation
                ?: throw Exception("Failed to resolve required values.", metadata = ExceptionMetadata(this))
        updateConversation(conversation, message, null)
    }

    // MARK: - Auxiliary

    private suspend fun updateConversation(
        conversation: Conversation,
        message: Message,
        newReaction: Reaction?,
    ) {
        val currentUserID =
            User.currentUserID
                ?: throw Exception("Current user ID has not been set.", metadata = ExceptionMetadata(this))

        // Strip sentinel entries, then remove the current user's reactions
        // to this message, dropping entries that become empty.
        var metadata =
            (conversation.reactionMetadata ?: emptyList())
                .filter { it.messageID != BANG_QUALIFIED_EMPTY }
                .mapNotNull { entry ->
                    if (entry.messageID != message.id) return@mapNotNull entry
                    val reactions = entry.reactions.filter { it.userID != currentUserID }
                    if (reactions.isEmpty()) null else entry.copy(reactions = reactions)
                }

        // Add the new reaction, if provided.
        if (newReaction != null) {
            val index = metadata.indexOfFirst { it.messageID == message.id }
            metadata =
                if (index >= 0) {
                    metadata.toMutableList().also { it[index] = it[index].copy(reactions = it[index].reactions + newReaction) }
                } else {
                    metadata + ReactionMetadata(messageID = message.id, reactions = listOf(newReaction))
                }
        }

        val updated = conversation.copy(reactionMetadata = metadata.ifEmpty { listOf(ReactionMetadata.empty) })
        conversation.commitFieldUpdates(updated, setOf(REACTION_METADATA_KEY))
    }

    // MARK: - Companion

    private const val REACTION_METADATA_KEY = "reactionMetadata"
}
