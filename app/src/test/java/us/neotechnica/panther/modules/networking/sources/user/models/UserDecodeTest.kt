//
//  UserDecodeTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.user.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.parity.FixtureJson

/** Exercises the user decoder's handling of the open-conversations map. */
class UserDecodeTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("user-decode-test")
    }

    // MARK: - Tests

    @Test
    fun `open conversations decode only as a string map`() {
        val fixture = FixtureJson.loadObject("user.json").toMutableMap()
        assertEquals(1, User.decode(fixture).conversationIDs?.size)

        fixture["openConversations"] = mapOf("-FixtureConversation01" to true)

        assertFalse(User.canDecode(fixture))
        assertNull(User.decode(fixture).conversationIDs)
    }

    @Test
    fun `absent open conversations decode as none`() {
        val fixture = FixtureJson.loadObject("user.json").toMutableMap()
        fixture.remove("openConversations")

        assertTrue(User.canDecode(fixture))
        assertNull(User.decode(fixture).conversationIDs)
    }
}
