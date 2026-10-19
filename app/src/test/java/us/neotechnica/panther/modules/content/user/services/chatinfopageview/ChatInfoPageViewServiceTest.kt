//
//  ChatInfoPageViewServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatinfopageview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.navigators.ChatRoute
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.ImageFileExtension
import us.neotechnica.panther.modules.common.models.MediaFile
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import us.neotechnica.panther.modules.content.user.models.MediaItemViewData
import us.neotechnica.panther.modules.networking.sources.conversation.models.Activity
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.conversation.models.copyWith
import us.neotechnica.panther.modules.networking.sources.conversation.remotelyupdatable.ConversationUpdatableKey
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.navigation.RootNavigationService.Route

/**
 * Exercises the chat info page service: participant resolution and
 * ordering, leaving and removal through the confirmation alert, the
 * metadata update's activity, and the media preview state – each
 * through injected collaborators.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatInfoPageViewServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    private val me = SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001")
    private val userA = SessionTestEnvironment.user(id = "a", nationalNumber = "5550000002")
    private val userB = SessionTestEnvironment.user(id = "b", nationalNumber = "5550000003")

    private var currentConversation: Conversation? = null
    private var confirms = true
    private var contactNames = mapOf<String, String>()
    private val steps = mutableListOf<String>()
    private val routes = mutableListOf<Route>()
    private var resolveCount = 0
    private var updatedData: Map<ConversationUpdatableKey, Any>? = null

    private val service =
        ChatInfoPageViewService(
            ChatInfoPageViewService.Dependencies(
                addEffectUponIsSendingMessage = { steps.add("effect") },
                contactPair = { user -> contactNames[user.id]?.let { ContactPair.withUser(user, name = it) } },
                currentConversation = { currentConversation },
                currentUserID = { "me" },
                dismissSheets = { steps.add("dismissSheets") },
                navigate = { routes.add(it) },
                presentConfirmationAlert = { _, _ -> confirms },
                removeConversation = { steps.add("removeConversation:$it") },
                removeFromConversation = { userID, _ -> steps.add("removeFromConversation:$userID") },
                resignFirstResponders = { steps.add("resignFirstResponders") },
                resolveUsers = {
                    resolveCount += 1
                    SessionStore.upsertUsers(setOf(userA, userB))
                },
                sendChatInfoPageLoadingStateUpdated = { steps.add("loading") },
                sendCurrentConversationActivityChanged = { steps.add("activityChanged") },
                sendCurrentConversationMetadataChanged = { steps.add("metadataChanged") },
                storeShouldNotifyOfConversationAvailability = { steps.add("shouldNotify:$it") },
                updateValues = { conversation, data ->
                    updatedData = data
                    conversation
                },
            ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("chat-info-page-view-service-test")
        environment.signIn(me)
        currentConversation = SessionTestEnvironment.conversation("c1", listOf("me", "a", "b"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun participant(user: User): ChatParticipant = ChatParticipant(displayName = user.id, contactPair = ContactPair.withUser(user))

    // MARK: - Get Chat Participants

    @Test
    fun `unresolved users are resolved once and the participants returned`() =
        runBlocking {
            val participants = service.getChatParticipants()

            assertEquals(1, resolveCount)
            assertEquals(listOf("a", "b"), participants.map { it.firstUser?.id })
        }

    @Test
    fun `participants sort letter-prefixed names first, then the rest`() =
        runBlocking {
            SessionStore.upsertUsers(setOf(userA, userB))
            contactNames = mapOf("a" to "amy", "b" to "Zed")

            val names = service.getChatParticipants().map { it.displayName }

            assertEquals(listOf("Zed", "amy"), names)
        }

    @Test
    fun `a number-named participant sorts after named ones`() =
        runBlocking {
            SessionStore.upsertUsers(setOf(userA, userB))
            contactNames = mapOf("b" to "Zed")

            val names = service.getChatParticipants().map { it.displayName }

            assertEquals("Zed", names.first())
            assertEquals(userA.phoneNumber.formattedString(), names.last())
        }

    @Test
    fun `resolved participants are cached until the cache is cleared`() =
        runBlocking {
            SessionStore.upsertUsers(setOf(userA, userB))
            contactNames = mapOf("a" to "amy", "b" to "Zed")
            service.getChatParticipants()

            contactNames = mapOf("a" to "Beth", "b" to "Zed")
            assertEquals(listOf("Zed", "amy"), service.getChatParticipants().map { it.displayName })

            service.clearCache()
            assertEquals(listOf("Beth", "Zed"), service.getChatParticipants().map { it.displayName })
        }

    @Test
    fun `no current conversation throws`() =
        runBlocking {
            currentConversation = null
            try {
                service.getChatParticipants()
                throw AssertionError("Expected an exception.")
            } catch (exception: us.neotechnica.panther.subsystem.modules.foundation.models.Exception) {
                assertEquals("No current conversation.", exception.descriptor)
            }
        }

    // MARK: - Leave Conversation

    @Test
    fun `leaving removes the current user, dismisses sheets, removes the conversation, and resets the stack`() {
        service.leaveConversationButtonTapped(currentConversation)

        assertEquals(
            listOf("loading", "shouldNotify:false", "removeFromConversation:me", "dismissSheets", "removeConversation:c1"),
            steps,
        )
        assertEquals(listOf<Route>(Route.UserContent(UserContentRoute.Stack(emptyList()))), routes)
    }

    @Test
    fun `declining the leave confirmation changes nothing`() {
        confirms = false
        service.leaveConversationButtonTapped(currentConversation)
        assertTrue(steps.isEmpty())
        assertTrue(routes.isEmpty())
    }

    // MARK: - Remove User

    @Test
    fun `removing a participant dismisses the chat sheet, removes them, and announces the activity`() {
        SessionStore.upsertUsers(setOf(userA))

        service.removeUserButtonTapped(participant(userA), currentConversation)

        assertEquals(listOf("loading", "removeFromConversation:a", "activityChanged"), steps)
        assertEquals(listOf<Route>(Route.Chat(ChatRoute.Sheet(null))), routes)
    }

    @Test
    fun `declining the removal confirmation changes nothing`() {
        SessionStore.upsertUsers(setOf(userA))
        confirms = false
        service.removeUserButtonTapped(participant(userA), currentConversation)
        assertTrue(steps.isEmpty())
    }

    // MARK: - Update Metadata

    @Test
    fun `updating metadata appends the activity and writes the new metadata`() =
        runBlocking {
            val conversation = checkNotNull(currentConversation)
            val newMetadata = conversation.metadata.copyWith(name = "Team")

            service.updateMetadata(conversation, Activity.Action.RenamedConversation("Team"), newMetadata)

            val data = checkNotNull(updatedData)
            val activities = data[ConversationUpdatableKey.ACTIVITIES] as List<*>
            assertEquals(1, activities.size)
            assertEquals(Activity.Action.RenamedConversation("Team"), (activities.single() as Activity).action)
            assertEquals(newMetadata, data[ConversationUpdatableKey.METADATA])
        }

    // MARK: - Media Preview

    @Test
    fun `tapping a media item presents the preview until it is dismissed`() {
        val file = MediaFile("media/image.jpeg", "image", MediaFileExtension.Image(ImageFileExtension.JPEG))
        val metadata = MediaItemViewData(file, "Image", "from you", "Today")

        service.mediaItemViewTapped(metadata, filePaths = listOf(file.relativePath), startingIndex = 0)

        assertTrue(service.isPreviewingMedia)
        val preview = service.mediaPreview.value
        assertNotNull(preview)
        assertEquals(listOf("media/image.jpeg"), preview?.filePaths)
        assertEquals(0, preview?.startingIndex)

        service.mediaPreviewDismissed()
        assertFalse(service.isPreviewingMedia)
        assertNull(service.mediaPreview.value)
    }

    // MARK: - View Lifecycle

    @Test
    fun `appearing resigns first responders and schedules the metadata notification`() {
        service.viewAppeared()
        assertEquals(listOf("resignFirstResponders", "effect"), steps)
    }
}
