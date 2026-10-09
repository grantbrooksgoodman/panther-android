//
//  ActivityDescriptionTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.conversation.models

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.extensions.UserDisplayNameCache
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.parity.FixtureJson
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.io.File
import java.util.Date

/**
 * Exercises the activity description templates: every action in
 * English and Spanish, the lowercased "you"/"someone" object forms,
 * the phone-number and user-identifier forms of a departure, and the
 * parity fixture for the departure activity.
 */
class ActivityDescriptionTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    private lateinit var me: User
    private lateinit var them: User

    @Before
    fun setUp() {
        environment.install("activity-description-test")

        LocalizedStringResolver.initializeForTesting { source ->
            val root = if (source == LocalizationSource.APP) "src/main/assets" else "../subsystem/src/main/assets"
            val json = Json.parseToJsonElement(File(root, source.assetName).readText()).jsonObject
            json.mapValues { (_, languages) ->
                languages.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            }
        }

        CoreUtilities.setLanguageCode("en")
        ActivityDescriptionCache.clearCache()
        UserDisplayNameCache.clearCache()

        me = SessionTestEnvironment.user(id = "me", nationalNumber = "5551234567")
        them = SessionTestEnvironment.user(id = "them", languageCode = "es", nationalNumber = "5559876543")

        environment.signIn(me)
        SessionStore.upsertUser(them)
    }

    @After
    fun tearDown() {
        CoreUtilities.setLanguageCode("en")
        ActivityDescriptionCache.clearCache()
        UserDisplayNameCache.clearCache()
    }

    // MARK: - Templates

    @Test
    fun `every action describes itself in English with the current user as You`() {
        val themName = them.displayName

        assertEquals("⌘You⌘ added ⌘$themName⌘ to the conversation.", describe(Activity.Action.AddedToConversation("them")))
        assertEquals("⌘You⌘ changed the group photo.", describe(Activity.Action.ChangedGroupPhoto))
        assertEquals(
            "⌘You⌘ left the conversation.",
            describe(Activity.Action.LeftConversation, userID = me.phoneNumber.compiledNumberString),
        )
        assertEquals(
            "⌘You⌘ removed ⌘$themName⌘ from the conversation.",
            describe(Activity.Action.RemovedFromConversation("them")),
        )
        assertEquals("⌘You⌘ removed the group photo.", describe(Activity.Action.RemovedGroupPhoto))
        assertEquals("⌘You⌘ removed the conversation name.", describe(Activity.Action.RemovedName))
        assertEquals("⌘You⌘ named the conversation ⌘“Team”⌘.", describe(Activity.Action.RenamedConversation("Team")))
    }

    @Test
    fun `the object of an action is lowercased when it is you or someone`() {
        val themName = them.displayName

        assertEquals(
            "⌘$themName⌘ added ⌘you⌘ to the conversation.",
            describe(Activity.Action.AddedToConversation("me"), userID = "them"),
        )

        assertEquals(
            "⌘Someone⌘ removed ⌘someone⌘ from the conversation.",
            describe(Activity.Action.RemovedFromConversation("unknown"), userID = "stranger"),
        )
    }

    @Test
    fun `templates resolve in the selected language`() {
        CoreUtilities.setLanguageCode("es")
        val themName = them.displayName

        assertEquals(
            "⌘$themName⌘ ha(s) añadido a ⌘tú⌘ a la conversación.",
            describe(Activity.Action.AddedToConversation("me"), userID = "them"),
        )

        assertEquals("⌘Tú⌘ ha(s) cambiado la foto del grupo.", describe(Activity.Action.ChangedGroupPhoto))
        assertEquals(
            "⌘Tú⌘ ha(s) denominado la conversación ⌘“Equipo”⌘.",
            describe(Activity.Action.RenamedConversation("Equipo")),
        )
    }

    // MARK: - Departure Forms

    @Test
    fun `a departure resolves its phone number, a legacy user identifier, and an unknown number`() {
        val themName = them.displayName

        assertEquals(
            "⌘$themName⌘ left the conversation.",
            describe(Activity.Action.LeftConversation, userID = them.phoneNumber.compiledNumberString),
        )

        // Backward compatibility: a departure recorded against a user identifier.
        assertEquals("⌘$themName⌘ left the conversation.", describe(Activity.Action.LeftConversation, userID = "them"))

        val unknownNumber = PhoneNumber("5550001111")
        assertEquals(
            "⌘${unknownNumber.formattedString()}⌘ left the conversation.",
            describe(Activity.Action.LeftConversation, userID = unknownNumber.compiledNumberString),
        )
    }

    @Test
    fun `a synthesized departure is attributed to the current user's phone number`() {
        val activity = checkNotNull(Activity.from(Activity.Action.LeftConversation))
        val fixture = FixtureJson.loadObject("activity_left.json")

        assertEquals(fixture["userID"], activity.userID)
        assertEquals(fixture["action"], activity.encoded["action"])

        val decoded = Activity.decode(fixture)
        assertEquals(Activity.Action.LeftConversation, decoded.action)
        assertEquals(fixture, decoded.encoded)
        assertFalse(decoded.description.contains("⁂"))
    }

    // MARK: - Auxiliary

    private fun describe(
        action: Activity.Action,
        userID: String = "me",
    ): String = Activity(action = action, date = Date(), userID = userID).description
}
