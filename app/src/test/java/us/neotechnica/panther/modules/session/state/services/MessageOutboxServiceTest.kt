//
//  MessageOutboxServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import java.io.File
import java.util.Date

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
    fun `decodes legacy text entry`() {
        Persistent.setString(
            PersistentStorageKey.messageOutbox,
            """[{"id":"outbox-1","conversationIDKey":"c1","fromAccountID":"u1","recipientUserIDs":["u2"],""" +
                """"text":"hello","mediaRelativePath":null,"isPenPalsConversation":false,"createdDate":1000,""" +
                """"attemptCount":1,"lastAttemptDate":null,"reservedRemoteID":null,"state":"failed"}]""",
        )
        MessageOutboxService.reloadForTesting()

        val entry = MessageOutboxService.entry("outbox-1")
        assertNotNull(entry)
        assertTrue(entry!!.payload is OutboxEntry.Payload.Text)
        assertEquals("hello", (entry.payload as OutboxEntry.Payload.Text).value)
    }

    @Test
    fun `decodes legacy media entry`() {
        Persistent.setString(
            PersistentStorageKey.messageOutbox,
            """[{"id":"outbox-2","conversationIDKey":"c1","fromAccountID":"u1","recipientUserIDs":["u2"],""" +
                """"text":"","mediaRelativePath":"media/image.jpeg","isPenPalsConversation":false,"createdDate":1000,""" +
                """"attemptCount":1,"lastAttemptDate":null,"reservedRemoteID":null,"state":"failed"}]""",
        )
        MessageOutboxService.reloadForTesting()

        val entry = MessageOutboxService.entry("outbox-2")
        assertNotNull(entry)
        assertTrue(entry!!.payload is OutboxEntry.Payload.Media)
        assertEquals("image.jpeg", (entry.payload as OutboxEntry.Payload.Media).fileName)
    }

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
    fun `migrates legacy archive to file store and clears the string`() {
        Persistent.setString(
            PersistentStorageKey.messageOutbox,
            """[{"id":"outbox-4","conversationIDKey":"c1","fromAccountID":"u1","recipientUserIDs":["u2"],""" +
                """"text":"migrated","mediaRelativePath":null,"isPenPalsConversation":false,"createdDate":1000,""" +
                """"attemptCount":1,"lastAttemptDate":null,"reservedRemoteID":null,"state":"failed"}]""",
        )
        MessageOutboxService.reloadForTesting()

        assertNull(Persistent.string(PersistentStorageKey.messageOutbox))
        val entry = MessageOutboxService.entry("outbox-4")
        assertNotNull(entry)
        assertTrue(entry!!.payload is OutboxEntry.Payload.Text)
        assertEquals("migrated", (entry.payload as OutboxEntry.Payload.Text).value)
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
}
