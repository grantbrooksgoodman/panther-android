//
//  ConversationMediaItemMetadataTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.modules.common.models.DocumentFileExtension
import us.neotechnica.panther.modules.common.models.ImageFileExtension
import us.neotechnica.panther.modules.common.models.MediaFile
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.common.services.sources.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Activity
import us.neotechnica.panther.modules.networking.sources.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.message.models.RichMessageContent
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.foundation.extensions.formattedShortString
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.io.File
import java.util.Date
import java.util.Locale

/**
 * Exercises the conversation's shared-media metadata: which messages
 * contribute, their order, their labels, and the offset from the
 * current user's addition date.
 */
class ConversationMediaItemMetadataTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val defaultLocale = Locale.getDefault()

    private val me = SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001")
    private val userA = SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002")

    private val now = Date()
    private val anHourAgo = Date(now.time - 3_600_000L)
    private val twoHoursAgo = Date(now.time - 7_200_000L)

    @Before
    fun setUp() {
        environment.install("conversation-media-item-metadata-test")
        environment.signIn(me)
        SessionStore.upsertUsers(setOf(userA))
        CommonPropertyLists.initializeForTesting(callingCodes = mapOf("US" to "1"), lookupTables = mapOf("10" to listOf("1")))
        RegionDetailService.clearCache()

        LocalizedStringResolver.initializeForTesting { source ->
            val root = if (source == LocalizationSource.APP) "src/main/assets" else "../subsystem/src/main/assets"
            val json = Json.parseToJsonElement(File(root, source.assetName).readText()).jsonObject
            json.mapValues { (_, languages) ->
                languages.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            }
        }
        CoreUtilities.setLanguageCode("en")
        Locale.setDefault(Locale.US)

        SessionStore.upsertMessages(
            setOf(
                mediaMessage("m1", from = "me", sentDate = twoHoursAgo, file = image("one")),
                mediaMessage("m2", from = "a", sentDate = now, file = document("two")),
                SessionTestEnvironment.message("m3", fromAccountID = "a", sentDate = anHourAgo),
                mediaMessage("m4", from = "ghost", sentDate = now, file = image("four")),
            ),
        )
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
        LocalizedStringResolver.clearCache()
    }

    private fun image(name: String): MediaFile = MediaFile("media/$name.jpeg", name, MediaFileExtension.Image(ImageFileExtension.JPEG))

    private fun document(name: String): MediaFile =
        MediaFile("media/$name.pdf", name, MediaFileExtension.Document(DocumentFileExtension.Pdf))

    private fun mediaMessage(
        id: String,
        from: String,
        sentDate: Date,
        file: MediaFile,
    ): Message =
        SessionTestEnvironment
            .message(id, fromAccountID = from, sentDate = sentDate)
            .copy(
                contentType = HostedContentType.Media(id, file.fileExtension),
                richContent = RichMessageContent.Media(file),
            )

    private fun conversation() = SessionTestEnvironment.conversation("c1", listOf("me", "a"), messageIDs = listOf("m1", "m2", "m3", "m4"))

    // MARK: - Tests

    @Test
    fun `media messages with a resolvable sender are listed newest first`() {
        val metadata = conversation().mediaItemMetadata

        assertEquals(listOf("two", "one"), metadata.map { it.file.name })
    }

    @Test
    fun `labels name the type, the sender, and the short timestamp`() {
        val metadata = conversation().mediaItemMetadata

        val document = metadata[0]
        assertEquals("${LocalizedStringKey.File.localized()} • pdf", document.mediaTypeLabelText)
        assertEquals(LocalizedStringKey.FromUser.localized().replace("⌘", userA.displayName), document.senderLabelText)
        assertEquals(now.formattedShortString, document.timestampLabelText)

        val image = metadata[1]
        assertEquals(LocalizedStringKey.Image.localized(), image.mediaTypeLabelText)
        assertEquals(LocalizedStringKey.FromYou.localized().replaceFirstChar { it.lowercase() }, image.senderLabelText)
        assertEquals(twoHoursAgo.formattedShortString, image.timestampLabelText)
    }

    @Test
    fun `messages before the current user's addition are excluded`() {
        val added = Activity(Activity.Action.AddedToConversation("me"), date = anHourAgo, userID = "a")
        val conversation = conversation().copy(activities = listOf(added))

        val metadata = conversation.withMessagesOffsetFromCurrentUserAdditionDate.mediaItemMetadata

        assertEquals(listOf("two"), metadata.map { it.file.name })
    }
}
