//
//  DatabaseDelegateTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.networking.modules.database.extensions.conformsTo
import kotlin.reflect.typeOf

class DatabaseDelegateTest {
    // MARK: - Tests

    @Test
    fun `a map conforms only when every key and value matches the type arguments`() {
        val values: Any = mapOf("a" to mapOf("x" to 1), "b" to mapOf("y" to "two"))

        assertTrue(values.conformsTo(typeOf<Map<String, Map<String, Any>>>()))
        assertTrue(values.conformsTo(typeOf<Map<String, Any>>()))
        assertFalse(values.conformsTo(typeOf<Map<String, String>>()))
        assertFalse(values.conformsTo(typeOf<Map<String, Map<String, Int>>>()))
    }

    @Test
    fun `a list conforms only when every element matches the type argument`() {
        val values: Any = listOf("a", "b")

        assertTrue(values.conformsTo(typeOf<List<String>>()))
        assertFalse(values.conformsTo(typeOf<List<Int>>()))
        assertFalse(values.conformsTo(typeOf<Map<String, Any>>()))
    }

    @Test
    fun `a null value conforms only to a nullable type`() {
        val value: Any? = null

        assertTrue(value.conformsTo(typeOf<String?>()))
        assertFalse(value.conformsTo(typeOf<String>()))
        assertFalse(mapOf("a" to null).conformsTo(typeOf<Map<String, Any>>()))
    }

    @Test
    fun `scalars and star projections conform by runtime type`() {
        assertTrue("text".conformsTo(typeOf<String>()))
        assertFalse("text".conformsTo(typeOf<Int>()))
        assertTrue(mapOf(1 to "a").conformsTo(typeOf<Map<*, *>>()))
    }
}
