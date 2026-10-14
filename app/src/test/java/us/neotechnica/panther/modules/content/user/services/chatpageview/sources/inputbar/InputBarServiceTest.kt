//
//  InputBarServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.inputbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment

/**
 * Exercises the input bar service: the send and attach-media enable
 * rules, the text change path, and the sending state toggle, each
 * through injected collaborators.
 */
class InputBarServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    private var currentConversation: Conversation? = null
    private var isNewChatPagePresented = false
    private var isOnline = true
    private var isRecipientBarFirstResponder = false
    private var isSendingMessage = false

    private val service =
        InputBarService(
            InputBarService.Dependencies(
                currentConversation = { currentConversation },
                isNewChatPagePresented = { isNewChatPagePresented },
                isOnline = { isOnline },
                isRecipientBarFirstResponder = { isRecipientBarFirstResponder },
                isSendingMessage = { isSendingMessage },
            ),
        )

    @Before
    fun setUp() {
        environment.install("input-bar-service-test")
        currentConversation = SessionTestEnvironment.conversation("c1", listOf("me", "a"))
    }

    // MARK: - Send Button

    @Test
    fun `the send button enables only for non-blank text in a stored conversation`() {
        assertFalse(service.shouldEnableSendButton)

        service.textViewDidChange("hello")
        assertTrue(service.shouldEnableSendButton)
        assertTrue(service.isSendButtonEnabled.value)

        service.textViewDidChange("   ")
        assertFalse(service.shouldEnableSendButton)
        assertFalse(service.isSendButtonEnabled.value)
    }

    @Test
    fun `the send button disables while sending, while the recipient bar edits, and without a conversation`() {
        service.textViewDidChange("hello")

        isSendingMessage = true
        assertFalse(service.shouldEnableSendButton)
        isSendingMessage = false

        isRecipientBarFirstResponder = true
        assertFalse(service.shouldEnableSendButton)
        isRecipientBarFirstResponder = false

        currentConversation = null
        assertFalse(service.shouldEnableSendButton)
    }

    @Test
    fun `offline text sends stay enabled except from the new chat page`() {
        service.textViewDidChange("hello")
        isOnline = false

        assertTrue(service.shouldEnableSendButton)

        isNewChatPagePresented = true
        assertFalse(service.shouldEnableSendButton)
    }

    // MARK: - Attach Media Button

    @Test
    fun `the attach-media button requires connectivity and an idle stored conversation`() {
        assertTrue(service.shouldEnableAttachMediaButton)

        isOnline = false
        assertFalse(service.shouldEnableAttachMediaButton)
        isOnline = true

        isSendingMessage = true
        assertFalse(service.shouldEnableAttachMediaButton)
        isSendingMessage = false

        isRecipientBarFirstResponder = true
        assertFalse(service.shouldEnableAttachMediaButton)
    }

    // MARK: - Configure

    @Test
    fun `configuring publishes the enable rules and force updates republish`() {
        service.configureInputBar()
        assertTrue(service.isAttachMediaButtonEnabled.value)
        assertFalse(service.isSendButtonEnabled.value)

        service.setAttachMediaButtonIsEnabled(false)
        service.configureInputBar(forceUpdate = true)
        assertTrue(service.isAttachMediaButtonEnabled.value)
    }

    // MARK: - Sending UI

    @Test
    fun `showing the sending state clears the text and disables both buttons`() {
        service.textViewDidChange("hello")

        service.toggleSendingUI(on = true)

        assertEquals("", service.text.value)
        assertTrue(service.isShowingSendingUI.value)
        assertFalse(service.isAttachMediaButtonEnabled.value)
        assertFalse(service.isSendButtonEnabled.value)
    }

    @Test
    fun `hiding the sending state restores the attach-media button and keeps retained text`() {
        service.textViewDidChange("hello")
        service.toggleSendingUI(on = true, clearInputTextViewText = false)
        assertEquals("hello", service.text.value)

        service.toggleSendingUI(on = false)

        assertFalse(service.isShowingSendingUI.value)
        assertTrue(service.isAttachMediaButtonEnabled.value)
        assertTrue(service.isSendButtonEnabled.value)
    }

    // MARK: - First Responder

    @Test
    fun `the first responder status is recorded`() {
        service.setIsFirstResponder(true)
        assertTrue(service.isFirstResponder.value)

        service.setIsFirstResponder(false)
        assertFalse(service.isFirstResponder.value)
    }
}
