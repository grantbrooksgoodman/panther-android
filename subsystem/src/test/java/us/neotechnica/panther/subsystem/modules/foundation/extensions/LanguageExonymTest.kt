//
//  LanguageExonymTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

/** Verifies the language exonym and language name string extensions. */
class LanguageExonymTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        Persistent.initializeForTesting()
        RuntimeStorage.languageCodeDictionary = mapOf("en" to "English", "es" to "Spanish (Español)")
    }

    @After
    fun tearDown() {
        RuntimeStorage.languageCodeDictionary = null
    }

    // MARK: - Tests

    @Test
    fun `the exonym is the name before its endonym`() {
        assertEquals("Spanish", "es".languageExonym)
        assertEquals("English", "en".languageExonym)
    }

    @Test
    fun `the language name keeps the endonym`() {
        assertEquals("Spanish (Español)", "es".languageName)
    }

    @Test
    fun `codes are matched after lowercasing and trimming`() {
        assertEquals("Spanish", " ES ".languageExonym)
    }

    @Test
    fun `unknown and blank codes have no exonym`() {
        assertNull("zz".languageExonym)
        assertNull("".languageExonym)
        assertNull("   ".languageExonym)
    }

    @Test
    fun `nothing resolves without a language code dictionary`() {
        RuntimeStorage.languageCodeDictionary = null
        assertNull("es".languageExonym)
    }
}
