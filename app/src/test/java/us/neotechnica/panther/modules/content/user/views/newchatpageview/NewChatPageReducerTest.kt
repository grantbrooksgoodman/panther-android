//
//  NewChatPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.newchatpageview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.modules.content.user.dependencies.inputBarService
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.RecipientBarService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Send
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.io.File

/**
 * Exercises the new chat page's input-bar gating: a send into an
 * empty conversation is refused, a send into a stored conversation
 * shows the sending state, a blank conversation key navigates
 * nowhere, and the input field taking focus commits a pending
 * recipient entry.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewChatPageReducerTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val reducer = NewChatPageReducer()

    private val inputBarService
        get() = DependencyValues.current.inputBarService

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("new-chat-page-reducer-test")

        LocalizedStringResolver.initializeForTesting { source ->
            val root = if (source == LocalizationSource.APP) "src/main/assets" else "../subsystem/src/main/assets"
            val json = Json.parseToJsonElement(File(root, source.assetName).readText()).jsonObject
            json.mapValues { (_, languages) ->
                languages.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            }
        }

        CoreUtilities.setLanguageCode("en")
        RecipientBarService.reset()

        val me = SessionTestEnvironment.user(id = "me", languageCode = "en")
        environment.signIn(me)
        SessionStore.upsertUser(SessionTestEnvironment.user(id = "a", nationalNumber = "5559876543"))
        inputBarService.textViewDidChange("")
    }

    @After
    fun tearDown() {
        // A blank query reloads the suggestions table on the Main
        // scope; join that resolution before the Main dispatcher goes
        // away.
        runBlocking { RecipientBarService.tableView.resolveContactPairs() }
        DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
        ConversationSessionService.setCurrentConversation(null)
        inputBarService.toggleSendingUI(on = false)
        inputBarService.textViewDidChange("")
        RecipientBarService.reset()
        LocalizedStringResolver.clearCache()
        Dispatchers.resetMain()
    }

    // MARK: - Sending

    @Test
    fun `a send into an empty conversation leaves the state untouched`() {
        ConversationSessionService.setCurrentConversation(Conversation.empty)
        val state = NewChatPageReducer.State(inputText = "hi")
        reducer.reduce(state, NewChatPageReducer.Action.InputChanged("hi"))

        val result = reducer.reduce(state, NewChatPageReducer.Action.SendTapped)

        assertEquals(state, result.state)
        assertFalse(inputBarService.isSendButtonEnabled.value)
        assertFalse(inputBarService.isShowingSendingUI.value)
    }

    @Test
    fun `a send into a stored conversation shows the sending state and clears the text`() {
        ConversationSessionService.setCurrentConversation(SessionTestEnvironment.conversation("-C", listOf("me", "a")))
        val state = NewChatPageReducer.State(inputText = "hi")
        reducer.reduce(state, NewChatPageReducer.Action.InputChanged("hi"))
        assertTrue(inputBarService.isSendButtonEnabled.value)

        val result = reducer.reduce(state, NewChatPageReducer.Action.SendTapped)

        assertEquals("", result.state.inputText)
        assertTrue(inputBarService.isShowingSendingUI.value)
    }

    @Test
    fun `a blank conversation key after the send navigates nowhere`() {
        val navigation = DependencyValues.current.navigation
        val before = navigation.state.value

        val result = reducer.reduce(NewChatPageReducer.State(), NewChatPageReducer.Action.SendReturned(""))

        assertFalse(result.state.hasSentFirstMessage)
        assertEquals(before, navigation.state.value)
    }

    // MARK: - Focus

    @Test
    fun `the input field taking focus commits typed recipient text as a mock recipient`() =
        runBlocking {
            RecipientBarService.layout.setTextFieldText("Bob")

            val result = reducer.reduce(NewChatPageReducer.State(), NewChatPageReducer.Action.InputBarFocusChanged(true))
            result.effect.operation(Send {})

            val selected = RecipientBarService.contactSelectionUI.selectedContactPairs.value
            assertEquals(listOf(true), selected.map { it.isMock })
            assertTrue(inputBarService.isFirstResponder.value)
        }

    @Test
    fun `the input field taking focus with nothing pending selects no recipient`() =
        runBlocking {
            val result = reducer.reduce(NewChatPageReducer.State(), NewChatPageReducer.Action.InputBarFocusChanged(true))
            result.effect.operation(Send {})

            assertTrue(
                RecipientBarService.contactSelectionUI.selectedContactPairs.value
                    .isEmpty(),
            )
        }
}
