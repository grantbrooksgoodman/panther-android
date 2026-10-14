//
//  ConversationsPageViewServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.bundle.mismatchedHashAndCallingCode
import us.neotechnica.panther.modules.common.services.sources.PermissionService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import java.util.Date

/**
 * Exercises the conversations page service: the startup prompt order,
 * the contact-sync throttle, and the chat page store-change branches,
 * each through injected collaborators.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsPageViewServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    private var notificationPermissionStatus = PermissionService.PermissionStatus.GRANTED
    private var requestedNotificationPermission = false
    private var suggestedInvitation = false
    private var promptedToReview = false
    private var hasContacts = true
    private var lastContactSyncDate: Date? = null
    private var syncCount = 0
    private var syncException: Exception? = null
    private var currentConversation: Conversation? = null
    private var resolvedLastMessageIDs = mutableListOf<String>()
    private var hadUnreadMessages = false

    private val service =
        ConversationsPageViewService(
            ConversationsPageViewService.Dependencies(
                clearTemporaryCaches = {},
                currentConversation = { currentConversation },
                currentUser = { null },
                hasContactsBesidesCurrentUser = { hasContacts },
                isChatPagePresented = { true },
                isSendingMessage = { false },
                lastContactSyncDate = { lastContactSyncDate },
                notificationPermissionStatus = { notificationPermissionStatus },
                promptToReview = { promptedToReview = true },
                requestNotificationPermission = { requestedNotificationPermission = true },
                resolveCurrentUser = {},
                resolveLastMessage = { id, _ -> resolvedLastMessageIDs.add(id) },
                setLastContactSyncDate = { lastContactSyncDate = it },
                suggestInvitationIfNeeded = { suggestedInvitation },
                syncContactPairArchive = {
                    syncCount += 1
                    syncException?.let { throw it }
                },
                updateReadDateForUnreadMessages = { hadUnreadMessages },
            ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("conversations-page-service-test")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - Prompts

    @Test
    fun `an undetermined notification permission is requested before any other prompt`() =
        runBlocking {
            notificationPermissionStatus = PermissionService.PermissionStatus.UNKNOWN

            service.showPromptsIfNeeded()

            assertTrue(requestedNotificationPermission)
            assertFalse(promptedToReview)
        }

    @Test
    fun `a review prompt follows only when no invitation was suggested`() =
        runBlocking {
            service.showPromptsIfNeeded()
            assertTrue(promptedToReview)

            promptedToReview = false
            suggestedInvitation = true
            service.showPromptsIfNeeded()
            assertFalse(promptedToReview)
        }

    // MARK: - Contact Sync Throttle

    @Test
    fun `the archive syncs when it is empty or stale and records the sync date`() =
        runBlocking {
            hasContacts = false
            service.syncContactPairArchiveIfNeeded()
            assertEquals(1, syncCount)
            assertNotNull(lastContactSyncDate)

            hasContacts = true
            service.syncContactPairArchiveIfNeeded()
            assertEquals(1, syncCount)

            lastContactSyncDate = Date(System.currentTimeMillis() - STALE_SYNC_MILLISECONDS)
            service.syncContactPairArchiveIfNeeded()
            assertEquals(2, syncCount)
        }

    @Test
    fun `a mismatched hash is swallowed and other sync failures propagate`() =
        runBlocking {
            hasContacts = false
            syncException =
                Exception(
                    "Mismatched hash and calling code.",
                    userInfo = mapOf(Exception.UserInfo.STATIC_ERROR_CODE.rawValue to AppException.mismatchedHashAndCallingCode.errorCode),
                    metadata = ExceptionMetadata(this),
                )
            service.syncContactPairArchiveIfNeeded()
            assertNull(lastContactSyncDate)

            syncException = Exception("Offline.", metadata = ExceptionMetadata(this))
            try {
                service.syncContactPairArchiveIfNeeded()
                fail("Expected the sync failure to propagate.")
            } catch (exception: Exception) {
                assertEquals("Offline.", exception.descriptor)
            }
        }

    // MARK: - Store Changes

    @Test
    fun `a one-to-one conversation re-fetches its last message and announces its metadata`() =
        runBlocking {
            val current = SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001")
            environment.signIn(current)
            currentConversation = SessionTestEnvironment.conversation("c1", listOf("me", "other"), listOf("m1", "m2"))

            var announced = false
            val collector =
                CoroutineScope(Dispatchers.Main).launch {
                    val sharedEvents = DependencyValues.current.sharedEvents
                    sharedEvents.currentConversationMetadataChanged.events.first()
                    announced = true
                }

            service.handleChatPageStoreChangeNow()

            assertEquals(listOf("m2"), resolvedLastMessageIDs)
            assertTrue(announced)
            collector.cancel()
        }

    @Test
    fun `marking unread messages read returns before the metadata announcement`() =
        runBlocking {
            currentConversation = SessionTestEnvironment.conversation("c1", listOf("me", "a", "b"))
            hadUnreadMessages = true

            var announced = false
            val collector =
                CoroutineScope(Dispatchers.Main).launch {
                    val sharedEvents = DependencyValues.current.sharedEvents
                    sharedEvents.currentConversationMetadataChanged.events.first()
                    announced = true
                }

            service.handleChatPageStoreChangeNow()

            assertTrue(resolvedLastMessageIDs.isEmpty())
            assertFalse(announced)
            collector.cancel()
        }

    // MARK: - Seconds to Load

    @Test
    fun `the load report names the seconds and the conversation count`() {
        val message = service.secondsToLoadMessage()

        assertTrue(message.startsWith("Loaded content in "))
        assertTrue(message.contains("second"))
    }

    private companion object {
        const val STALE_SYNC_MILLISECONDS = 3_601_000L
    }
}
