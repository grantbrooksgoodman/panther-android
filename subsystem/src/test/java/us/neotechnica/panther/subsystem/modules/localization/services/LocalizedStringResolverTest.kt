//
//  LocalizedStringResolverTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.localization.services

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCodeDictionary
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import java.io.File
import java.nio.file.Files

class LocalizedStringResolverTest {
    // MARK: - Setup

    private lateinit var directory: File

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("localized-string-resolver-test").toFile()
        FileStore.initializeForTesting(directory)
        LocalizedStringResolver.initializeForTesting { source ->
            if (source == LocalizationSource.SUBSYSTEM) subsystemTable else emptyMap()
        }
    }

    @After
    fun tearDown() {
        RuntimeStorage.remove(StoredItemKey.languageCode)
        RuntimeStorage.remove(StoredItemKey.languageCodeDictionary)
        LocalizedStringResolver.clearCache()
        directory.deleteRecursively()
    }

    // MARK: - Tests

    @Test
    fun `an unsupported language code reverts to english and is logged`() {
        CoreUtilities.setLanguageCode("xx")

        LocalizedStringResolver.initialize()

        assertEquals("en", RuntimeStorage.languageCode)
        val record = Logger.sessionRecordFilePath?.takeIf { it.exists() }?.readText() ?: ""
        assertTrue(record.contains("Unsupported language code; reverting to English."))
        assertTrue(record.contains("[LanguageCode: xx]"))
    }

    @Test
    fun `a supported language code is left untouched`() {
        CoreUtilities.setLanguageCode("fr")

        LocalizedStringResolver.initialize()

        assertEquals("fr", RuntimeStorage.languageCode)
    }

    @Test
    fun `the unsupported languages are absent from the dictionary`() {
        CoreUtilities.setLanguageCode("en")

        LocalizedStringResolver.initialize()

        val dictionary = RuntimeStorage.languageCodeDictionary ?: emptyMap()
        for (code in listOf("ba", "ceb", "jv", "la", "mr", "ms", "udm")) {
            assertFalse(code, dictionary.containsKey(code))
        }
        assertEquals(setOf("en", "fr"), dictionary.keys)
    }

    @Test
    fun `an unsupported device language reverts to english`() {
        CoreUtilities.setLanguageCode("ms")

        LocalizedStringResolver.initialize()

        assertEquals("en", RuntimeStorage.languageCode)
    }

    // MARK: - Auxiliary

    private val subsystemTable: Map<String, Map<String, String>>
        get() =
            mapOf(
                "language_codes" to
                    mapOf(
                        "ba" to "Bashkir",
                        "ceb" to "Cebuano",
                        "en" to "English",
                        "fr" to "French (Français)",
                        "jv" to "Javanese",
                        "la" to "Latin",
                        "mr" to "Marathi",
                        "ms" to "Malay",
                        "udm" to "Udmurt",
                    ),
            )
}
