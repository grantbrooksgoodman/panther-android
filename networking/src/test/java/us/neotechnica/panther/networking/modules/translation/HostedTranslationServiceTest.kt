//
//  HostedTranslationServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.networking.modules.translation.services.HostedTranslationService
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.services.LocalTranslationArchiver

/**
 * Exercises the hosted translation pipeline's offline paths:
 * idempotent batches, local-archive hits, and input validation.
 */
class HostedTranslationServiceTest {
    @Before
    fun setUp() {
        LocalTranslationArchiver.clearArchive()
    }

    @After
    fun tearDown() {
        LocalTranslationArchiver.clearArchive()
    }

    @Test
    fun `idempotent batches return sanitized inputs in order`() =
        runTest {
            val inputs = listOf(TranslationInput("Hello"), TranslationInput("⌘World⌘"))
            val translations =
                HostedTranslationService.shared.getTranslations(
                    inputs,
                    languagePair = LanguagePair(from = "en", to = "en"),
                )

            assertEquals(2, translations.size)
            assertEquals("Hello", translations[0].output)
            assertEquals("World", translations[1].output)
        }

    @Test
    fun `translate returns the locally archived value without a network request`() =
        runTest {
            val languagePair = LanguagePair(from = "en", to = "es")
            LocalTranslationArchiver.addValue(
                Translation(
                    input = TranslationInput("Hello"),
                    output = "Hola",
                    languagePair = languagePair,
                ),
            )

            val translation =
                HostedTranslationService.shared.translate(
                    TranslationInput("Hello"),
                    languagePair = languagePair,
                )

            assertEquals("Hola", translation.output)
        }

    @Test
    fun `malformed inputs fail validation`() =
        runTest {
            try {
                HostedTranslationService.shared.translate(
                    TranslationInput(""),
                    languagePair = LanguagePair(from = "en", to = "es"),
                )
                fail("Expected a validation failure.")
            } catch (exception: Exception) {
                assertEquals("Input fails validation.", exception.descriptor)
            }
        }
}
