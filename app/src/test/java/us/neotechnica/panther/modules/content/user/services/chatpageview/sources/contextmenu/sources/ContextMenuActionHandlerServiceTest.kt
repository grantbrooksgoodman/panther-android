//
//  ContextMenuActionHandlerServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.contextmenu.sources

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.models.ImageFileExtension
import us.neotechnica.panther.modules.common.models.MediaFile
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.content.user.extensions.consentRequestMessageID
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.AlternateMessageService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.saveMedia
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.message.models.RichMessageContent
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.io.File
import java.util.Date
import java.util.Locale

/**
 * Exercises the context menu action handler: the menu assembled per
 * content type and state, and the copy, speak, save, and alternate
 * actions, each through injected collaborators.
 */
class ContextMenuActionHandlerServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val defaultLocale = Locale.getDefault()

    private var copiedText: String? = null
    private var currentConversation: Conversation? = null
    private var currentUser: User? = null
    private var filedReports = mutableListOf<Exception>()
    private var isSpeaking = false
    private var matchConfidences = mutableMapOf<String, Float>()
    private var reportedErrorCodes = listOf<String>()
    private var savedMediaFiles = mutableListOf<MediaFile>()
    private var speakSucceeds = true
    private var spokenUtterances = mutableListOf<Triple<String, String, String>>()
    private var stoppedPlayback = false
    private var stoppedSpeaking = false
    private var successHUDCount = 0
    private var toggles = mutableListOf<Pair<AlternateMessageService.AlternateMessageType, String>>()
    private var voicelessLanguageCodes = mutableSetOf<String>()

    private val service =
        ContextMenuActionHandlerService(
            ContextMenuActionHandlerService.Dependencies(
                copyToPasteboard = { copiedText = it },
                currentConversation = { currentConversation },
                currentUser = { currentUser },
                fileReport = { filedReports.add(it) },
                isSpeaking = { isSpeaking },
                isTextToSpeechSupported = { it !in voicelessLanguageCodes },
                matchConfidence = { _, languageCode -> matchConfidences[languageCode] ?: 1f },
                presentExportController = {},
                presentReactionDetails = {},
                reportedErrorCodes = { reportedErrorCodes },
                saveMedia = { savedMediaFiles.add(it) },
                scope = CoroutineScope(Dispatchers.Unconfined),
                showSuccessHUD = { successHUDCount += 1 },
                speak = { text, languageCode, messageID ->
                    spokenUtterances.add(Triple(text, languageCode, messageID))
                    speakSucceeds
                },
                stopPlayback = { stoppedPlayback = true },
                stopSpeakingIfNeeded = { stoppedSpeaking = true },
                toggleAlternate = { type, message -> toggles.add(type to message.id) },
            ),
        )

    @Before
    fun setUp() {
        environment.install("context-menu-action-handler-service-test")
        val me = SessionTestEnvironment.user(id = "me", languageCode = "en")
        environment.signIn(me)
        currentUser = me
        currentConversation = SessionTestEnvironment.conversation("c1", listOf("me", "a"))

        LocalizedStringResolver.initializeForTesting { source ->
            val root = if (source == LocalizationSource.APP) "src/main/assets" else "../subsystem/src/main/assets"
            val json = Json.parseToJsonElement(File(root, source.assetName).readText()).jsonObject
            json.mapValues { (_, languages) ->
                languages.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            }
        }
        CoreUtilities.setLanguageCode("en")
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        LocalizedStringResolver.clearCache()
        AlternateMessageService.restoreAllAlternateTextMessageIDs()
        AlternateMessageService.restoreAllAudioTranscriptionMessageIDs()
    }

    // MARK: - Menu Assembly

    @Test
    fun `a received translated text message offers copy, speak, report, and view original`() {
        val message = receivedTextMessage()

        val titles = service.menuForMessage(message)?.map { it.title }

        assertEquals(listOf("Copy", "Speak", "Report a mistranslation", "View original"), titles)
    }

    @Test
    fun `an own message in a group never offers the alternate action`() {
        currentConversation = SessionTestEnvironment.conversation("c1", listOf("me", "a", "b"))
        val message = ownTextMessage()

        val titles = service.menuForMessage(message)?.map { it.title }

        assertEquals(listOf("Copy", "Speak"), titles)
    }

    @Test
    fun `speaking hides the report and alternate actions and swaps the speak title`() {
        isSpeaking = true

        val titles = service.menuForMessage(receivedTextMessage())?.map { it.title }

        assertEquals(listOf("Copy", "Stop speaking"), titles)
    }

    @Test
    fun `a message whose language has no installed voice does not offer speak`() {
        voicelessLanguageCodes += "en"

        val titles = service.menuForMessage(receivedTextMessage())?.map { it.title }

        assertEquals(listOf("Copy", "Report a mistranslation", "View original"), titles)
    }

    @Test
    fun `speaking offers stop speaking even without an installed voice`() {
        isSpeaking = true
        voicelessLanguageCodes += "en"

        val titles = service.menuForMessage(receivedTextMessage())?.map { it.title }

        assertEquals(listOf("Copy", "Stop speaking"), titles)
    }

    @Test
    fun `an already-reported mistranslation is no longer offered`() {
        val message = receivedTextMessage()
        select("Report a mistranslation", message)
        assertEquals(1, filedReports.size)

        reportedErrorCodes = listOf(filedReports.single().code)
        val titles = service.menuForMessage(message)?.map { it.title }

        assertEquals(listOf("Copy", "Speak", "View original"), titles)
    }

    @Test
    fun `an idempotent translation offers neither report nor alternate`() {
        val message = receivedTextMessage(pair = LanguagePair("en", "en"), output = "Hello")

        val titles = service.menuForMessage(message)?.map { it.title }

        assertEquals(listOf("Copy", "Speak"), titles)
    }

    @Test
    fun `a media message offers only the save action`() {
        val titles = service.menuForMessage(mediaMessage())?.map { it.title }

        assertEquals(listOf("Save"), titles)
    }

    @Test
    fun `a consent message offers no menu`() {
        val consentText = "I consent to receiving messages from you."
        val message = receivedTextMessage(input = consentText, output = consentText, pair = LanguagePair("en", "en"))
        Message.consentRequestMessageID = message.id

        assertNull(service.menuForMessage(message))
        Message.consentRequestMessageID = null
    }

    // MARK: - Actions

    @Test
    fun `copy places the displayed text on the pasteboard`() {
        val message = receivedTextMessage()

        select("Copy", message)

        assertEquals("Hello", copiedText)
    }

    @Test
    fun `view original toggles the alternate text`() {
        val message = receivedTextMessage()

        select("View original", message)

        assertEquals(listOf(AlternateMessageService.AlternateMessageType.ALTERNATE_TEXT to message.id), toggles)
    }

    @Test
    fun `speak stops playback and speaks the displayed side's language`() {
        val message = receivedTextMessage()

        select("Speak", message)

        assertTrue(stoppedPlayback)
        assertEquals(Triple("Hello", "en", message.id), spokenUtterances.single())
        assertEquals(message, service.speakingMessage)

        service.resetSpeakingMessage()
        assertNull(service.speakingMessage)
    }

    @Test
    fun `speak swaps languages when an own message matches the other side and both sides read the same`() {
        val message = ownTextMessage(input = "Taco", output = "Taco")
        matchConfidences["en"] = 0.1f
        matchConfidences["es"] = 0.95f

        select("Speak", message)

        assertEquals("es", spokenUtterances.single().second)
    }

    @Test
    fun `speak keeps the sender's language when the matched language has no installed voice`() {
        val message = ownTextMessage(input = "Taco", output = "Taco")
        matchConfidences["en"] = 0.1f
        matchConfidences["es"] = 0.95f
        voicelessLanguageCodes += "es"

        select("Speak", message)

        assertEquals("en", spokenUtterances.single().second)
    }

    @Test
    fun `an utterance that fails to queue marks no message as speaking`() {
        speakSucceeds = false

        select("Speak", receivedTextMessage())

        assertEquals(1, spokenUtterances.size)
        assertNull(service.speakingMessage)
    }

    @Test
    fun `speak while speaking only stops the utterance`() {
        isSpeaking = true

        select("Stop speaking", receivedTextMessage())

        assertTrue(stoppedSpeaking)
        assertTrue(spokenUtterances.isEmpty())
    }

    @Test
    fun `save writes an image to the shared collection and shows the success HUD`() {
        val message = mediaMessage()

        select("Save", message)

        assertEquals(1, savedMediaFiles.size)
        assertEquals(1, successHUDCount)
    }

    // MARK: - Auxiliary

    private fun receivedTextMessage(
        input: String = "Hola",
        output: String = "Hello",
        pair: LanguagePair = LanguagePair("es", "en"),
    ): Message = textMessage("m1", from = "a", input = input, output = output, pair = pair)

    private fun ownTextMessage(
        input: String = "Hello",
        output: String = "Hola",
    ): Message = textMessage("m2", from = "me", input = input, output = output, pair = LanguagePair("en", "es"))

    private fun select(
        title: String,
        message: Message,
    ) {
        val action = service.menuForMessage(message)?.first { it.title == title }
        action?.onSelect?.invoke()
    }

    private fun textMessage(
        id: String,
        from: String,
        input: String,
        output: String,
        pair: LanguagePair,
    ): Message =
        SessionTestEnvironment
            .message(id = id, fromAccountID = from, sentDate = Date())
            .copy(
                translations = listOf(Translation(TranslationInput(input), output, pair)),
                translationReferences = listOf(TranslationReference(pair, TranslationReference.Type.Archived("hash-$id"))),
            )

    private fun mediaMessage(): Message {
        val file = MediaFile("media/image.jpeg", "image", MediaFileExtension.Image(ImageFileExtension.JPEG))
        return SessionTestEnvironment
            .message(id = "m3", fromAccountID = "a", sentDate = Date())
            .copy(
                contentType = HostedContentType.Media("image", file.fileExtension),
                richContent = RichMessageContent.Media(file),
            )
    }
}
