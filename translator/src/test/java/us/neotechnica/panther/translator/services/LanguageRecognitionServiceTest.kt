//
//  LanguageRecognitionServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageRecognitionServiceTest {
    // MARK: - Tests

    @Test
    fun `two identification legs without a valid sentence stop at 0,8`() =
        runTest {
            val service = service(spellChecker = { words, _ -> words.toSet() })

            val confidence = service.matchConfidence("Bonjour tout le monde", "fr")

            assertEquals(0.8f, confidence, EPSILON)
            assertFalse(confidence > SKIP_THRESHOLD)
        }

    @Test
    fun `all three legs reach 1,0 and skip translation`() =
        runTest {
            val service = service(spellChecker = { _, _ -> emptySet() })

            val confidence = service.matchConfidence("Bonjour tout le monde", "fr")

            assertEquals(1.0f, confidence, EPSILON)
            assertTrue(confidence > SKIP_THRESHOLD)
        }

    @Test
    fun `without a spell checker the spell leg passes`() =
        runTest {
            val service =
                LanguageRecognitionService(
                    LanguageRecognitionService.Dependencies(
                        identifyLanguage = { "und" },
                        identifyPossibleLanguages = { emptyList() },
                        spellChecker = null,
                    ),
                )

            assertEquals(0.2f, service.matchConfidence("Bonjour tout le monde", "fr"), EPSILON)
        }

    @Test
    fun `short, numeric, dotted, and URL-like words are not checked`() =
        runTest {
            val checked = mutableListOf<String>()
            val service =
                service(
                    spellChecker = { words, _ ->
                        checked.addAll(words)
                        emptySet()
                    },
                )

            service.matchConfidence("Go to v1.2 at http://x.io on 2 pm with friends", "en")

            assertEquals(listOf("with", "friends"), checked)
        }

    @Test
    fun `a sentence with nothing checkable is not penalized`() =
        runTest {
            val service = service(spellChecker = { words, _ -> words.toSet() })

            assertEquals(1.0f, service.matchConfidence("1 2 3", "fr"), EPSILON)
        }

    @Test
    fun `results are cached per string and language`() =
        runTest {
            var calls = 0
            val service =
                service(
                    spellChecker = { _, _ ->
                        calls += 1
                        emptySet()
                    },
                )

            service.matchConfidence("Bonjour tout le monde", "fr")
            service.matchConfidence("Bonjour tout le monde", "fr")

            assertEquals(1, calls)
        }

    // MARK: - Auxiliary

    private fun service(spellChecker: LanguageRecognitionService.SpellChecker): LanguageRecognitionService =
        LanguageRecognitionService(
            LanguageRecognitionService.Dependencies(
                identifyLanguage = { "fr" },
                identifyPossibleLanguages = { listOf("fr" to 0.9f, "en" to 0.1f) },
                spellChecker = spellChecker,
            ),
        )

    private companion object {
        const val EPSILON = 0.0001f
        const val SKIP_THRESHOLD = 0.8f
    }
}
