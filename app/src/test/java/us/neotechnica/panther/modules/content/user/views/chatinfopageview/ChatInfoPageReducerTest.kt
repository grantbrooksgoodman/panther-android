//
//  ChatInfoPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.NumberPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import us.neotechnica.panther.modules.content.user.services.ChatInfoPageViewService
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.navigation.ChatNavigatorState
import us.neotechnica.panther.navigation.ChatRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

/**
 * Exercises the chat info page reducer: the participant-count gates,
 * the appearance resets, the metadata change paths, the picker
 * transitions, and the metadata-changed notification.
 */
class ChatInfoPageReducerTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val reducer = ChatInfoPageReducer()

    @Before
    fun setUp() {
        environment.install("chat-info-page-reducer-test")
        environment.signIn(SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001"))
    }

    @After
    fun tearDown() {
        ConversationSessionService.setCurrentConversation(null)
        DependencyValues.current.navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))
    }

    private fun participant(name: String): ChatParticipant {
        val phoneNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = "5550000000",
                regionCode = "US",
                label = null,
                internalFormattedString = null,
            )
        return ChatParticipant(
            displayName = name,
            contactPair =
                ContactPair(
                    contact = Contact(id = name, firstName = name, lastName = "", phoneNumbers = listOf(phoneNumber), imageData = null),
                    numberPairs = listOf(NumberPair(phoneNumber = phoneNumber, userIDs = listOf("user-$name"))),
                ),
        )
    }

    private fun participants(count: Int): List<ChatParticipant> = (1..count).map { participant("P$it") }

    // Makes a conversation with the given number of other users current.
    private fun presentConversation(
        otherUserCount: Int,
        requiresConsent: Boolean = false,
    ): Conversation {
        val conversation =
            SessionTestEnvironment
                .conversation("c1", listOf("me") + (1..otherUserCount).map { "u$it" })
                .let { conversation ->
                    if (!requiresConsent) return@let conversation
                    conversation.copy(metadata = conversation.metadata.copyWith(requiresConsentFromInitiator = "me"))
                }
        ConversationSessionService.setCurrentConversation(conversation)
        return conversation
    }

    // MARK: - Gates

    @Test
    fun `the leave button is enabled only with more than two participants`() {
        presentConversation(otherUserCount = 3)
        assertFalse(ChatInfoPageReducer.State(chatParticipants = participants(2)).isLeaveConversationButtonEnabled)
        assertTrue(ChatInfoPageReducer.State(chatParticipants = participants(3)).isLeaveConversationButtonEnabled)
    }

    @Test
    fun `the leave button is disabled while a message is being sent`() {
        presentConversation(otherUserCount = 3)
        val state = ChatInfoPageReducer.State(chatParticipants = participants(3), isSendingMessage = true)
        assertFalse(state.isLeaveConversationButtonEnabled)
        assertFalse(state.isAddContactButtonEnabled)
    }

    @Test
    fun `the remove swipe is shown only with more than two visible participants`() {
        presentConversation(otherUserCount = 3)
        assertFalse(ChatInfoPageReducer.State(visibleParticipants = participants(2)).showsRemoveUserSwipeAction)
        assertTrue(ChatInfoPageReducer.State(visibleParticipants = participants(3)).showsRemoveUserSwipeAction)
    }

    @Test
    fun `the remove swipe is hidden while the conversation awaits the initiator's consent`() {
        presentConversation(otherUserCount = 3, requiresConsent = true)
        assertFalse(ChatInfoPageReducer.State(visibleParticipants = participants(3)).showsRemoveUserSwipeAction)
    }

    @Test
    fun `the add-contact row shows for an expanded list of fewer than ten participants`() {
        presentConversation(otherUserCount = 9)
        assertEquals(1, ChatInfoPageReducer.State(visibleParticipants = participants(9)).visibleParticipantsIncrement)
        assertEquals(0, ChatInfoPageReducer.State(visibleParticipants = participants(10)).visibleParticipantsIncrement)
        assertEquals(0, ChatInfoPageReducer.State(visibleParticipants = emptyList()).visibleParticipantsIncrement)
    }

    @Test
    fun `the add-contact row is hidden while the conversation awaits the initiator's consent`() {
        presentConversation(otherUserCount = 3, requiresConsent = true)
        assertEquals(0, ChatInfoPageReducer.State(visibleParticipants = participants(3)).visibleParticipantsIncrement)
    }

    @Test
    fun `a sole participant is the single chat participant`() {
        assertNull(ChatInfoPageReducer.State(chatParticipants = participants(2)).singleChatParticipant)
        assertEquals("P1", ChatInfoPageReducer.State(chatParticipants = participants(1)).singleChatParticipant?.displayName)
    }

    @Test
    fun `the chat info cell describes the participants`() {
        val state = ChatInfoPageReducer.State(chatParticipants = participants(2), visibleParticipants = emptyList())
        assertEquals("2 people", state.chatInfoCellTitleLabelText)
        assertEquals("P1, P2", state.chatInfoCellSubtitleLabelText)
        assertEquals("chevron.right.circle", state.chatInfoCellImageSystemName)
        assertEquals("chevron.down.circle", state.copy(visibleParticipants = participants(2)).chatInfoCellImageSystemName)
    }

    // MARK: - Appearance

    @Test
    fun `appearing resets the segment selection, the loading state, and the metadata button`() {
        presentConversation(otherUserCount = 3)
        val state =
            ChatInfoPageReducer.State(
                isChangeMetadataButtonEnabled = false,
                segmentedControlSelectionIndex = 1,
                viewState = ViewState.Loaded,
            )

        val result = reducer.reduce(state, ChatInfoPageReducer.Action.ViewAppeared("c1"))

        assertEquals("c1", result.state.conversationIDKey)
        assertEquals(0, result.state.segmentedControlSelectionIndex)
        assertEquals(ViewState.Loading, result.state.viewState)
        assertTrue(result.state.isChangeMetadataButtonEnabled)
        assertNotNull(result.effect)
    }

    @Test
    fun `appearing keeps the metadata button disabled while consent is required`() {
        presentConversation(otherUserCount = 3, requiresConsent = true)
        val result = reducer.reduce(ChatInfoPageReducer.State(), ChatInfoPageReducer.Action.ViewAppeared("c1"))
        assertFalse(result.state.isChangeMetadataButtonEnabled)
    }

    @Test
    fun `the first participants to return load the page and later ones only rebuild it`() {
        val loading = reducer.reduce(ChatInfoPageReducer.State(), ChatInfoPageReducer.Action.GetChatParticipantsReturned(participants(2)))
        assertEquals(ViewState.Loaded, loading.state.viewState)
        assertEquals(2, loading.state.visibleParticipants.size)

        val reloaded = reducer.reduce(loading.state, ChatInfoPageReducer.Action.GetChatParticipantsReturned(participants(3)))
        assertNotEquals(loading.state.viewID, reloaded.state.viewID)
        assertEquals(3, reloaded.state.chatParticipants.size)
    }

    @Test
    fun `tapping the chat info cell toggles the visible participants`() {
        val collapsed = ChatInfoPageReducer.State(chatParticipants = participants(2), visibleParticipants = emptyList())
        val expanded = reducer.reduce(collapsed, ChatInfoPageReducer.Action.ChatInfoCellTapped).state
        assertEquals(2, expanded.visibleParticipants.size)
        assertNotEquals(collapsed.chatInfoCellViewID, expanded.chatInfoCellViewID)
        val collapsedAgain = reducer.reduce(expanded, ChatInfoPageReducer.Action.ChatInfoCellTapped).state
        assertTrue(collapsedAgain.visibleParticipants.isEmpty())
    }

    // MARK: - Metadata Changes

    @Test
    fun `a compression failure toasts and re-enables the metadata button`() {
        val result =
            reducer.reduce(
                ChatInfoPageReducer.State(isChangeMetadataButtonEnabled = false),
                ChatInfoPageReducer.Action.UpdateMetadataFailed(Exception("Failed to compress image.", metadata = ExceptionMetadata(this))),
            )
        assertTrue(result.state.isChangeMetadataButtonEnabled)
    }

    @Test
    fun `canceling the change-metadata sheet re-enables the button`() {
        val result =
            reducer.reduce(
                ChatInfoPageReducer.State(isChangeMetadataButtonEnabled = false),
                ChatInfoPageReducer.Action.ChangeMetadataActionSheetDismissed(null),
            )
        assertTrue(result.state.isChangeMetadataButtonEnabled)
    }

    @Test
    fun `a name change without a conversation re-enables the button`() {
        val result =
            reducer.reduce(
                ChatInfoPageReducer.State(isChangeMetadataButtonEnabled = false),
                ChatInfoPageReducer.Action.ChangeMetadataActionSheetDismissed(
                    ChatInfoPageViewService.MetadataChangeType.Name(Conversation.empty.metadata.copyWith(name = "Team")),
                ),
            )
        assertTrue(result.state.isChangeMetadataButtonEnabled)
    }

    @Test
    fun `a name change with a conversation starts the update`() {
        presentConversation(otherUserCount = 3)
        val result =
            reducer.reduce(
                ChatInfoPageReducer.State(isChangeMetadataButtonEnabled = false),
                ChatInfoPageReducer.Action.ChangeMetadataActionSheetDismissed(
                    ChatInfoPageViewService.MetadataChangeType.Name(Conversation.empty.metadata.copyWith(name = "Team")),
                ),
            )
        assertFalse(result.state.isChangeMetadataButtonEnabled)
        assertNotNull(result.effect)
    }

    @Test
    fun `a finished update notifies observers, re-enables the button, and rebuilds the page`() =
        runBlocking {
            val state = ChatInfoPageReducer.State(isChangeMetadataButtonEnabled = false)
            val received = CompletableDeferred<Unit>()
            val collector =
                launch(Dispatchers.Unconfined) {
                    DependencyValues
                        .current
                        .sharedEvents
                        .currentConversationMetadataChanged
                        .events
                        .first()
                    received.complete(Unit)
                }

            val result = reducer.reduce(state, ChatInfoPageReducer.Action.UpdateMetadataReturned)

            withTimeout(1_000) { received.await() }
            collector.cancel()
            assertTrue(result.state.isChangeMetadataButtonEnabled)
            assertNotEquals(state.viewID, result.state.viewID)
        }

    // MARK: - Pickers

    @Test
    fun `choosing the camera or the library presents the matching picker sheet`() {
        val navigation = DependencyValues.current.navigation

        val camera = ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromCamera
        reducer.reduce(ChatInfoPageReducer.State(), ChatInfoPageReducer.Action.ChangeMetadataActionSheetDismissed(camera))
        assertEquals(ChatNavigatorState.SheetPaths.CameraPicker, navigation.state.value.chat.sheet)

        val library = ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromLibrary
        reducer.reduce(ChatInfoPageReducer.State(), ChatInfoPageReducer.Action.ChangeMetadataActionSheetDismissed(library))
        assertEquals(ChatNavigatorState.SheetPaths.PhotoPicker, navigation.state.value.chat.sheet)
    }

    @Test
    fun `a picker's dismissal clears the sheet and re-enables the metadata button`() {
        val navigation = DependencyValues.current.navigation
        navigation.navigate(Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPaths.CameraPicker)))

        val camera =
            reducer.reduce(
                ChatInfoPageReducer.State(isChangeMetadataButtonEnabled = false),
                ChatInfoPageReducer.Action.CameraPickerDismissed(null),
            )
        assertTrue(camera.state.isChangeMetadataButtonEnabled)
        assertNull(navigation.state.value.chat.sheet)

        navigation.navigate(Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPaths.PhotoPicker)))
        val photo =
            reducer.reduce(
                ChatInfoPageReducer.State(isChangeMetadataButtonEnabled = false),
                ChatInfoPageReducer.Action.PhotoPickerDismissed(Exception("Failed to get image data.", metadata = ExceptionMetadata(this))),
            )
        assertTrue(photo.state.isChangeMetadataButtonEnabled)
        assertNull(navigation.state.value.chat.sheet)
    }

    @Test
    fun `resolved strings rebuild the segmented control`() {
        val state = ChatInfoPageReducer.State()
        val result = reducer.reduce(state, ChatInfoPageReducer.Action.ResolveReturned(state.strings))
        assertNotEquals(state.segmentedControlViewID, result.state.segmentedControlViewID)
    }
}
