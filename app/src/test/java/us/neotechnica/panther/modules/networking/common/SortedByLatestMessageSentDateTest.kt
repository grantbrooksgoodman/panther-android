//
//  SortedByLatestMessageSentDateTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.state.services.SessionStore
import java.util.Date

/**
 * Exercises the latest-message ordering: activity dates count, ties
 * order by key, and conversations without a date sort last.
 */
class SortedByLatestMessageSentDateTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("sorted-by-latest-message-test")
        environment.signIn(SessionTestEnvironment.user(id = "me"))
        SessionStore.upsertUser(SessionTestEnvironment.user(id = "them", nationalNumber = "5559876543"))
    }

    // MARK: - Tests

    @Test
    fun `conversations order by latest message or activity date, ties by key, undated last`() {
        SessionStore.upsertMessages(
            setOf(
                SessionTestEnvironment.message("-A1", fromAccountID = "them", sentDate = Date(3_000)),
                SessionTestEnvironment.message("-B1", fromAccountID = "them", sentDate = Date(1_000)),
                SessionTestEnvironment.message("-D1", fromAccountID = "them", sentDate = Date(3_000)),
            ),
        )

        val participants = listOf("me", "them")
        val oldest = SessionTestEnvironment.conversation("-A", participants, messageIDs = listOf("-A1"))
        val renamedLater =
            SessionTestEnvironment
                .conversation("-B", participants, messageIDs = listOf("-B1"))
                .copy(activities = listOf(Activity(Activity.Action.RenamedConversation("Team"), Date(5_000), "them")))
        val undated = SessionTestEnvironment.conversation("-C", participants)
        val tied = SessionTestEnvironment.conversation("-D", participants, messageIDs = listOf("-D1"))

        val sorted = listOf(undated, tied, oldest, renamedLater).sortedByLatestMessageSentDate

        assertEquals(listOf("-B", "-A", "-D", "-C"), sorted.map { it.id.key })
    }
}
