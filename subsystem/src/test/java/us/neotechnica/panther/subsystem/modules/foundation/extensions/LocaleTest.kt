//
//  LocaleTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocaleTest {
    // MARK: - Tests

    @Test
    fun `legacy hebrew code resolves to he`() {
        assertEquals("he", systemLanguageCode(tags(Locale("iw")), setOf("he")))
    }

    @Test
    fun `legacy indonesian code resolves to id`() {
        assertEquals("id", systemLanguageCode(tags(Locale("in")), setOf("id")))
    }

    @Test
    fun `legacy yiddish code resolves to yi`() {
        assertEquals("yi", systemLanguageCode(tags(Locale("ji")), setOf("yi")))
    }

    @Test
    fun `a script and region qualified tag resolves to its language`() {
        assertEquals("zh", systemLanguageCode(listOf("zh-Hant-TW"), setOf("zh")))
    }

    @Test
    fun `a single-letter code falls back to english`() {
        assertEquals("en", systemLanguageCode(listOf("x"), setOf("fr")))
    }

    @Test
    fun `an empty preferred list falls back to english`() {
        assertEquals("en", systemLanguageCode(emptyList(), setOf("fr")))
    }

    @Test
    fun `the first supported preferred language wins`() {
        assertEquals("fr", systemLanguageCode(listOf("ms-MY", "fr-FR", "de-DE"), setOf("fr", "de")))
    }

    @Test
    fun `the primary language is used when none is supported`() {
        assertEquals("ms", systemLanguageCode(listOf("ms-MY", "jv-ID"), setOf("fr")))
    }

    // MARK: - Auxiliary

    private fun tags(vararg locales: Locale): List<String> = locales.map { it.toLanguageTag() }
}
