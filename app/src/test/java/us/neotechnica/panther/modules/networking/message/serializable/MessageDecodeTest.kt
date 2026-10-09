//
//  MessageDecodeTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.serializable

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.LocalAudioFilePath
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.parity.FixtureJson
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * Exercises the message decoder against the parity decode vectors:
 * translation selection for sent and received messages, the identity
 * rewrite, nearly-matching previous language codes, validator
 * rejection, and the media and audio content resolution.
 */
class MessageDecodeTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("message-decode-test")

        @Suppress("UNCHECKED_CAST")
        val archive = FixtureJson.loadObject("message_decode_vectors.json")["archive"] as List<Map<String, Any?>>
        for (entry in archive) {
            val languagePair = checkNotNull(LanguagePair.fromString(entry["languagePair"] as String))
            environment.hostedTranslation.seed(
                inputValueEncodedHash = entry["hash"] as String,
                translation =
                    Translation(
                        input = TranslationInput(entry["input"] as String),
                        output = entry["output"] as String,
                        languagePair = languagePair,
                    ),
            )
        }
    }

    @After
    fun tearDown() {
        ConversationSessionService.setCurrentConversation(null)
    }

    // MARK: - Selection Vectors

    @Test
    fun `selection vectors decode as expected`() =
        runTest {
            @Suppress("UNCHECKED_CAST")
            val cases = FixtureJson.loadObject("message_decode_vectors.json")["cases"] as List<Map<String, Any?>>

            for (case in cases) {
                val name = case["name"] as String

                @Suppress("UNCHECKED_CAST")
                val currentUser = case["currentUser"] as Map<String, Any?>

                @Suppress("UNCHECKED_CAST")
                environment.signIn(
                    MessageDecodeEnvironment.user(
                        id = currentUser["id"] as String,
                        languageCode = currentUser["languageCode"] as String,
                        previousLanguageCodes = currentUser["previousLanguageCodes"] as? List<String>,
                    ),
                )

                val data = textMessageData(case["fromAccount"] as String, case["translations"] as List<*>)

                if (case["throws"] == true) {
                    try {
                        Message.decode(data)
                        fail("$name: expected the decode to throw")
                    } catch (_: Exception) {
                        continue
                    }
                }

                @Suppress("UNCHECKED_CAST")
                val expected = case["expected"] as Map<String, Any?>
                val message = Message.decode(data)
                val translation = checkNotNull(message.translation) { "$name: no translation" }

                assertEquals(name, expected["languagePair"], translation.languagePair.string)
                assertEquals(name, expected["input"], translation.input.value)
                assertEquals(name, expected["output"], translation.output)
                assertNull(name, message.richContent)
            }
        }

    @Test
    fun `every reference of a received message is resolved when none targets the current user`() =
        runTest {
            environment.signIn(MessageDecodeEnvironment.user(id = "me", languageCode = "de"))

            val message =
                Message.decode(
                    textMessageData("them", listOf("en-es | -HashHelloEnEs", "en-fr | -HashHelloEnFr")),
                )

            assertEquals(listOf("en-es", "en-fr"), message.translations?.map { it.languagePair.string })
            assertEquals(2, environment.hostedTranslation.lookups.size)
        }

    // MARK: - Rich Content

    @Test
    fun `media messages resolve their media component from the local copy`() =
        runTest {
            environment.signIn(MessageDecodeEnvironment.user(id = "me", languageCode = "es"))
            environment.writeFile("media/-MediaID.jpg")

            val message = Message.decode(FixtureJson.loadObject("message_media.json") + ("contentType" to "image/jpeg – -MediaID – jpg"))

            assertTrue(message.contentType.isMedia)
            assertNull(message.translations)
            assertEquals("media/-MediaID.jpg", message.imageComponent?.relativePath)
            assertNull(message.videoComponent)
            assertNull(message.documentComponent)
            assertEquals("media/-MediaID.jpg", message.localMediaFilePath?.relativePathString)
        }

    @Test
    fun `media messages download a missing media component`() =
        runTest {
            environment.signIn(MessageDecodeEnvironment.user(id = "me", languageCode = "es"))
            environment.storage.hostedItems["media/-MediaID.jpg"] = byteArrayOf(9, 9, 9)

            val message = Message.decode(FixtureJson.loadObject("message_media.json") + ("contentType" to "image/jpeg – -MediaID – jpg"))

            assertNotNull(message.imageComponent)
            assertTrue(checkNotNull(environment.storage.hostedItems["media/-MediaID.jpg"]).isNotEmpty())
        }

    @Test
    fun `audio messages resolve their audio component beside the selected translation`() =
        runTest {
            environment.signIn(MessageDecodeEnvironment.user(id = "me", languageCode = "ko"))

            val data = FixtureJson.loadObject("message_audio.json") + ("translations" to listOf("es-ko | -HashHolaEsKo"))
            val translation = checkNotNull(environment.hostedTranslation.archivedTranslations["-HashHolaEsKo" to "es-ko"])
            val localAudioFilePath = LocalAudioFilePath.from(data["id"] as String, translation)
            environment.writeFile(localAudioFilePath.inputFilePathString)
            environment.writeFile(localAudioFilePath.outputFilePathString)

            val message = Message.decode(data)

            assertTrue(message.contentType.isAudio)
            assertEquals("es-ko", message.translation?.languagePair?.string)
            val audioComponent = checkNotNull(message.audioComponent)
            assertEquals(localAudioFilePath.inputFilePathString, audioComponent.original.relativePath)
            assertEquals(localAudioFilePath.outputFilePathString, audioComponent.translated.relativePath)
            assertEquals(localAudioFilePath.outputDirectoryPathString, audioComponent.translatedDirectoryPath)
            assertEquals(localAudioFilePath, message.localAudioFilePath)
        }

    // MARK: - Can Decode

    @Test
    fun `canDecode rejects a text message without references`() {
        assertTrue(Message.canDecode(textMessageData("them", listOf("en-es | -HashHelloEnEs"))))
        assertEquals(false, Message.canDecode(textMessageData("them", listOf("!"))))
        assertEquals(false, Message.canDecode(textMessageData("them", listOf("en-es | -HashHelloEnEs")) - "sentDate"))
    }

    // MARK: - Auxiliary

    private fun textMessageData(
        fromAccountID: String,
        translations: List<*>,
    ): Map<String, Any?> =
        mapOf(
            "id" to "-Message",
            "fromAccount" to fromAccountID,
            "contentType" to HostedContentType.Text.hostedValue,
            "translations" to translations,
            "readReceipts" to listOf("!"),
            "sentDate" to "2025-08-19 22:30:00 GMT",
        )
}
