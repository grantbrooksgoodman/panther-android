//
//  ReactionSessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.conversation.models.ReactionMetadata
import us.neotechnica.panther.modules.networking.conversation.remotelyupdatable.ConversationUpdatableKey
import us.neotechnica.panther.modules.networking.conversation.remotelyupdatable.update
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.extensions.isMock
import us.neotechnica.panther.modules.session.entity.extensions.isOutboxMessage
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

        val messageID = message.id
        val encodedReactionStyle = newReaction?.style?.encodedValue
        val reactionUserID = newReaction?.userID

        // Atomically read-modify-write the reactionMetadata node; didWrite
        // commits the hash and participant token fan-out and upserts to the
        // session store.
        conversation.update(ConversationUpdatableKey.REACTION_METADATA, applyingRaw = { currentValue ->
            @Suppress("UNCHECKED_CAST")
            val current = (currentValue as? List<Map<String, Any?>>) ?: emptyList()

            // Strip sentinel entries.
            var metadata = current.filter { (it[KEY_MESSAGE_ID] as? String) != BANG_QUALIFIED_EMPTY }

            // Remove the current user's reactions to this message.
            metadata =
                metadata.mapNotNull { entry ->
                    if ((entry[KEY_MESSAGE_ID] as? String) != messageID) return@mapNotNull entry
                    @Suppress("UNCHECKED_CAST")
                    val reactions =
                        ((entry[KEY_REACTIONS] as? List<Map<String, Any?>>) ?: emptyList())
                            .filter { (it[KEY_USER_ID] as? String) != currentUserID }
                    if (reactions.isEmpty()) null else entry + (KEY_REACTIONS to reactions)
                }

            // Add the new reaction, if provided.
            if (encodedReactionStyle != null && reactionUserID != null) {
                val reactionStyle = Reaction.Style.from(encodedReactionStyle) ?: Reaction.Style.LOVE
                val reaction = Reaction(style = reactionStyle, userID = reactionUserID)
                val index = metadata.indexOfFirst { (it[KEY_MESSAGE_ID] as? String) == messageID }
                metadata =
                    if (index >= 0) {
                        @Suppress("UNCHECKED_CAST")
                        val reactions =
                            ((metadata[index][KEY_REACTIONS] as? List<Map<String, Any?>>) ?: emptyList()) + reaction.encoded
                        metadata.toMutableList().also { it[index] = it[index] + (KEY_REACTIONS to reactions) }
                    } else {
                        metadata + ReactionMetadata(messageID = messageID, reactions = listOf(reaction)).encoded
                    }
            }

            // Return the empty sentinel if no reactions remain.
            if (metadata.isEmpty()) listOf(ReactionMetadata.empty.encoded) else metadata
        })

        // Refresh the on-screen messages so the new reaction renders. The
        // reaction is already committed to the conversation's reactionMetadata
        // by didWrite, so recomputing the displayed messages suffices.
        if (ConversationSessionService.currentConversation?.id?.key == conversation.id.key) {
            ConversationSessionService.updateDisplayedMessages()
        }
    }

    // MARK: - Companion

    private const val KEY_MESSAGE_ID = "messageID"
    private const val KEY_REACTIONS = "reactions"
    private const val KEY_USER_ID = "userID"
}
