//
//  ContextMenuInteractionTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextMenuInteractionTest {
    // MARK: - Setup

    @After
    fun tearDown() {
        ContextMenuInteraction.setCanBegin(true)
    }

    // MARK: - Tests

    @Test
    fun `disabling blocks context menu interactions`() {
        ContextMenuInteraction.setCanBegin(false)
        assertFalse(ContextMenuInteraction.canBegin)
    }

    @Test
    fun `re-enabling restores context menu interactions`() {
        ContextMenuInteraction.setCanBegin(false)
        ContextMenuInteraction.setCanBegin(true)
        assertTrue(ContextMenuInteraction.canBegin)
    }
}
