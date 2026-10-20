//
//  CoreDatabaseTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.networking.modules.database.services.CoreDatabase

class CoreDatabaseTest {
    // MARK: - Properties

    private val coreDatabase = CoreDatabase()

    // MARK: - Tests

    @Test
    fun `a null value is encodable so a write can delete its path`() {
        assertTrue(coreDatabase.isEncodable(null))
        assertTrue(coreDatabase.isEncodable(mapOf("users/a/pushTokens/token" to null)))
        assertTrue(coreDatabase.isEncodable(listOf("a", null)))
    }

    @Test
    fun `scalars and collections of encodable values are encodable`() {
        assertTrue(coreDatabase.isEncodable("text"))
        assertTrue(coreDatabase.isEncodable(true))
        assertTrue(coreDatabase.isEncodable(1.5))
        assertTrue(coreDatabase.isEncodable(mapOf("a" to listOf(1, mapOf("b" to "c")))))
    }

    @Test
    fun `an unsupported value is not encodable at any depth`() {
        assertFalse(coreDatabase.isEncodable(Any()))
        assertFalse(coreDatabase.isEncodable(listOf("a", Any())))
        assertFalse(coreDatabase.isEncodable(mapOf("a" to mapOf("b" to Any()))))
    }
}
