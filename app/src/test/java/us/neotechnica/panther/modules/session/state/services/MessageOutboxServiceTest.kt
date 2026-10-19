//
//  MessageOutboxServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.networking.sources.common.extensions.SessionStoreStorageKey
import us.neotechnica.panther.modules.networking.sources.common.extensions.sessionStore
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import java.io.File
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class MessageOutboxServiceTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        val directory = File(System.getProperty("java.io.tmpdir"), "outbox-test-${System.nanoTime()}")
        directory.mkdirs()
        FileStore.initializeForTesting(directory)
        Persistent.initializeForTesting()
        MessageOutboxService.reloadForTesting()
    }

    // MARK: - Tests

    @Test
    fun `garbage collects unreferenced payload files`() {
        val outboxDirectory = FileStore.resolve("outbox")!!
        outboxDirectory.mkdirs()
        File(outboxDirectory, "referenced.jpeg").writeText("x")
        File(outboxDirectory, "orphan.jpeg").writeText("y")

        val entry =
            OutboxEntry(
                conversationIDKey = "c1",
                createdDate = Date(),
                fromAccountID = "u1",
                id = "outbox-3",
                isPenPalsConversation = false,
                payload = OutboxEntry.Payload.Media("referenced.jpeg", MediaFileExtension.from("jpeg")!!),
                recipientUserIDs = listOf("u2"),
                attemptCount = 1,
                lastAttemptDate = null,
                reservedRemoteID = null,
                state = OutboxEntry.State.FAILED,
                transcription = null,
            )
        MessageOutboxService.enqueue(entry)
        MessageOutboxService.reloadForTesting()

        assertTrue(File(outboxDirectory, "referenced.jpeg").exists())
        assertFalse(File(outboxDirectory, "orphan.jpeg").exists())
    }

    @Test
    fun `round-trips an enqueued entry through the archive`() {
        val entry =
            OutboxEntry(
                conversationIDKey = "c1",
                createdDate = Date(1000),
                fromAccountID = "u1",
                id = "outbox-5",
                isPenPalsConversation = false,
                payload = OutboxEntry.Payload.Text("archived"),
                recipientUserIDs = listOf("u2"),
                attemptCount = 2,
                lastAttemptDate = Date(2000),
                reservedRemoteID = "remote-5",
                state = OutboxEntry.State.FAILED,
                transcription = null,
            )
        MessageOutboxService.enqueue(entry)
        MessageOutboxService.reloadForTesting()

        val reloaded = MessageOutboxService.entry("outbox-5")
        assertNotNull(reloaded)
        assertEquals("archived", (reloaded!!.payload as OutboxEntry.Payload.Text).value)
        assertEquals(2, reloaded.attemptCount)
        assertEquals("remote-5", reloaded.reservedRemoteID)
    }

    @Test
    fun `an archived entry missing a required field is dropped`() {
        val complete = encodeEntry(entry(id = "outbox-6"))
        val incomplete = complete.filterKeys { it != "attemptCount" }
        Persistent.setArchive(
            PersistentStorageKey.sessionStore(SessionStoreStorageKey.MESSAGE_OUTBOX),
            listOf(complete, incomplete + ("id" to "outbox-7")),
        )
        MessageOutboxService.reloadForTesting()

        assertNotNull(MessageOutboxService.entry("outbox-6"))
        assertNull(MessageOutboxService.entry("outbox-7"))
    }

    // MARK: - Retry

    @Test
    fun `a retry whose send throws a runtime exception marks the entry failed`() =
        runBlocking {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            try {
                val environment = MessageDecodeEnvironment()
                environment.install("outbox-retry-test")
                MessageOutboxService.reloadForTesting()

                val me = SessionTestEnvironment.user(id = "u1", languageCode = "en")
                // Blocking the sender keeps the recipient out of the push fan-out.
                val them =
                    SessionTestEnvironment.user(
                        id = "u2",
                        languageCode = "en",
                        nationalNumber = "5559876543",
                        blockedUserIDs = listOf("u1"),
                    )
                environment.signIn(me)
                SessionStore.upsertUser(them)
                SessionStore.upsertConversation(SessionTestEnvironment.conversation("c1", listOf("u1", "u2")))
                environment.database.commitFailure = IllegalStateException("Commit failed.")

                MessageOutboxService.enqueue(entry(id = "outbox-8"))
                MessageOutboxService.retry("outbox-8")

                assertEquals(OutboxEntry.State.FAILED, MessageOutboxService.entry("outbox-8")?.state)
            } finally {
                // Let any background notify settle before Main goes away.
                delay(SETTLE_MILLIS)
                Dispatchers.resetMain()
            }
        }

    // MARK: - Auxiliary

    private fun entry(id: String): OutboxEntry =
        OutboxEntry(
            conversationIDKey = "c1",
            createdDate = Date(1000),
            fromAccountID = "u1",
            id = id,
            isPenPalsConversation = false,
            payload = OutboxEntry.Payload.Text("archived"),
            recipientUserIDs = listOf("u2"),
            attemptCount = 1,
            lastAttemptDate = null,
            reservedRemoteID = null,
            state = OutboxEntry.State.FAILED,
            transcription = null,
        )

    private companion object {
        const val SETTLE_MILLIS = 100L
    }
}
