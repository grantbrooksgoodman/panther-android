//
//  UserRemotelyUpdatableTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.user.remotelyupdatable

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.support.FakeDatabaseDelegate
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.parity.FixtureJson
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import java.io.File

class UserRemotelyUpdatableTest {
    // MARK: - Setup

    private lateinit var database: FakeDatabaseDelegate

    @Before
    fun setUp() {
        val directory = File(System.getProperty("java.io.tmpdir"), "user-updatable-test-${System.nanoTime()}")
        directory.mkdirs()
        FileStore.initializeForTesting(directory)
        Persistent.initializeForTesting()
        SessionStore.reloadForTesting()

        database = FakeDatabaseDelegate()
        Networking.config.registerDatabaseDelegate(database)
    }

    // MARK: - Tests

    @Test
    fun `update pushTokens writes only the added and removed entries`() =
        runTest {
            val user =
                User.decode(FixtureJson.loadObject("user.json")).copy(pushTokens = listOf("tokenA", "tokenB"))
            val basePath = "users/${user.id}/pushTokens"

            val updated = user.update(UserUpdatableKey.PUSH_TOKENS, to = listOf("tokenB", "tokenC"))

            val updates = database.committedUpdates.single()
            assertEquals(true, updates["$basePath/tokenC"])
            assertTrue(updates.containsKey("$basePath/tokenA"))
            assertNull(updates["$basePath/tokenA"])
            assertFalse(updates.containsKey("$basePath/tokenB"))

            assertEquals(listOf("tokenB", "tokenC"), updated.pushTokens)
            assertEquals(updated, SessionStore.users[user.id])
        }

    @Test
    fun `update pushTokens with no change writes nothing`() =
        runTest {
            val user =
                User.decode(FixtureJson.loadObject("user.json")).copy(pushTokens = listOf("tokenA"))

            user.update(UserUpdatableKey.PUSH_TOKENS, to = listOf("tokenA"))

            assertTrue(database.committedUpdates.isEmpty())
        }
}
