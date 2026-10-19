//
//  ReactionSessionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.modules.common.extensions.toException
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.NotificationService
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.extensions.isOutboxMessage
import us.neotechnica.panther.modules.content.user.models.ContextMenuInteraction
import us.neotechnica.panther.modules.content.user.services.ChatPageStateService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.sources.conversation.models.ReactionMetadata
import us.neotechnica.panther.modules.networking.sources.conversation.remotelyupdatable.ConversationUpdatableKey
import us.neotechnica.panther.modules.networking.sources.conversation.remotelyupdatable.update
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.models.ReactionSessionServiceEffectID
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * The service that applies and removes message reactions.
 *
 * [isReactingToMessage] is written only through one internal path,
 * in the same turn that drains and runs the effects registered for
 * the new value. A registered effect therefore runs exactly once, for
 * the assignment it was registered against, and no other writer can
 * interleave between the write and the drain.
 */
object ReactionSessionService {
    // MARK: - Dependencies

    private val conversationSession: ConversationSessionService by Dependency { it.clientSession.entity.conversation }
    private val notificationService: NotificationService by Dependency { it.commonServices.notification }

    // MARK: - Properties

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val internalIsReactingToMessage = LockIsolated(false)

    private val uponIsReactingToMessageChangedToFalse =
        LockIsolated(mapOf<ReactionSessionServiceEffectID, () -> Unit>())
    private val uponIsReactingToMessageChangedToTrue =
        LockIsolated(mapOf<ReactionSessionServiceEffectID, () -> Unit>())

    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether a reaction is currently
     * being applied to a message.
     */
    val isReactingToMessage: Boolean
        get() = internalIsReactingToMessage.wrappedValue

    // MARK: - Add Effect

