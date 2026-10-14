//
//  InputBarActionHandlerService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.bundle.bugPrevention
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.extensions.isEmpty
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * The service that handles the input bar's button actions.
 *
 * Use [InputBarActionHandlerService] to respond to taps on the input
 * bar's attach-media and send buttons.
 */
class InputBarActionHandlerService {
    // MARK: - Dependencies

    private val conversationSession: ConversationSessionService by Dependency { it.clientSession.entity.conversation }
    private val textToSpeechService: TextToSpeechService by Dependency { it.commonServices.audio.textToSpeech }

    // MARK: - Did Press Attach Media Button

    /** Handles a tap on the attach-media button by presenting the attach-media action sheet. */
    fun didPressAttachMediaButton() {
        MediaActionHandlerService.attachMediaButtonTapped()
    }

    // MARK: - Did Press Send Button

    /**
     * Sends the given text as a message in the current conversation.
     *
     * This method has no effect when the current conversation is
     * empty.
     *
     * @param text The text to send.
     *
     * @throws Exception if delivery fails.
     */
    suspend fun didPressSendButton(text: String) {
        // Guards against rapid typing enabling the send button for an
        // empty conversation.
        val currentConversation = conversationSession.currentConversation
        if (currentConversation != null && currentConversation.isEmpty) {
            Logger.log("Intercepted invalid send button press bug.", domain = LoggerDomain.bugPrevention)
            return
        }

        textToSpeechService.stopSpeakingIfNeeded()
        MessageDeliveryService.sendTextMessage(text)
    }
}
