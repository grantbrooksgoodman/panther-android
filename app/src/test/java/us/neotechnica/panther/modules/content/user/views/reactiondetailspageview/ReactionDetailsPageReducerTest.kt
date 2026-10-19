//
//  ReactionDetailsPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.reactiondetailspageview

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.modules.common.services.sources.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.networking.sources.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.sources.conversation.models.ReactionMetadata
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.foundation.extensions.capitalized
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.io.File
import java.util.Locale

/**
 * Exercises the reaction details page reducer: the list rows built
 * from the message's reactions, their ordering, and the page title.
 */
class ReactionDetailsPageReducerTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val reducer = ReactionDetailsPageReducer()
    private val defaultLocale = Locale.getDefault()

    private val me = SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001")
    private val userA = SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002")
    private val userB = SessionTestEnvironment.user(id = "b", nationalNumber = "5550000003")

    @Before
    fun setUp() {
        environment.install("reaction-details-page-reducer-test")
        environment.signIn(me)
        SessionStore.upsertUsers(setOf(userA, userB))
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

        val conversation =
            SessionTestEnvironment
                .conversation("c1", listOf("me", "a", "b"), messageIDs = listOf("m1"))
                .copy(
                    reactionMetadata =
                        listOf(
                            ReactionMetadata(
                                messageID = "m1",
                                reactions =
                                    listOf(
                                        Reaction(Reaction.Style.LIKE, userID = "b"),
                                        Reaction(Reaction.Style.LOVE, userID = "me"),
                                        Reaction(Reaction.Style.LIKE, userID = "a"),
                                        Reaction(Reaction.Style.LAUGH, userID = "ghost"),
                                    ),
                            ),
                        ),
                )
        SessionStore.upsertMessages(setOf(SessionTestEnvironment.message("m1", fromAccountID = "a", sentDate = java.util.Date())))
        ConversationSessionService.setCurrentConversation(conversation)
    }

    @After
    fun tearDown() {
        ConversationSessionService.setCurrentConversation(null)
        Locale.setDefault(defaultLocale)
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
        LocalizedStringResolver.clearCache()
    }

    // MARK: - List Items

    @Test
    fun `rows group reactions by style in display order with sorted names`() {
        val rows = ReactionDetailsPageReducer.State(messageID = "m1").listItems

        assertEquals(2, rows.size)
        assertEquals(LocalizedStringKey.You.localized(), rows[0].innerText)
        assertEquals(listOf(userA.displayName, userB.displayName).sorted().joinToString("\n"), rows[1].innerText)
        assertTrue(rows.all { !it.isEnabled })
    }

    @Test
    fun `a message without reactions has no rows`() {
        assertTrue(ReactionDetailsPageReducer.State(messageID = "m9").listItems.isEmpty())
    }

    // MARK: - Title

    @Test
    fun `the title capitalizes the localized reaction details string in English`() {
        val expected =
            LocalizedStringKey.ReactionDetails
                .localized()
                .replace("…", "")
                .capitalized
        assertEquals(expected, ReactionDetailsPageReducer.State().navigationTitle)
    }

    @Test
    fun `capitalizing lowers interior capitals and keeps the separators`() {
        assertEquals("Reaction Details", "rEACTION dETAILS".capitalized)
        assertEquals("Reaction  Details", "reaction  DETAILS".capitalized)
    }

    // MARK: - Reduce

    @Test
    fun `updating the view identity regenerates it`() {
        val state = ReactionDetailsPageReducer.State(messageID = "m1")
        val result = reducer.reduce(state, ReactionDetailsPageReducer.Action.UpdateViewID)
        assertNotEquals(state.viewID, result.state.viewID)
        assertFalse(result.state.messageID.isEmpty())
    }
}
