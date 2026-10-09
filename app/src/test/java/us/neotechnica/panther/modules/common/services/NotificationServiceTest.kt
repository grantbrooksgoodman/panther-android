//
//  NotificationServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.modules.common.extensions.stalePushToken
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.util.Date

class NotificationServiceTest {
    // MARK: - Body

    @Test
    fun `text body uses the translation into the recipient's language`() {
        val message =
            textMessage(
                Translation(TranslationInput("Hello"), "Hola", LanguagePair("en", "es")),
                Translation(TranslationInput("Hello"), "Bonjour", LanguagePair("en", "fr")),
            )

        assertEquals("Bonjour", NotificationService.notificationBody(message, user(languageCode = "fr")))
    }

    @Test
    fun `text body falls back to the input of the translation from the recipient's language`() {
        val message = textMessage(Translation(TranslationInput("Bonjour"), "Hello", LanguagePair("fr", "en")))

        assertEquals("Bonjour", NotificationService.notificationBody(message, user(languageCode = "fr")))
    }

    @Test
    fun `text body is sanitized and absent without a matching translation`() {
        val sanitized = textMessage(Translation(TranslationInput("Hi"), "Hola ⌘Ana⌘", LanguagePair("en", "es")))
        assertEquals("Hola Ana", NotificationService.notificationBody(sanitized, user(languageCode = "es")))

        val unmatched = textMessage(Translation(TranslationInput("Hi"), "Hallo", LanguagePair("en", "de")))
        assertNull(NotificationService.notificationBody(unmatched, user(languageCode = "ja")))
    }

    // MARK: - Delivery Failures

    @Test
    fun `an unregistered token with a not-found status is stale`() {
        val exception =
            NotificationService.sendNotificationException(
                "{\"error\":{\"status\":\"NOT_FOUND\",\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}",
                responseCode = 404,
                pushToken = "token",
            )

        assertTrue(exception.isEqual(AppException.stalePushToken))
        assertFalse(exception.isReportable)
        assertEquals("token", exception.userInfo?.get("PushToken"))
    }

    @Test
    fun `an unregistered token with another status is a non-reportable failure`() {
        val exception =
            NotificationService.sendNotificationException(
                "UNREGISTERED",
                responseCode = 400,
                pushToken = "token",
            )

        assertFalse(exception.isEqual(AppException.stalePushToken))
        assertFalse(exception.isReportable)
        assertEquals("Failed to decode URL response or status did not indicate success.", exception.descriptor)
        assertEquals(400, exception.userInfo?.get("URLResponseCode"))
    }

    @Test
    fun `any other failure is reportable`() {
        val exception =
            NotificationService.sendNotificationException(
                "INTERNAL",
                responseCode = 500,
                pushToken = "token",
            )

        assertTrue(exception.isReportable)
        assertEquals("INTERNAL", exception.userInfo?.get("ResponseBody"))
    }

    // MARK: - Auxiliary

    private fun textMessage(vararg translations: Translation): Message =
        Message(
            id = "message",
            fromAccountID = "sender",
            contentType = HostedContentType.Text,
            richContent = null,
            translationReferences = null,
            translations = translations.toList(),
            readReceipts = null,
            sentDate = Date(0),
        )

    private fun user(languageCode: String): User =
        User(
            id = "recipient",
            aiEnhancedTranslationsEnabled = false,
            blockedUserIDs = null,
            conversationIDs = null,
            deviceID = "device",
            isPenPalsParticipant = false,
            languageCode = languageCode,
            messageRecipientConsentRequired = false,
            phoneNumber = PhoneNumber("5551234567"),
            previousLanguageCodes = null,
            pushTokens = null,
        )
}
