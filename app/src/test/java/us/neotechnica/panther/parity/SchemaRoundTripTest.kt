//
//  SchemaRoundTripTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.parity

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.message.models.LocalAudioFilePath
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.serializable.decode
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

class SchemaRoundTripTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("schema-round-trip-test")
        environment.signIn(MessageDecodeEnvironment.user(id = "androidFixtureUser0002", languageCode = "es"))

        environment.hostedTranslation.seed(
            inputValueEncodedHash = "-FixtureTranslation01",
            translation = Translation(TranslationInput("Hello"), "Hola", LanguagePair("en", "es")),
        )

        val audioTranslation = Translation(TranslationInput("Hola"), "안녕", LanguagePair("es", "ko"))
        environment.hostedTranslation.seed(inputValueEncodedHash = "-FixtureAudioReference01", translation = audioTranslation)
        val localAudioFilePath = LocalAudioFilePath.from("-FixtureMessage000003", audioTranslation)
        environment.writeFile(localAudioFilePath.inputFilePathString)
        environment.writeFile(localAudioFilePath.outputFilePathString)

        environment.writeFile("media/-FixtureMediaID000001.jpg")
    }

    // MARK: - Round-Trip Tests

    @Test
    fun `user decodes and re-encodes structurally identically`() =
        runTest {
            val fixture = FixtureJson.loadObject("user.json")
            assertEquals(fixture, User.decode(fixture).encoded)
        }

    @Test
    fun `conversation decodes and re-encodes structurally identically`() =
        runTest {
            val fixture = FixtureJson.loadObject("conversation.json")
            assertEquals(fixture, Conversation.decode(fixture).encoded)
        }

    @Test
    fun `message decodes and re-encodes structurally identically`() =
        runTest {
            val fixture = FixtureJson.loadObject("message.json")
            assertEquals(fixture, Message.decode(fixture).encoded)
        }

    // A media message carries its file identifier and extension in the composite
    // `contentType` string ("<mime> – <id> – <ext>"), covering the R2/R3 media surface.
    @Test
    fun `media message decodes and re-encodes structurally identically`() =
        runTest {
            val fixture = FixtureJson.loadObject("message_media.json")
            assertEquals(fixture, Message.decode(fixture).encoded)
        }

    // An audio message's `contentType` is its bare MIME string, covering the R4 audio surface.
    @Test
    fun `audio message decodes and re-encodes structurally identically`() =
        runTest {
            val fixture = FixtureJson.loadObject("message_audio.json")
            assertEquals(fixture, Message.decode(fixture).encoded)
        }

    // MARK: - Identity-Hash Tests

    @Test
    fun `decoded type hashes match iOS`() =
        runTest {
            val expected = FixtureJson.loadObject("type_hashes.json")

            assertEquals(
                expected["userHash"],
                User.decode(FixtureJson.loadObject("user.json")).encodedHash,
            )

            assertEquals(
                expected["conversationHash"],
                Conversation.decode(FixtureJson.loadObject("conversation.json")).encodedHash,
            )

            assertEquals(
                expected["messageHash"],
                Message.decode(FixtureJson.loadObject("message.json")).encodedHash,
            )
        }
}
