//
//  ConversationSyncServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.sync.services

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.currentUserID
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.support.FakeDatabaseDelegate
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.parity.FixtureJson
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import java.io.File

class ConversationSyncServiceTest {
    // MARK: - Setup

    private lateinit var database: FakeDatabaseDelegate
    private lateinit var conversation: Conversation

    @Before
    fun setUp() {
        val directory = File(System.getProperty("java.io.tmpdir"), "conversation-sync-test-${System.nanoTime()}")
        directory.mkdirs()
        FileStore.initializeForTesting(directory)
        Persistent.initializeForTesting()
        SessionStore.reloadForTesting()

        database = FakeDatabaseDelegate()
        Networking.config.registerDatabaseDelegate(database)

        conversation = runBlocking { Conversation.decode(FixtureJson.loadObject("conversation.json")) }

        // The current user is not a participant, so synchronization takes
        // the data-only path and never fetches messages.
        Persistent.setString(PersistentStorageKey.currentUserID, "outsider")
    }

    // MARK: - Tests

    @Test
    fun `a changed server version is synchronized into the store, not refetched`() =
        runBlocking {
            val serverVersion = conversation.copy(metadata = conversation.metadata.copyWith(name = "Server Renamed"))
            assertNotEquals(conversation.encodedHash, serverVersion.encodedHash)

            // The store holds the stale version; the server node reflects the change.
            SessionStore.upsertConversation(conversation)
            database.getValuesResult = serverVersion.encoded

            val result = ConversationSyncService().synchronizeConversation(conversation)

            assertEquals("Server Renamed", result.metadata.name)
            assertEquals("Server Renamed", SessionStore.getConversation(conversation.id.key)?.metadata?.name)

            // Synchronization is read-only: it commits nothing (no refetch write path).
            assertTrue(database.committedUpdates.isEmpty())
        }
}
