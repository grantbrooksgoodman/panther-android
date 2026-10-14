//
//  ChatPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.user.dependencies.inputBarService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.AlternateMessageService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.parity.FixtureJson
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import java.util.Date

/**
 * Exercises the chat page reducer: the configuration carried into
 * state, the input bar forwarding, the alternate content toggles, the
 * send gate, and the reload triggers.
 */
class ChatPageReducerTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val reducer = ChatPageReducer()

    @Before
    fun setUp() {
        environment.install("chat-page-reducer-test")
        DependencyValues.current.inputBarService.textViewDidChange("")
    }

    @After
    fun tearDown() {
        DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
        ConversationSessionService.setCurrentConversation(null)
        AlternateMessageService.restoreAllAlternateTextMessageIDs()
        AlternateMessageService.restoreAllAudioTranscriptionMessageIDs()
        DependencyValues.current.inputBarService.toggleSendingUI(on = false)
        DependencyValues.current.inputBarService.textViewDidChange("")
    }

    // MARK: - Appearance

    @Test
    fun `appearing records the conversation and configuration`() {
        val configuration = ChatPageView.Configuration.Default(focusedMessageID = "m7")
        val result = reducer.reduce(ChatPageReducer.State(), ChatPageReducer.Action.ViewAppeared("c1", configuration))

        assertEquals("c1", result.state.conversationIDKey)
        assertEquals(configuration, result.state.configuration)
        assertEquals("m7", result.state.focusedMessageID)
    }

    @Test
    fun `the new chat and preview configurations carry no focused message`() {
        assertNull(ChatPageView.Configuration.NewChat.focusedMessageID)
        assertNull(ChatPageView.Configuration.Preview().focusedMessageID)
        assertEquals("m1", ChatPageView.Configuration.Preview("m1").focusedMessageID)
    }

    // MARK: - Messages

    @Test
    fun `updated messages load the view and rotate the change token`() {
        val state = ChatPageReducer.State()
        val message = SessionTestEnvironment.message("m1", "a", Date())

        val result = reducer.reduce(state, ChatPageReducer.Action.MessagesUpdated(listOf(message)))

        assertEquals(ViewState.Loaded, result.state.viewState)
        assertEquals(listOf(message), result.state.messages)
        assertNotEquals(state.changeToken, result.state.changeToken)
    }

    // MARK: - Input Bar

    @Test
    fun `text changes forward to the input bar service`() {
        reducer.reduce(ChatPageReducer.State(), ChatPageReducer.Action.TextViewDidChange("hola"))

        assertEquals("hola", DependencyValues.current.inputBarService.text.value)
    }

    @Test
    fun `pressing send with blank text or while sending leaves the state untouched`() {
        val state = ChatPageReducer.State()
        assertEquals(state, reducer.reduce(state, ChatPageReducer.Action.DidPressSendButton).state)

        reducer.reduce(state, ChatPageReducer.Action.TextViewDidChange("hi"))
        val sending = state.copy(isSendingMessage = true)
        assertEquals(sending, reducer.reduce(sending, ChatPageReducer.Action.DidPressSendButton).state)
        assertEquals("hi", DependencyValues.current.inputBarService.text.value)
    }

    @Test
    fun `pressing send with text shows the sending state and clears the field`() {
        reducer.reduce(ChatPageReducer.State(), ChatPageReducer.Action.TextViewDidChange("hi"))

        reducer.reduce(ChatPageReducer.State(), ChatPageReducer.Action.DidPressSendButton)

        assertTrue(DependencyValues.current.inputBarService.isShowingSendingUI.value)
        assertEquals("", DependencyValues.current.inputBarService.text.value)
    }

    @Test
    fun `the sending flag mirrors the delivery service and toggles the sending state`() {
        val state = ChatPageReducer.State()

        val sending = reducer.reduce(state, ChatPageReducer.Action.IsSendingMessageChanged(true)).state
        assertTrue(sending.isSendingMessage)
        assertTrue(DependencyValues.current.inputBarService.isShowingSendingUI.value)

        val idle = reducer.reduce(sending, ChatPageReducer.Action.IsSendingMessageChanged(false)).state
        assertFalse(idle.isSendingMessage)
        assertFalse(DependencyValues.current.inputBarService.isShowingSendingUI.value)
    }

    // MARK: - Alternate Content

    @Test
    fun `view-alternate and audio-message actions toggle the alternate content`() {
        val message = SessionTestEnvironment.message("m1", "a", Date())
        val state = ChatPageReducer.State()

        val toggled = reducer.reduce(state, ChatPageReducer.Action.HandleViewAlternateAction(message)).state
        assertTrue(AlternateMessageService.isDisplayingAlternateText(message))
        assertNotEquals(state.changeToken, toggled.changeToken)

        reducer.reduce(toggled, ChatPageReducer.Action.HandleViewAlternateAction(message))
        assertFalse(AlternateMessageService.isDisplayingAlternateText(message))

        reducer.reduce(state, ChatPageReducer.Action.HandleAudioMessageAction(message))
        assertTrue(AlternateMessageService.isDisplayingAudioTranscription(message))
    }

    // MARK: - Reloads

    @Test
    fun `an outbox change records the sending entry flag`() {
        val result = reducer.reduce(ChatPageReducer.State(), ChatPageReducer.Action.HandleOutboxChange(true))
        assertTrue(result.state.isSendingOutboxEntry)
    }

    @Test
    fun `a user store change never reloads and a reload request rotates the token`() {
        val state = ChatPageReducer.State()
        val change = SessionStoreChange.Users(upsertedIDs = setOf("a"), removedIDs = emptySet())

        assertEquals(state, reducer.reduce(state, ChatPageReducer.Action.HandleSessionStoreChange(change)).state)
        assertNotEquals(state.changeToken, reducer.reduce(state, ChatPageReducer.Action.ReloadRequested).state.changeToken)
    }

    // MARK: - Disappearance

    @Test
    fun `disappearing keeps the current conversation while another chat page presents it`() {
        val conversation = runBlocking { Conversation.decode(FixtureJson.loadObject("conversation.json")) }
        ConversationSessionService.setCurrentConversation(conversation)
        DependencyValues.current.navigation.navigate(
            Route.UserContent(UserContentRoute.Stack(listOf(UserContentNavigatorState.SeguePaths.Chat(conversation.id.key)))),
        )

        val state =
            ChatPageReducer.State(
                conversationIDKey = conversation.id.key,
                configuration = ChatPageView.Configuration.NewChat,
            )

        reducer.reduce(state, ChatPageReducer.Action.ViewDisappeared)
        assertEquals(conversation.id.key, ConversationSessionService.currentConversation?.id?.key)
    }

    @Test
    fun `disappearing clears the current conversation once the conversation leaves the screen`() {
        val conversation = runBlocking { Conversation.decode(FixtureJson.loadObject("conversation.json")) }
        ConversationSessionService.setCurrentConversation(conversation)
        DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))

        reducer.reduce(ChatPageReducer.State(conversationIDKey = conversation.id.key), ChatPageReducer.Action.ViewDisappeared)
        assertNull(ConversationSessionService.currentConversation)
    }
}
