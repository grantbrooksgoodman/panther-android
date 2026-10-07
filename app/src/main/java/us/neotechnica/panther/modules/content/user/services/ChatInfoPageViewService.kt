//
//  ChatInfoPageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.chatInfoPageLoadingStateUpdated
import us.neotechnica.panther.bundle.currentConversationActivityChanged
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.bundle.shouldNotifyOfConversationAvailability
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import us.neotechnica.panther.modules.content.user.models.MessageDeliveryServiceEffectID
import us.neotechnica.panther.modules.networking.conversation.models.ActivityAction
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.entity.services.ActivitySessionService
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.navigation.ChatRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

/**
 * Handles the chat info page's user interactions.
 *
 * Loads the current conversation's participants and responds to the
 * chat info page's controls – changing the conversation's name and
 * photo, removing participants, leaving the conversation, and
 * previewing media.
 */
object ChatInfoPageViewService {
    // MARK: - Types

    /** A change the user chose to make to the conversation's metadata. */
    sealed interface MetadataChangeType {
        /** A name change, carrying the metadata with the new name applied. */
        data class Name(
            val metadata: ConversationMetadata,
        ) : MetadataChangeType

        /** A photo removal, carrying the metadata with the photo cleared. */
        data class RemovePhoto(
            val metadata: ConversationMetadata,
        ) : MetadataChangeType

        /** A request to capture a new photo with the camera. */
        data object SelectPhotoFromCamera : MetadataChangeType

        /** A request to choose a new photo from the photo library. */
        data object SelectPhotoFromLibrary : MetadataChangeType
    }

    // MARK: - Properties

    /** A Boolean value that indicates whether a media preview is presented. */
    var isPreviewingMedia = false
        private set

    private val cachedChatParticipantsForUserIDs = LockIsolated<Map<String, ChatParticipant>?>(null)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Get Chat Participants

    /**
     * Returns the current conversation's participants, prepared for
     * display, sorted by display name – names beginning with a letter
     * first.
     *
     * Each participant combines their user record with a matching
     * contact pair. Resolved participants are cached in memory per
     * user.
     *
     * @return The participants.
     *
     * @throws Exception if no current conversation is set.
     */
    fun getChatParticipants(): List<ChatParticipant> {
        val conversation =
            ConversationSessionService.currentConversation
                ?: throw Exception("No current conversation.", metadata = ExceptionMetadata(this))

        val cache = cachedChatParticipantsForUserIDs.wrappedValue ?: emptyMap()
        val resolved = mutableMapOf<String, ChatParticipant>()
        val chatParticipants =
            conversation.users.orEmpty().map { user ->
                cache[user.id] ?: run {
                    val participant =
                        ChatParticipant(
                            displayName = user.displayName,
                            contactPair = user.contactPair ?: ContactPair.withUser(user, name = user.displayName),
                        )
                    resolved[user.id] = participant
                    participant
                }
            }

        if (resolved.isNotEmpty()) {
            cachedChatParticipantsForUserIDs.wrappedValue = cache + resolved
        }

        val (withAlphabeticalPrefix, withoutAlphabeticalPrefix) =
            chatParticipants.partition { it.displayName.firstOrNull()?.isLetter() == true }
        return withAlphabeticalPrefix.sortedBy { it.displayName } + withoutAlphabeticalPrefix.sortedBy { it.displayName }
    }

    // MARK: - Reducer Action Handlers

    /**
     * Asks the user to confirm leaving the given conversation, applying
     * it if they accept.
     *
     * Leaving removes the current user from the conversation, dismisses
     * all presented sheets, and returns to the conversations list.
     * Failures surface as a toast.
     *
     * @param conversation The conversation to leave.
     */
    fun leaveConversationButtonTapped(conversation: Conversation?) {
        serviceScope.launch {
            conversation ?: return@launch
            val currentUserID = User.currentUserID ?: return@launch

            var conversationName = "⌘${conversation.metadata.name}⌘"
            if (conversationName.sanitizedName.isBangQualifiedEmpty) conversationName = "Conversation"

            val didConfirm =
                ConfirmationAlert(
                    title = "Leave $conversationName",
                    message = "Are you sure you'd like to leave this conversation?",
                    confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                ).present(
                    translating =
                        listOf(
                            ConfirmationAlert.TranslationOptionKey.ConfirmButtonTitle,
                            ConfirmationAlert.TranslationOptionKey.Message,
                            ConfirmationAlert.TranslationOptionKey.Title,
                        ),
                )
            if (!didConfirm) return@launch

            val sharedEvents = DependencyValues.current.sharedEvents
            sharedEvents.chatInfoPageLoadingStateUpdated.send(Unit)
            RuntimeStorage.store(false, StoredItemKey.shouldNotifyOfConversationAvailability)

            try {
                ActivitySessionService.removeFromConversation(currentUserID, conversation)
                Application.dismissSheets()
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }
    }

    /**
     * Asks the user to confirm removing the given participant, applying
     * it if they accept.
     *
     * Removal dismisses the chat sheet, removes the participant from the
     * conversation, and notifies observers of the activity change.
     * Failures surface as a toast.
     *
     * @param chatParticipant The participant to remove.
     * @param conversation The conversation to remove them from.
     */
    fun removeUserButtonTapped(
        chatParticipant: ChatParticipant,
        conversation: Conversation?,
    ) {
        serviceScope.launch {
            conversation ?: return@launch
            val user = chatParticipant.firstUser ?: return@launch

            val didConfirm =
                ConfirmationAlert(
                    title = user.displayName,
                    message = "Are you sure you'd like to remove this person from the conversation?",
                    confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                ).present(
                    translating =
                        listOf(
                            ConfirmationAlert.TranslationOptionKey.ConfirmButtonTitle,
                            ConfirmationAlert.TranslationOptionKey.Message,
                        ),
                )
            if (!didConfirm) return@launch

            val sharedEvents = DependencyValues.current.sharedEvents
            DependencyValues.current.navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))
            sharedEvents.chatInfoPageLoadingStateUpdated.send(Unit)

            try {
                ActivitySessionService.removeFromConversation(user.id, conversation)
                sharedEvents.currentConversationActivityChanged.send(Unit)
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }
    }

    /**
     * Applies new metadata to the given conversation, recording an
     * activity for the change.
     *
     * @param conversation The conversation to update.
     * @param action The activity action describing the change.
     * @param newMetadata The metadata to apply.
     *
     * @throws Exception if the update fails.
     */
    suspend fun updateMetadata(
        conversation: Conversation,
        action: ActivityAction,
        newMetadata: ConversationMetadata,
    ) {
        ActivitySessionService.updateMetadata(conversation, action, newMetadata)
    }

    // MARK: - View Lifecycle

    /**
     * Responds to the chat info page appearing by dismissing the
     * keyboard and scheduling a metadata change notification for when
     * any in-flight message send completes.
     */
    fun viewAppeared() {
        KeyboardService.resignFirstResponders()
        MessageDeliveryService.addEffectUponIsSendingMessage(
            state = false,
            id = MessageDeliveryServiceEffectID.updateChatInfoPageView,
        ) {
            val sharedEvents = DependencyValues.current.sharedEvents
            sharedEvents.currentConversationMetadataChanged.send(Unit)
        }
    }

    /** Responds to the chat info page finishing its initial load. */
    fun viewLoaded() = Unit

    // MARK: - Clear Cache

    /** Removes every cached chat participant. */
    fun clearCache() {
        cachedChatParticipantsForUserIDs.wrappedValue = null
    }
}

private val String.sanitizedName: String
    get() = replace("⌘", "").replace("⁂", "").replace("※", "")
