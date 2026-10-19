//
//  DeviceIDTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.user.models

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceIDTest {
    // MARK: - Teardown

    @After
    fun tearDown() {
        DeviceID.setDependencies(DeviceID.Dependencies())
    }

    // MARK: - Tests

    @Test
    fun `the first read mints a prefixed identifier and stores it verbatim`() {
        val saved = mutableListOf<String>()
        DeviceID.setDependencies(
            DeviceID.Dependencies(
                androidID = { "abc123" },
                read = { null },
                save = { saved.add(it) },
            ),
        )

        assertEquals("and_abc123", DeviceID.current)
        assertEquals(listOf("and_abc123"), saved)
    }

    @Test
    fun `a later read returns the stored identifier unchanged`() {
        val saved = mutableListOf<String>()
        DeviceID.setDependencies(
            DeviceID.Dependencies(
                androidID = { "abc123" },
                read = { "and_abc123" },
                save = { saved.add(it) },
            ),
        )

        assertEquals("and_abc123", DeviceID.current)
        assertEquals("and_abc123", DeviceID.current)
        assertEquals(emptyList<String>(), saved)
    }

    @Test
    fun `a missing Android ID falls back to a UUID`() {
        DeviceID.setDependencies(
            DeviceID.Dependencies(
                androidID = { null },
                read = { null },
                save = {},
            ),
        )

        val current = DeviceID.current
        assertEquals("and_", current.take(PREFIX_LENGTH))
        assertEquals(PREFIX_LENGTH + UUID_LENGTH, current.length)
    }

    private companion object {
        const val PREFIX_LENGTH = 4
        const val UUID_LENGTH = 36
    }
}
