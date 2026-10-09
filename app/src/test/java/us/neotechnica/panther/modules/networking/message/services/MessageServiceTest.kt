//
//  MessageServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.services

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.models.ImageFileExtension
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.RichMessageContent
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.networking.modules.translation.extensions.reference
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/** Exercises [MessageService.buildMessage] for text and media content. */
class MessageServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("message-service-test")
    }

    // MARK: - Text

    @Test
    fun `buildMessage builds a text message carrying its translation references`() =
        runTest {
            val translation = Translation(TranslationInput("Hello"), "Hola", LanguagePair("en", "es"))

            val message =
                MessageService.buildMessage(
                    fromAccountID = "me",
                    richContent = null,
                    translations = listOf(translation),
                )

            assertEquals("-fakeGeneratedKey", message.id)
            assertEquals(HostedContentType.Text, message.contentType)
            assertNull(message.richContent)
            assertEquals(listOf(translation.reference), message.translationReferences)
            assertEquals(translation, message.translation)
            assertNull(message.readReceipts)
            assertTrue(environment.storage.uploadedPaths.isEmpty())
        }

    @Test
    fun `buildMessage honours a preset identifier`() =
        runTest {
            val message =
                MessageService.buildMessage(
                    fromAccountID = "me",
                    presetID = "-Preset",
                    richContent = null,
                    translations = listOf(Translation(TranslationInput("Hello"), "Hola", LanguagePair("en", "es"))),
                )

            assertEquals("-Preset", message.id)
        }

    // MARK: - Media

    @Test
    fun `buildMessage uploads media and rewrites the rich content to its hosted path`() =
        runTest {
            environment.writeFile("staging/photo.jpg")
            val staged = MediaFile("staging/photo.jpg", name = "photo", fileExtension = MediaFileExtension.Image(ImageFileExtension.JPG))

            val message =
                MessageService.buildMessage(
                    fromAccountID = "me",
                    richContent = RichMessageContent.Media(staged),
                    translations = null,
                )

            val contentType = message.contentType as HostedContentType.Media
            assertEquals("media/${contentType.id}.jpg", message.imageComponent?.relativePath)
            assertEquals(contentType.id, message.imageComponent?.name)
            assertNull(message.translations)
            assertNull(message.translationReferences)
            assertEquals(listOf("media/${contentType.id}.jpg"), environment.storage.uploadedPaths.map { it.first })
        }

    // MARK: - Validation

    @Test
    fun `buildMessage rejects arguments that fail validation`() =
        runTest {
            val wellFormed = Translation(TranslationInput("Hello"), "Hola", LanguagePair("en", "es"))
            val malformed = Translation(TranslationInput("Hello"), "", LanguagePair("en", "es"))

            assertFailsValidation {
                MessageService.buildMessage(
                    fromAccountID = "!",
                    richContent = null,
                    translations = listOf(wellFormed),
                )
            }

            assertFailsValidation {
                MessageService.buildMessage(
                    fromAccountID = "me",
                    richContent = null,
                    translations = null,
                )
            }

            assertFailsValidation {
                MessageService.buildMessage(
                    fromAccountID = "me",
                    richContent = null,
                    translations = listOf(malformed),
                )
            }
        }

    @Test
    fun `getMessages rejects an empty identifier list`() =
        runTest {
            try {
                MessageService.getMessages(listOf("!"))
                fail("Expected getMessages to throw")
            } catch (exception: Exception) {
                assertEquals("No IDs provided.", exception.descriptor)
            }
        }

    // MARK: - Auxiliary

    private suspend fun assertFailsValidation(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected buildMessage to throw")
        } catch (exception: Exception) {
            assertEquals("Passed arguments fail validation.", exception.descriptor)
        }
    }
}
