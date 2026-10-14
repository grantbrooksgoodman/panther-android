//
//  ConversationCellReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.conversationcellview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.services.sources.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewStrings
import us.neotechnica.panther.modules.content.user.extensions.empty
import us.neotechnica.panther.modules.content.user.extensions.isRelevant
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewDataCache
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.conversation.models.Participant
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.modules.session.state.services.SessionStore

/**
 * Exercises the conversation cell reducer: the relevance of session
 * store changes, the retry when data cannot resolve, and redaction.
 */
class ConversationCellReducerTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val reducer = ConversationCellReducer()

    private val conversation =
        Conversation.empty.copy(
            id = Conversation.empty.id.copy(key = "c1"),
            messageIDs = listOf("m1", "m2"),
            participants = listOf(Participant("u1"), Participant("u2")),
        )

    @Before
    fun setUp() {
        environment.install("conversation-cell-reducer-test")
        environment.signIn(SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001"))
        CommonPropertyLists.initializeForTesting(callingCodes = mapOf("US" to "1"), lookupTables = mapOf("10" to listOf("1")))
        RegionDetailService.clearCache()
        ConversationCellViewDataCache.clearCache()
    }

    @After
    fun tearDown() {
        ConversationCellViewDataCache.clearCache()
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
    }

    // MARK: - Relevance

    @Test
    fun `an upsert of the conversation is relevant`() {
        assertTrue(SessionStoreChange.Conversations(setOf("c1"), emptySet()).isRelevant(conversation))
    }

    @Test
    fun `a removal of the conversation is relevant`() {
        assertTrue(SessionStoreChange.Conversations(emptySet(), setOf("c1")).isRelevant(conversation))
    }

    @Test
    fun `an unrelated conversation change is not relevant`() {
        assertFalse(SessionStoreChange.Conversations(setOf("other"), emptySet()).isRelevant(conversation))
    }

    @Test
    fun `a change to one of the conversation's messages is relevant`() {
        assertTrue(SessionStoreChange.Messages(setOf("m2"), emptySet()).isRelevant(conversation))
    }

    @Test
    fun `an unrelated message change is not relevant`() {
        assertFalse(SessionStoreChange.Messages(setOf("mX"), emptySet()).isRelevant(conversation))
    }

    @Test
    fun `a change to one of the conversation's participants is relevant`() {
        assertTrue(SessionStoreChange.Users(setOf("u2"), emptySet()).isRelevant(conversation))
    }

    @Test
    fun `an unrelated participant change is not relevant`() {
        assertFalse(SessionStoreChange.Users(setOf("uX"), emptySet()).isRelevant(conversation))
    }

    // MARK: - Loading

    @Test
    fun `unresolvable data leaves the placeholder in place and retries`() {
        SessionStore.upsertConversation(SessionTestEnvironment.conversation("c1", listOf("me", "missing")))
        val state = ConversationCellReducer.State(conversationIDKey = "c1", searchQuery = "")

        val result = reducer.reduce(state, ConversationCellReducer.Action.ViewAppeared)

        assertEquals(ConversationCellViewData.empty, result.state.cellViewData)
    }

    @Test
    fun `resolvable data populates the cell`() {
        SessionStore.upsertUsers(setOf(SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002")))
        SessionStore.upsertConversation(SessionTestEnvironment.conversation("c1", listOf("me", "a")))
        val state = ConversationCellReducer.State(conversationIDKey = "c1", searchQuery = "")

        val result = reducer.reduce(state, ConversationCellReducer.Action.ViewAppeared)

        val titleLabelText = result.state.cellViewData.titleLabelText
        assertTrue(titleLabelText.isNotBlank())
        assertFalse(result.state.isShowingRedactedContent)
    }

    // MARK: - Redaction

    @Test
    fun `a reloading conversation redacts with placeholder text`() {
        val state = ConversationCellReducer.State(conversationIDKey = "c1", searchQuery = "")

        val reloading = reducer.reduce(state, ConversationCellReducer.Action.ReloadingConversationsChanged(setOf("c1"))).state

        assertTrue(reloading.isShowingRedactedContent)
        assertEquals(ConversationCellViewStrings.REDACTED_DATE_LABEL_TEXT, reloading.dateLabelText)
        assertEquals(ConversationCellViewStrings.REDACTED_SUBTITLE_LABEL_TEXT, reloading.subtitleLabelText)

        val settled = reducer.reduce(reloading, ConversationCellReducer.Action.ReloadingConversationsChanged(emptySet())).state
        assertFalse(settled.isShowingRedactedContent)
        assertEquals("", settled.dateLabelText)
    }
}
