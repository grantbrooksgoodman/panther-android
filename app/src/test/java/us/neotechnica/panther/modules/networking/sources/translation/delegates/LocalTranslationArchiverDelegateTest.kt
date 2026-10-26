//
//  LocalTranslationArchiverDelegateTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.translation.delegates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.io.File

class LocalTranslationArchiverDelegateTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        val directory = File(System.getProperty("java.io.tmpdir"), "translation-archiver-test-${System.nanoTime()}")
        directory.mkdirs()
        FileStore.initializeForTesting(directory)
        Persistent.initializeForTesting()
    }

    // MARK: - Tests

    @Test
    fun `getValue keyed by input-value hash and language pair hits after addValue`() {
        val archiver = LocalTranslationArchiverDelegate()
        val translation = translation("Hello", "Hola", LanguagePair("en", "es"))
        archiver.addValue(translation)
        archiver.flushForTesting()

        // The same key the reference hash and TranslationService use.
        assertEquals(translation, archiver.getValue(encodedHashOf(listOf("Hello")), LanguagePair("en", "es")))

        // A different language pair is a miss.
        assertNull(archiver.getValue(encodedHashOf(listOf("Hello")), LanguagePair("en", "fr")))
    }

    @Test
    fun `removeValue and clearArchive evict entries`() {
        val archiver = LocalTranslationArchiverDelegate()
        val pair = LanguagePair("en", "es")
        archiver.addValue(translation("Hello", "Hola", pair))
        archiver.addValue(translation("Bye", "Adios", pair))

        archiver.removeValue(encodedHashOf(listOf("Hello")), pair)
        assertNull(archiver.getValue(encodedHashOf(listOf("Hello")), pair))
        assertEquals("Adios", archiver.getValue(encodedHashOf(listOf("Bye")), pair)?.output)

        archiver.clearArchive()
        archiver.flushForTesting()
        assertNull(archiver.getValue(encodedHashOf(listOf("Bye")), pair))
    }

    @Test
    fun `an archived translation survives a fresh instance`() {
        val pair = LanguagePair("en", "es")
        val translation = translation("Hello", "Hola", pair)

        val writer = LocalTranslationArchiverDelegate()
        writer.addValue(translation)
        writer.flushForTesting()

        // A fresh instance decodes the archive from disk and resolves the entry.
        val reopened = LocalTranslationArchiverDelegate()
        assertEquals(translation, reopened.getValue(encodedHashOf(listOf("Hello")), pair))
    }

    @Test
    fun `changes are written together once the write delay passes`() {
        val archiver = LocalTranslationArchiverDelegate()
        val pair = LanguagePair("en", "es")
        archiver.addValue(translation("Hello", "Hola", pair))
        archiver.addValue(translation("Bye", "Adios", pair))

        // Nothing reaches the disk until the pending write runs.
        assertFalse(archiveFile().exists())

        archiver.flushForTesting()
        assertTrue(archiveFile().exists())

        val reopened = LocalTranslationArchiverDelegate()
        assertEquals("Hola", reopened.getValue(encodedHashOf(listOf("Hello")), pair)?.output)
        assertEquals("Adios", reopened.getValue(encodedHashOf(listOf("Bye")), pair)?.output)
    }

    @Test
    fun `adding a translation the archive already holds writes nothing`() {
        val archiver = LocalTranslationArchiverDelegate()
        val translation = translation("Hello", "Hola", LanguagePair("en", "es"))
        archiver.addValue(translation)
        archiver.flushForTesting()
        archiveFile().delete()

        archiver.addValue(translation)
        archiver.addValues(setOf(translation))
        archiver.flushForTesting()

        assertFalse(archiveFile().exists())
    }

    // MARK: - Auxiliary

    private fun archiveFile(): File = FileStore.resolve("persistent/${PersistentStorageKey.translationArchive.rawValue}.json")!!

    private fun translation(
        input: String,
        output: String,
        languagePair: LanguagePair,
    ): Translation =
        Translation(
            input = TranslationInput(input),
            output = output,
            languagePair = languagePair,
        )
}
