//
//  ConversationCellReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.conversationcellview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.session.entity.extensions.empty
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange

class ConversationCellReducerTest {
    // MARK: - Setup

    private val reducer = ConversationCellReducer()

    private val conversation =
        Conversation.empty.copy(
            id = Conversation.empty.id.copy(key = "c1"),
            messageIDs = listOf("m1", "m2"),
            participants = listOf(Participant("u1"), Participant("u2")),
        )

    // MARK: - Tests

    @Test
    fun `an upsert of the conversation is relevant`() {
        assertTrue(reducer.isRelevantChange(SessionStoreChange.Conversations(setOf("c1"), emptySet()), conversation))
    }

    @Test
    fun `a removal of the conversation is relevant`() {
        assertTrue(reducer.isRelevantChange(SessionStoreChange.Conversations(emptySet(), setOf("c1")), conversation))
    }

    @Test
    fun `an unrelated conversation change is not relevant`() {
        assertFalse(reducer.isRelevantChange(SessionStoreChange.Conversations(setOf("other"), emptySet()), conversation))
    }

    @Test
    fun `a change to one of the conversation's messages is relevant`() {
        assertTrue(reducer.isRelevantChange(SessionStoreChange.Messages(setOf("m2"), emptySet()), conversation))
    }

    @Test
    fun `an unrelated message change is not relevant`() {
        assertFalse(reducer.isRelevantChange(SessionStoreChange.Messages(setOf("mX"), emptySet()), conversation))
    }

    @Test
    fun `a change to one of the conversation's participants is relevant`() {
        assertTrue(reducer.isRelevantChange(SessionStoreChange.Users(setOf("u2"), emptySet()), conversation))
    }

    @Test
    fun `an unrelated participant change is not relevant`() {
        assertFalse(reducer.isRelevantChange(SessionStoreChange.Users(setOf("uX"), emptySet()), conversation))
    }
}
