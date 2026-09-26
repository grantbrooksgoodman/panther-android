//
//  PersistentArchiveTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import java.io.File

class PersistentArchiveTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        val directory = File(System.getProperty("java.io.tmpdir"), "persistent-test-${System.nanoTime()}")
        directory.mkdirs()
        FileStore.initializeForTesting(directory)
        Persistent.initializeForTesting()
    }

    // MARK: - Tests

    @Test
    fun `archive round trips encoded maps`() {
        val key = PersistentStorageKey("testArchive")
        val encoded =
            listOf(
                mapOf("id" to "a", "count" to 3L, "nested" to mapOf("flag" to true)),
                mapOf("id" to "b", "count" to 7L),
            )

        Persistent.setArchive(key, encoded)
        val decoded = Persistent.archive(key) { it }

        assertEquals(2, decoded?.size)
        assertEquals("a", decoded?.get(0)?.get("id"))
        assertEquals(3L, decoded?.get(0)?.get("count"))
        assertEquals(mapOf("flag" to true), decoded?.get(0)?.get("nested"))
    }

    @Test
    fun `setting a null archive removes it`() {
        val key = PersistentStorageKey("removable")
        Persistent.setArchive(key, listOf(mapOf("id" to "a")))
        Persistent.setArchive(key, null)

        assertNull(Persistent.archive(key) { it })
    }

    @Test
    fun `reset preserves only the specified keys`() {
        val keep = PersistentStorageKey("keep")
        val drop = PersistentStorageKey("drop")
        val archived = PersistentStorageKey("archived")

        Persistent.setString(keep, "kept")
        Persistent.setString(drop, "dropped")
        Persistent.setArchive(archived, listOf(mapOf("id" to "a")))

        Persistent.reset(preserving = listOf(keep))

        assertEquals("kept", Persistent.string(keep))
        assertNull(Persistent.string(drop))
        assertNull(Persistent.archive(archived) { it })
    }
}
