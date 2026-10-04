//
//  ChatInfoPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import us.neotechnica.panther.modules.content.user.services.ChatInfoPageViewService
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.entity.extensions.empty

class ChatInfoPageReducerTest {
    // MARK: - Setup

    private val reducer = ChatInfoPageReducer()

    private fun participant(name: String): ChatParticipant =
        ChatParticipant(
            displayName = name,
            contactPair =
                ContactPair(
                    contact = Contact(id = name, firstName = name, lastName = "", phoneNumbers = emptyList(), imageData = null),
                    numberPairs = emptyList(),
                ),
        )

    private fun conversationRequiringConsent(): Conversation =
        Conversation.empty.copy(metadata = Conversation.empty.metadata.copyWith(requiresConsentFromInitiator = "initiator"))

    // MARK: - Remove-User Swipe Gate

    @Test
    fun `remove swipe is shown for a group of more than two participants without pending consent`() {
        val state =
            ChatInfoPageReducer.State(
                conversation = Conversation.empty,
                visibleParticipants = listOf(participant("A"), participant("B")),
            )
        assertTrue(state.showsRemoveUserSwipeAction)
    }

    @Test
    fun `remove swipe is hidden when fewer than two participants are visible`() {
        val state =
            ChatInfoPageReducer.State(
                conversation = Conversation.empty,
                visibleParticipants = listOf(participant("A")),
            )
        assertFalse(state.showsRemoveUserSwipeAction)
    }

    @Test
    fun `remove swipe is hidden while the conversation awaits the initiator's consent`() {
        val state =
            ChatInfoPageReducer.State(
                conversation = conversationRequiringConsent(),
                visibleParticipants = listOf(participant("A"), participant("B")),
            )
        assertFalse(state.showsRemoveUserSwipeAction)
    }

    // MARK: - Visible Participants Increment

    @Test
    fun `increment is one for an expanded list without pending consent`() {
        val state =
            ChatInfoPageReducer.State(
                conversation = Conversation.empty,
                visibleParticipants = listOf(participant("A"), participant("B")),
            )
        assertEquals(1, state.visibleParticipantsIncrement)
    }

    @Test
    fun `increment is zero when the list is collapsed`() {
        val state = ChatInfoPageReducer.State(conversation = Conversation.empty, visibleParticipants = emptyList())
        assertEquals(0, state.visibleParticipantsIncrement)
    }

    @Test
    fun `increment is zero while the conversation awaits the initiator's consent`() {
        val state =
            ChatInfoPageReducer.State(
                conversation = conversationRequiringConsent(),
                visibleParticipants = listOf(participant("A")),
            )
        assertEquals(0, state.visibleParticipantsIncrement)
    }

    // MARK: - Picker Dismiss Transitions

    @Test
    fun `camera picker dismissal re-enables the change-metadata button and clears the request`() {
        val result =
            reducer.reduce(
                ChatInfoPageReducer.State(
                    isChangeMetadataButtonEnabled = false,
                    photoCaptureRequest = ChatInfoPageReducer.PhotoCaptureSource.CAMERA,
                ),
                ChatInfoPageReducer.Action.CameraPickerDismissed(null),
            )
        assertTrue(result.state.isChangeMetadataButtonEnabled)
        assertNull(result.state.photoCaptureRequest)
    }

    @Test
    fun `choosing the camera requests a camera capture`() {
        val result =
            reducer.reduce(
                ChatInfoPageReducer.State(),
                ChatInfoPageReducer.Action.ChangeMetadataActionSheetDismissed(
                    ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromCamera,
                ),
            )
        assertEquals(ChatInfoPageReducer.PhotoCaptureSource.CAMERA, result.state.photoCaptureRequest)
    }

    @Test
    fun `choosing the library requests a library capture`() {
        val result =
            reducer.reduce(
                ChatInfoPageReducer.State(),
                ChatInfoPageReducer.Action.ChangeMetadataActionSheetDismissed(
                    ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromLibrary,
                ),
            )
        assertEquals(ChatInfoPageReducer.PhotoCaptureSource.LIBRARY, result.state.photoCaptureRequest)
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
}
