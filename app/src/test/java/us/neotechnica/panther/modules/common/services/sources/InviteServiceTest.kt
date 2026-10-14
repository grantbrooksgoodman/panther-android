//
//  InviteServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Exercises the invite service's pure rules: the share payload order and
 * the invitation suggestion gate without contact permission.
 */
class InviteServiceTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        Persistent.initializeForTesting()
    }

    // MARK: - Share Payload

    @Test
    fun `the share payload lists the app share link before the invitation text`() {
        assertEquals(
            "https://example.com/app\nHey, let's chat!",
            InviteService.activityItems("https://example.com/app", "Hey, let's chat!"),
        )
    }

    // MARK: - Suggestion Gate

    @Test
    fun `no suggestion is made without contact permission`() =
        runBlocking {
            assertFalse(InviteService.suggestInvitationIfNeeded())
        }
}
