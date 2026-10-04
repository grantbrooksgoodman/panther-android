//
//  SearchInteractionServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.content.user.models.ContextMenuInteraction

class SearchInteractionServiceTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        ChatPageStateService.setIsPresented(true)
        ContextMenuInteraction.setCanBegin(true)
    }

    @After
    fun tearDown() {
        ChatPageStateService.setIsPresented(false)
        ContextMenuInteraction.setCanBegin(true)
    }

    // MARK: - Tests

    @Test
    fun `does not trigger when the chat page is not presented`() =
        runTest {
            ChatPageStateService.setIsPresented(false)
            var presentCount = 0
            SearchInteractionService("m1") {
                presentCount += 1
                true
            }.triggerFocusedMessageCellInteractionIfNeeded()
            assertEquals(0, presentCount)
        }

    @Test
    fun `does not trigger when no message is focused`() =
        runTest {
            var presentCount = 0
            SearchInteractionService(null) {
                presentCount += 1
                true
            }.triggerFocusedMessageCellInteractionIfNeeded()
            assertEquals(0, presentCount)
        }

    @Test
    fun `triggers the interaction at most once`() =
        runTest {
            var presentCount = 0
            val service =
                SearchInteractionService("m1") {
                    presentCount += 1
                    true
                }
            service.triggerFocusedMessageCellInteractionIfNeeded()
            service.triggerFocusedMessageCellInteractionIfNeeded()
            assertEquals(1, presentCount)
        }

    @Test
    fun `retries once when the focused cell is not yet available`() =
        runTest {
            var presentCount = 0
            SearchInteractionService("m1") {
                presentCount += 1
                false
            }.triggerFocusedMessageCellInteractionIfNeeded()
            assertEquals(2, presentCount)
        }
}
