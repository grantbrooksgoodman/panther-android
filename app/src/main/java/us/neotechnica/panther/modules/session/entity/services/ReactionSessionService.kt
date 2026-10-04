//
//  ReactionSessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.modules.common.services.NotificationService
import us.neotechnica.panther.modules.content.user.models.ContextMenuInteraction
import us.neotechnica.panther.modules.content.user.services.ChatPageStateService
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
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.entity.models.ReactionSessionServiceEffectID
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Applies and removes message reactions.
 *
 * While a reaction is being applied, [isReactingToMessage] is
 * `true` and context menu interactions are disabled.
 */
object ReactionSessionService {
    // MARK: - Properties

    private val internalIsReactingToMessage = LockIsolated(false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val uponIsReactingToMessageChangedToFalse =
        LockIsolated(mapOf<ReactionSessionServiceEffectID, () -> Unit>())
    private val uponIsReactingToMessageChangedToTrue =
        LockIsolated(mapOf<ReactionSessionServiceEffectID, () -> Unit>())

    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether a reaction is
     * currently being applied to a message.
     */
    val isReactingToMessage: Boolean
        get() = internalIsReactingToMessage.wrappedValue

    // MARK: - Add Effect

    /**
     * Registers an effect to run once, the next time
     * [isReactingToMessage] is set to the given value.
     *
     * The effect is cleared after it runs. Registering a new
     * effect with the same identifier and target value replaces
     * the existing one.
     *
     * @param state The value of [isReactingToMessage] that
     *   triggers the effect.
     * @param id The identifier under which to register the effect.
     * @param effect The effect to run.
     */
    fun addEffectUponIsReactingToMessage(
        state: Boolean,
        id: ReactionSessionServiceEffectID,
        effect: () -> Unit,
    ) {
        val registry = if (state) uponIsReactingToMessageChangedToTrue else uponIsReactingToMessageChangedToFalse
        registry.withValue { it.value = it.value + (id to effect) }
    }

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

        setIsReactingToMessage(true)

        // Notify users of reaction to message
        scope.launch {
            try {
                notifyUsers(reaction, message)
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }

        updateConversation(conversation, message, reaction)
    }

    // MARK: - Notify Users

    private suspend fun notifyUsers(
        reaction: Reaction,
        message: Message,
    ) {
        if (message.fromAccountID == User.currentUserID) return

        val conversation = ConversationSessionService.currentConversation
        val currentUserID = User.currentUserID
        val user =
            conversation
                ?.users
                ?.filter { !(it.blockedUserIDs ?: emptyList()).contains(currentUserID) }
                ?.firstOrNull { message.fromAccountID == it.id }
        if (conversation == null || currentUserID == null || user == null || message.isMock || message.isOutboxMessage) {
            throw Exception("Failed to resolve required values.", metadata = ExceptionMetadata(this))
        }

        NotificationService.notify(
            users = listOf(user),
            ofReaction = reaction,
            message = message,
            conversationIDKey = conversation.id.key,
        )
    }

    // MARK: - Remove Reaction

    private suspend fun removeReaction(message: Message) {
        if (message.isMock || message.isOutboxMessage) return
        val conversation =
            ConversationSessionService.currentConversation
                ?: throw Exception("Failed to resolve required values.", metadata = ExceptionMetadata(this))
        setIsReactingToMessage(true)
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

        // Atomically read-modify-write the reactionMetadata node; didWrite
        // commits the hash and participant token fan-out and upserts to the
        // session store.
        try {
            conversation.update(ConversationUpdatableKey.REACTION_METADATA, applyingRaw = { currentValue ->
                reactionMetadata(currentValue, messageID, currentUserID, newReaction)
            })
        } catch (exception: Exception) {
            setIsReactingToMessage(false)
            throw exception
        }

        setIsReactingToMessage(false)

        // Refresh the on-screen messages so the new reaction renders. The
        // reaction is already committed to the conversation's reactionMetadata
        // by didWrite, so recomputing the displayed messages suffices.
        if (ChatPageStateService.isPresented &&
            ConversationSessionService.currentConversation?.id?.key == conversation.id.key
        ) {
            ConversationSessionService.updateDisplayedMessages()
        }
    }

    private fun reactionMetadata(
        currentValue: Any?,
        messageID: String,
        currentUserID: String,
        newReaction: Reaction?,
    ): Any {
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
        if (newReaction != null) {
            val reactionStyle = Reaction.Style.from(newReaction.style.encodedValue) ?: Reaction.Style.LOVE
            val reaction = Reaction(style = reactionStyle, userID = newReaction.userID)
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
        return if (metadata.isEmpty()) listOf(ReactionMetadata.empty.encoded) else metadata
    }

    private fun setIsReactingToMessage(isReactingToMessage: Boolean) {
        internalIsReactingToMessage.wrappedValue = isReactingToMessage
        didSetIsReactingToMessage(isReactingToMessage)
    }

    private fun didSetIsReactingToMessage(isReactingToMessage: Boolean) {
        ContextMenuInteraction.setCanBegin(!isReactingToMessage)
        val effects =
            drainEffects(
                if (isReactingToMessage) uponIsReactingToMessageChangedToTrue else uponIsReactingToMessageChangedToFalse,
            )
        if (effects.isEmpty()) return
        Logger.log(
            Exception(
                "Running effects for change of \"isReactingToMessage\" to " +
                    "${if (isReactingToMessage) "TRUE" else "FALSE"}. " +
                    "[EnqueuedEffectIDs: ${effects.keys.map { it.rawValue }}]",
                isReportable = false,
                metadata = ExceptionMetadata(this),
            ),
        )
        effects.values.forEach { it() }
    }

    private fun drainEffects(
        effects: LockIsolated<Map<ReactionSessionServiceEffectID, () -> Unit>>,
    ): Map<ReactionSessionServiceEffectID, () -> Unit> =
        effects.withValue { ref ->
            val drained = ref.value
            if (drained.isNotEmpty()) ref.value = emptyMap()
            drained
        }

    // MARK: - Companion

    private const val KEY_MESSAGE_ID = "messageID"
    private const val KEY_REACTIONS = "reactions"
    private const val KEY_USER_ID = "userID"
}
