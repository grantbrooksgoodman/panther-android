//
//  ConversationCellViewDataTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.modules.common.services.sources.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Activity
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.foundation.extensions.formattedShortString
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exercises the conversation cell view data: title selection, the
 * activity subtitle, the hydration flag, and the short date vectors.
 */
class ConversationCellViewDataTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val defaultLocale = Locale.getDefault()

    @Before
    fun setUp() {
        environment.install("conversation-cell-view-data-test")
        environment.signIn(SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001"))
        CommonPropertyLists.initializeForTesting(callingCodes = mapOf("US" to "1"), lookupTables = mapOf("10" to listOf("1")))
        RegionDetailService.clearCache()
        ConversationCellViewDataCache.clearCache()

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
        ConversationCellViewDataCache.clearCache()
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
        LocalizedStringResolver.clearCache()
    }

    // MARK: - Title

    @Test
    fun `a named conversation uses its name and has no other user`() {
        SessionStore.upsertUsers(setOf(SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002")))
        val conversation =
            named(SessionTestEnvironment.conversation("c1", listOf("me", "a")), "Team")

        val data = ConversationCellViewData(conversation)

        assertNotNull(data)
        assertEquals("Team", data?.titleLabelText)
        assertNull(data?.otherUser)
    }

    @Test
    fun `an unnamed one-to-one conversation uses the other user's number and exposes the user`() {
        val other = SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002")
        SessionStore.upsertUsers(setOf(other))
        val conversation = SessionTestEnvironment.conversation("c1", listOf("me", "a"))

        val data = ConversationCellViewData(conversation)

        assertEquals(other.phoneNumber.formattedString(), data?.titleLabelText)
        assertEquals("a", data?.otherUser?.id)
    }

    @Test
    fun `an unnamed group appends the other participant count`() {
        SessionStore.upsertUsers(
            setOf(
                SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002"),
                SessionTestEnvironment.user(id = "b", nationalNumber = "5550000003"),
            ),
        )
        val conversation = SessionTestEnvironment.conversation("c1", listOf("me", "a", "b"))

        val data = ConversationCellViewData(conversation)

        assertTrue(data?.titleLabelText?.endsWith(" + 1") == true)
        assertNull(data?.otherUser)
    }

    @Test
    fun `unresolvable users yield no data`() {
        val conversation = SessionTestEnvironment.conversation("c1", listOf("me", "missing"))

        assertNull(ConversationCellViewData(conversation))
    }

    // MARK: - Subtitle

    @Test
    fun `the newest activity wins over an older message`() {
        SessionStore.upsertUsers(setOf(SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002")))
        val message = textMessage("-m1", from = "a", sentDate = Date(System.currentTimeMillis() - HOUR_MILLISECONDS))
        SessionStore.upsertMessages(setOf(message))
        val activity = Activity(action = Activity.Action.RenamedConversation("Crew"), date = Date(), userID = "a")
        val base = SessionTestEnvironment.conversation("c1", listOf("me", "a"), listOf("-m1"))
        val conversation = base.copy(activities = listOf(activity))

        val data = ConversationCellViewData(conversation)

        assertEquals(activity.description.sanitized, data?.subtitleLabelText)
        assertEquals(activity.date.formattedShortString, data?.dateLabelText)
    }

    @Test
    fun `a received text message shows its translated output and the unread indicator`() {
        SessionStore.upsertUsers(setOf(SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002")))
        val message = textMessage("-m1", from = "a", sentDate = Date())
        SessionStore.upsertMessages(setOf(message))
        val conversation = SessionTestEnvironment.conversation("c1", listOf("me", "a"), listOf("-m1"))

        val data = ConversationCellViewData(conversation)

        assertEquals("Hola", data?.subtitleLabelText)
        assertTrue(data?.isShowingUnreadIndicator == true)
        assertFalse(data?.isAwaitingMessageHydration == true)
    }

    @Test
    fun `a hydrated conversation without messages falls back to the placeholder`() {
        SessionStore.upsertUsers(setOf(SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002")))
        val conversation = SessionTestEnvironment.conversation("c1", listOf("me", "a"))

        val data = ConversationCellViewData(conversation)

        assertEquals(LocalizedStringKey.CannotDisplayMessage.localized(), data?.subtitleLabelText)
        assertEquals(Date(0).formattedShortString, data?.dateLabelText)
    }

    // MARK: - Hydration

    @Test
    fun `missing messages flag provisional data that is never cached`() {
        SessionStore.upsertUsers(setOf(SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002")))
        val conversation = SessionTestEnvironment.conversation("c1", listOf("me", "a"), listOf("-m1"))

        val provisional = ConversationCellViewData(conversation)
        assertTrue(provisional?.isAwaitingMessageHydration == true)
        assertEquals("", provisional?.subtitleLabelText)

        SessionStore.upsertMessages(setOf(textMessage("-m1", from = "a", sentDate = Date())))
        val hydrated = ConversationCellViewData(conversation)
        assertFalse(hydrated?.isAwaitingMessageHydration == true)
        assertEquals("Hola", hydrated?.subtitleLabelText)
    }

    // MARK: - Short Date

    @Test
    fun `today renders as a time in the device locale`() {
        val now = Date()

        Locale.setDefault(Locale.US)
        assertEquals(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(now), now.formattedShortString)
        assertTrue(now.formattedShortString.matches(Regex("""\d{1,2}:\d{2}[\s\u202F]?[AP]M""")))

        Locale.setDefault(Locale.GERMANY)
        assertTrue(now.formattedShortString.matches(Regex("""\d{2}:\d{2}""")))
    }

    @Test
    fun `yesterday, the past week, and older dates render as expected`() {
        val yesterday = Date(System.currentTimeMillis() - DAY_MILLISECONDS)
        assertEquals("Yesterday", yesterday.formattedShortString)

        val threeDaysAgo = Date(System.currentTimeMillis() - 3 * DAY_MILLISECONDS)
        assertEquals(SimpleDateFormat("EEEE", Locale.US).format(threeDaysAgo), threeDaysAgo.formattedShortString)

        val tenDaysAgo = Date(System.currentTimeMillis() - 10 * DAY_MILLISECONDS)
        assertEquals(DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).format(tenDaysAgo), tenDaysAgo.formattedShortString)
    }

    // MARK: - Auxiliary

    private fun named(
        conversation: Conversation,
        name: String,
    ): Conversation =
        conversation.copy(
            metadata =
                ConversationMetadata(
                    name = name,
                    imageData = conversation.metadata.imageData,
                    isPenPalsConversation = conversation.metadata.isPenPalsConversation,
                    lastModifiedDate = conversation.metadata.lastModifiedDate,
                    messageRecipientConsentAcknowledgementData = conversation.metadata.messageRecipientConsentAcknowledgementData,
                    penPalsSharingData = conversation.metadata.penPalsSharingData,
                    requiresConsentFromInitiator = conversation.metadata.requiresConsentFromInitiator,
                ),
        )

    private fun textMessage(
        id: String,
        from: String,
        sentDate: Date,
    ): Message =
        SessionTestEnvironment
            .message(id = id, fromAccountID = from, sentDate = sentDate)
            .copy(translations = listOf(Translation(TranslationInput("Hello"), "Hola", LanguagePair("en", "es"))))

    private companion object {
        const val DAY_MILLISECONDS = 24L * 60 * 60 * 1000
        const val HOUR_MILLISECONDS = 60L * 60 * 1000
    }
}
