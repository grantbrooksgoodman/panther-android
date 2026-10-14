//
//  UICacheInvalidationServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.services.sources.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewDataCache
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.notificationextension.NotificationExtensionConstants
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Exercises the notification name map: group conversations whose
 * title resolves are recorded, and those whose users are unresolved
 * are skipped.
 */
class UICacheInvalidationServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("ui-cache-invalidation-test")
        environment.signIn(SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001"))
        CommonPropertyLists.initializeForTesting(callingCodes = mapOf("US" to "1"), lookupTables = mapOf("10" to listOf("1")))
        RegionDetailService.clearCache()
        ConversationCellViewDataCache.clearCache()
    }

    @After
    fun tearDown() {
        ConversationCellViewDataCache.clearCache()
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
    }

    // MARK: - Name Map

    @Test
    fun `the name map records resolvable group titles and skips unresolved ones`() {
        SessionStore.upsertUsers(
            setOf(
                SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002"),
                SessionTestEnvironment.user(id = "b", nationalNumber = "5550000003"),
            ),
        )
        val named =
            named(SessionTestEnvironment.conversation("c1", listOf("me", "a", "b")), "Team")
        val unresolved = SessionTestEnvironment.conversation("c2", listOf("me", "x", "y"))
        SessionStore.upsertConversations(setOf(named, unresolved))

        UICacheInvalidationService.refreshNotificationExtensionNameMap()

        val json =
            JSONObject(
                Persistent.string(PersistentStorageKey(NotificationExtensionConstants.CONVERSATION_NAME_MAP_DEFAULTS_KEY_NAME)) ?: "{}",
            )
        assertEquals("Team", json.optString("c1"))
        assertFalse(json.has("c2"))
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
}