    /**
     * Registers an effect to run once, the next time
     * [isReactingToMessage] is set to the given value.
     *
     * The effect is cleared after it runs. Registering a new effect
     * with the same identifier and target value replaces the existing
     * one.
     *
     * @param state The value of [isReactingToMessage] that triggers
     *   the effect.
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
     * Applies the given reaction to the given message, or removes it
     * if the same reaction is already applied.
     *
     * Reactions to mock or outbox messages are ignored. The message's
     * sender is notified of the reaction.
     *
     * @param reaction The reaction to apply.
     * @param message The message to react to.
     *
     * @throws Exception if the required values cannot be resolved or
     *   the write fails.
     */
    suspend fun react(
        reaction: Reaction,
        message: Message,
    ) {
        if (message.isMock || message.isOutboxMessage) return

        val conversation = conversationSession.currentConversation
        val currentUserID = User.currentUserID
        val messageIndex = conversationSession.displayedMessages.value.indexOfFirst { it.id == message.id }

        if (conversation == null || currentUserID == null || messageIndex < 0) {
            throw Exception(
                "Failed to resolve required values.",
                metadata = ExceptionMetadata(this),
            )
        }

        val reactionMetadata = conversation.reactionMetadata ?: emptyList()

        // Remove reaction if same one is already applied

        val isAlreadyApplied =
            reactionMetadata
                .filter { it.messageID == message.id }
                .flatMap { it.reactions }
                .filter { it.userID == currentUserID }
                .any { it.style == reaction.style }

        if (isAlreadyApplied) return removeReaction(message)

        setIsReactingToMessage(true)

        // Notify users of reaction to message

        backgroundScope.launch {
            try {
                notifyUsers(
                    ofReaction = reaction,
                    to = message,
                )
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }

        // Update conversation with new reaction metadata

        updateConversation(
            conversation,
            message = message,
            newReaction = reaction,
        )
    }

    // MARK: - Auxiliary

    private suspend fun notifyUsers(
        ofReaction: Reaction,
        to: Message,
    ) {
        if (to.fromAccountID == User.currentUserID) return

        val conversation = conversationSession.currentConversation
        val currentUserID = User.currentUserID
        val user =
            conversation
                ?.users
                ?.filter { !(it.blockedUserIDs ?: emptyList()).contains(currentUserID) }
                ?.firstOrNull { to.fromAccountID == it.id }

        if (conversation == null || currentUserID == null || user == null || to.isMock || to.isOutboxMessage) {
            throw Exception(
                "Failed to resolve required values.",
                metadata = ExceptionMetadata(this),
            )
        }

        notificationService.notify(
            users = listOf(user),
            ofReaction = ofReaction,
            message = to,
            conversationIDKey = conversation.id.key,
        )
    }

    private suspend fun removeReaction(from: Message) {
        val conversation = conversationSession.currentConversation
        val messageIndex = conversationSession.displayedMessages.value.indexOfFirst { it.id == from.id }

        if (conversation == null || messageIndex < 0 || from.isMock || from.isOutboxMessage) {
            throw Exception(
                "Failed to resolve required values.",
                metadata = ExceptionMetadata(this),
            )
        }

        setIsReactingToMessage(true)
        updateConversation(
            conversation,
            message = from,
            newReaction = null,
        )
    }

    /**
     * Writes [isReactingToMessage] and, in the same turn, drains and
     * runs the effects registered for the new value.
     *
     * The registry is drained before the effects run, so an effect
     * that registers a new effect for the same value keeps it for the
     * next assignment.
     */
    private fun setIsReactingToMessage(isReactingToMessage: Boolean) {
        internalIsReactingToMessage.wrappedValue = isReactingToMessage

        // Deferred to the next turn on purpose: the context menu gate
        // flips after this turn's effects and the caller's continuation
        // have run, which matches the ordering callers were written for.
        mainScope.launch {
            ContextMenuInteraction.setCanBegin(!isReactingToMessage)
        }

        val effects =
            (if (isReactingToMessage) uponIsReactingToMessageChangedToTrue else uponIsReactingToMessageChangedToFalse)
                .withValue { reference ->
                    val drained = reference.value
                    reference.value = emptyMap()
                    drained
                }

        if (effects.isEmpty()) return

        Logger.log(
            Exception(
                "Running effects for change of \"isReactingToMessage\" to ${if (isReactingToMessage) "TRUE" else "FALSE"}.",
                isReportable = false,
                userInfo = mapOf("EnqueuedEffectIDs" to effects.keys.map { it.rawValue }),
                metadata = ExceptionMetadata(this),
            ),
        )

        effects.values.forEach { it() }
    }

    private suspend fun updateConversation(
        conversation: Conversation,
        message: Message,
        newReaction: Reaction?,
    ) {
        val currentUserID =
            User.currentUserID
                ?: throw Exception(
                    "Current user ID has not been set.",
                    metadata = ExceptionMetadata(this),
                )

        val encodedReactionStyle = newReaction?.style?.encodedValue
        val messageID = message.id
        val reactionUserID = newReaction?.userID

        // Atomically read-modify-write the reactionMetadata
        // node; didWrite commits the hash and participant
        // token fan-out and upserts to the session store.

        val updatedConversation =
            try {
                conversation.update(ConversationUpdatableKey.REACTION_METADATA) { currentValue ->
                    reactionMetadata(
                        currentValue = currentValue,
                        messageID = messageID,
                        currentUserID = currentUserID,
                        encodedReactionStyle = encodedReactionStyle,
                        reactionUserID = reactionUserID,
                    )
                }
            } catch (cancellation: CancellationException) {
                setIsReactingToMessage(false)
                throw cancellation
            } catch (throwable: Throwable) {
                setIsReactingToMessage(false)
                throw throwable.toException()
            }

        setIsReactingToMessage(false)
        updatedConversation.resolveMessages(ids = setOf(message.id))

        if (!ChatPageStateService.isPresented ||
            conversationSession.currentConversation?.id?.key != conversation.id.key
        ) {
            return
        }

        conversationSession.updateDisplayedMessages()
    }

    @Suppress("UNCHECKED_CAST")
    private fun reactionMetadata(
        currentValue: Any?,
        messageID: String,
        currentUserID: String,
        encodedReactionStyle: String?,
        reactionUserID: String?,
    ): Any {
        var metadata = (currentValue as? List<Map<String, Any?>>) ?: emptyList()

        // Strip sentinel entries.
        metadata = metadata.filter { (it[ReactionMetadata.SerializableKey.MESSAGE_ID.rawValue] as? String) != BANG_QUALIFIED_EMPTY }

        // Remove current user's reactions to this message.
        metadata =
            metadata.mapNotNull { entry ->
                if ((entry[ReactionMetadata.SerializableKey.MESSAGE_ID.rawValue] as? String) != messageID) return@mapNotNull entry

                val reactions =
                    ((entry[ReactionMetadata.SerializableKey.REACTIONS.rawValue] as? List<Map<String, Any?>>) ?: emptyList())
                        .filter { (it[Reaction.SerializableKey.USER_ID.rawValue] as? String) != currentUserID }

                if (reactions.isEmpty()) null else entry + (ReactionMetadata.SerializableKey.REACTIONS.rawValue to reactions)
            }

        // Add new reaction if provided.
        if (encodedReactionStyle != null && reactionUserID != null) {
            val reactionStyle = Reaction.Style.from(encodedReactionStyle) ?: Reaction.Style.LOVE
            val reaction = Reaction(reactionStyle, userID = reactionUserID)
            val index = metadata.indexOfFirst { (it[ReactionMetadata.SerializableKey.MESSAGE_ID.rawValue] as? String) == messageID }

            metadata =
                if (index >= 0) {
                    val reactions =
                        ((metadata[index][ReactionMetadata.SerializableKey.REACTIONS.rawValue] as? List<Map<String, Any?>>) ?: emptyList()) + reaction.encoded
                    metadata.toMutableList().also { it[index] = it[index] + (ReactionMetadata.SerializableKey.REACTIONS.rawValue to reactions) }
                } else {
                    metadata + ReactionMetadata(messageID = messageID, reactions = listOf(reaction)).encoded
                }
        }

        // Return empty sentinel if no reactions remain.
        return if (metadata.isEmpty()) listOf(ReactionMetadata.empty.encoded) else metadata
    }
}
