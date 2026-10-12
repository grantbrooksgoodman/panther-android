//
//  RecipientBarConfigService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.recipientbar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.content.user.extensions.empty
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.extensions.mock
import us.neotechnica.panther.modules.content.user.extensions.userIDs
import us.neotechnica.panther.modules.content.user.extensions.users
import us.neotechnica.panther.modules.content.user.services.AudioMessagePlaybackService
import us.neotechnica.panther.modules.networking.common.sortedByLatestMessageSentDate
import us.neotechnica.panther.modules.networking.common.visibleForCurrentUser
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService

/**
 * The service that reconfigures the recipient bar's message list as
 * recipients change.
 *
 * [RecipientBarConfigService] resolves the current conversation from
 * the selected contacts and publishes the key of the existing
 * conversation the message list should preview, if any.
 */
class RecipientBarConfigService {
    // MARK: - Properties

    private val mutableConversationIDKey = MutableStateFlow<String?>(null)

    // MARK: - Computed Properties

    /**
     * The key of the existing conversation the selected recipients
     * resolve to, or `null` when the selection is empty or matches no
     * existing conversation.
     */
    val conversationIDKey: StateFlow<String?> = mutableConversationIDKey.asStateFlow()

    // MARK: - Reconfigure Collection View

    /**
     * Resolves the current conversation from the selected contacts and
     * reloads the message list to match.
     *
     * When the selected contacts match an existing conversation, that
     * conversation becomes current; otherwise, an empty or provisional
     * conversation is set up in its place. Speech and audio playback
     * stop before the list changes.
     */
    fun reconfigureCollectionView() {
        TextToSpeechService.stop()
        AudioMessagePlaybackService.stopPlayback()

        val currentConversation = ConversationSessionService.currentConversation
        val previousConversationIDKey = currentConversation?.id?.key ?: ""

        val conversations =
            UserSessionService.currentUser
                ?.conversations
                ?.visibleForCurrentUser
                ?.filter { it.users != null }

        val selectedContactPairs = RecipientBarService.contactSelectionUI.selectedContactPairs.value
        val userIDs = selectedContactPairs.userIDs
        val users = selectedContactPairs.users

        val existingConversation =
            conversations
                ?.sortedByLatestMessageSentDate
                ?.firstOrNull { conversation ->
                    userIDs.sorted() == conversation.users?.map { it.id }?.sorted()
                }

        if (existingConversation == null) {
            val conversation =
                when {
                    selectedContactPairs.isEmpty() -> Conversation.empty
                    selectedContactPairs.all { it.isMock } -> Conversation.empty(withUsers = users)
                    else -> Conversation.mock(withUsers = users)
                }

            ConversationSessionService.setCurrentConversation(conversation)
            mutableConversationIDKey.value = null
            return
        }

        ConversationSessionService.setCurrentConversation(existingConversation)
        if (existingConversation.id.key != previousConversationIDKey) {
            mutableConversationIDKey.value = existingConversation.id.key
        }
    }

    // MARK: - Reset

    /** Clears the previewed conversation for the next composition. */
    fun reset() {
        mutableConversationIDKey.value = null
    }
}
