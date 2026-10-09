//
//  TranslationSerializableTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.networking.modules.translation.extensions.reference
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.networking.modules.translation.serializable.encoded
import us.neotechnica.panther.networking.modules.translation.serializable.from
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.services.LocalTranslationArchiver

/**
 * Exercises the translation serialization surface: encoding to a
 * reference, archive-first decoding, and archive-on-decode.
 */
class TranslationSerializableTest {
    private val languagePair = LanguagePair(from = "en", to = "es")

    @Before
    fun setUp() {
        LocalTranslationArchiver.clearArchive()
    }

    @After
    fun tearDown() {
        LocalTranslationArchiver.clearArchive()
    }

    @Test
    fun `inline archived references decode without the archive and are archived on decode`() =
        runTest {
            val translation =
                Translation(
                    input = TranslationInput("Hello"),
                    output = "Hola",
                    languagePair = languagePair,
                )

            val decoded = Translation.from(translation.encoded)
            assertEquals("Hello", decoded.input.value)
            assertEquals("Hola", decoded.output)
            assertEquals(languagePair.string, decoded.languagePair.string)

            // Archive-on-decode: the decoded translation is now retrievable locally.
            assertNotNull(
                LocalTranslationArchiver.getValue(
                    inputValueEncodedHash = encodedHashOf(listOf("Hello")),
                    languagePair = languagePair,
                ),
            )
        }

    @Test
    fun `hash-only references resolve from the local archive first`() =
        runTest {
            val archived =
                Translation(
                    input = TranslationInput("Goodbye"),
                    output = "Adiós",
                    languagePair = languagePair,
                )

            LocalTranslationArchiver.addValue(archived)

            val hashOnlyReference =
                TranslationReference(
                    languagePair = languagePair,
                    type = TranslationReference.Type.Archived(encodedHashOf(listOf("Goodbye"))),
                )

            val decoded = Translation.from(hashOnlyReference)
            assertEquals("Adiós", decoded.output)
        }

    @Test
    fun `idempotent references decode from their encoded input`() =
        runTest {
            val idempotentPair = LanguagePair(from = "en", to = "en")
            val translation =
                Translation(
                    input = TranslationInput("Hello"),
                    output = "Hello",
                    languagePair = idempotentPair,
                )

            val decoded = Translation.from(translation.reference)
            assertEquals("Hello", decoded.input.value)
            assertEquals("Hello", decoded.output)
        }
}
