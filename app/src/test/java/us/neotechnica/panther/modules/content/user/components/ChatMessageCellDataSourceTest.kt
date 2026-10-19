//
//  ChatMessageCellDataSourceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.AlternateMessageService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.message.models.ReadReceipt
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.io.File
import java.util.Date
import java.util.Locale

/**
 * Exercises the chat cell's bottom and top labels: the failed, alternate
 * footer, delivered, and read forms, and the sender name rules.
 */
class ChatMessageCellDataSourceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val defaultLocale = Locale.getDefault()

    @Before
    fun setUp() {
        environment.install("chat-message-cell-data-source-test")
        environment.signIn(SessionTestEnvironment.user(id = "me"))
        LocalizedStringResolver.initializeForTesting { source ->
            val root = if (source == LocalizationSource.APP) "src/main/assets" else "../subsystem/src/main/assets"
            val json = Json.parseToJsonElement(File(root, source.assetName).readText()).jsonObject
            json.mapValues { (_, languages) ->
                languages.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            }
        }
        RuntimeStorage.languageCodeDictionary = mapOf("en" to "English", "es" to "Spanish (Español)")
        CoreUtilities.setLanguageCode("en")
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        RuntimeStorage.languageCodeDictionary = null
        LocalizedStringResolver.clearCache()
        AlternateMessageService.restoreAllAlternateTextMessageIDs()
    }

    // MARK: - Bottom Label

    @Test
    fun `a failed outbox message reads Not Delivered`() {
        val row = row(message("outbox-1", from = "me"), isFailed = true)

        assertEquals("Not Delivered", cellBottomLabelAttributedText(row)?.text)
    }

    @Test
    fun `a pending outbox message and a mock show no label`() {
        assertNull(cellBottomLabelAttributedText(row(message("outbox-1", from = "me"))))
    }

    @Test
    fun `a received message showing its original names the original language`() {
        val message = message("m1", from = "a", pair = LanguagePair("es", "en"))
        AlternateMessageService.toggle(AlternateMessageService.AlternateMessageType.ALTERNATE_TEXT, message)

        val label = cellBottomLabelAttributedText(row(message))

        assertEquals("Spanish original", label?.text)
    }

    @Test
    fun `an own message showing its translation names the translated language`() {
        val message = message("m1", from = "me", pair = LanguagePair("en", "es"))
        AlternateMessageService.toggle(AlternateMessageService.AlternateMessageType.ALTERNATE_TEXT, message)

        val label = cellBottomLabelAttributedText(row(message, isLastConfirmedOwnMessage = true, isLastMessage = true))

        assertEquals("Spanish translation", label?.text)
    }

    @Test
    fun `the last confirmed own message in a one-to-one chat reads Delivered or Read`() {
        val delivered = cellBottomLabelAttributedText(row(message("m1", from = "me"), isLastConfirmedOwnMessage = true, isLastMessage = true))
        assertEquals("Delivered", delivered?.text)

        val readMessage = message("m2", from = "me").copy(readReceipts = listOf(ReadReceipt(userID = "a", readDate = Date())))
        val read = cellBottomLabelAttributedText(row(readMessage, isLastConfirmedOwnMessage = true, isLastMessage = true))
        assertNotNull(read)
        assertTrue(read!!.text, read.text.startsWith("Read "))
    }

    @Test
    fun `group chats and earlier own messages show no delivery status`() {
        assertNull(cellBottomLabelAttributedText(row(message("m1", from = "me"), isLastConfirmedOwnMessage = true, isGroup = true, isLastMessage = true)))
        assertNull(cellBottomLabelAttributedText(row(message("m1", from = "me"))))
    }

    @Test
    fun `an own message followed by an incoming message shows no status unless it has reactions`() {
        val message = message("m1", from = "me")
        assertNull(cellBottomLabelAttributedText(row(message, isLastConfirmedOwnMessage = true)))

        val reacted = row(message, isLastConfirmedOwnMessage = true, reactions = listOf(Reaction(userID = "a", style = Reaction.Style.LOVE)))
        assertEquals("Delivered", cellBottomLabelAttributedText(reacted)?.text)
    }

    // MARK: - Top Label

    @Test
    fun `the sender name shows only for received group messages with a name`() {
        val received = message("m1", from = "a")
        assertEquals("   Ada", messageTopLabelAttributedText(row(received, isGroup = true, senderName = "Ada"))?.text)
        assertNull(messageTopLabelAttributedText(row(received, isGroup = true)))
        assertNull(messageTopLabelAttributedText(row(received, senderName = "Ada")))
        assertNull(messageTopLabelAttributedText(row(message("m2", from = "me"), isGroup = true, senderName = "Me")))
    }

    // MARK: - Auxiliary

    private fun message(
        id: String,
        from: String,
        pair: LanguagePair = LanguagePair("en", "en"),
    ): Message =
        SessionTestEnvironment
            .message(id = id, fromAccountID = from, sentDate = Date())
            .copy(translations = listOf(Translation(TranslationInput("Hola"), "Hello", pair)))

    private fun row(
        message: Message,
        isFailed: Boolean = false,
        isGroup: Boolean = false,
        isLastConfirmedOwnMessage: Boolean = false,
        isLastMessage: Boolean = false,
        senderName: String? = null,
        reactions: List<Reaction> = emptyList(),
    ): ChatMessageRowData =
        ChatMessageRowData(
            message = message,
            previousMessage = null,
            isLastConfirmedOwnMessage = isLastConfirmedOwnMessage,
            isGroup = isGroup,
            isFailed = isFailed,
            isLastMessage = isLastMessage,
            senderName = senderName,
            reactions = reactions,
        )
}
