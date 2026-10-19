//
//  ChatInfoPageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

@file:Suppress("FileLength")

package us.neotechnica.panther.modules.content.user.services.chatinfopageview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.neotechnica.panther.Application
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.bundle.chatInfoPageLoadingStateUpdated
import us.neotechnica.panther.bundle.currentConversationActivityChanged
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.bundle.navigators.ChatRoute
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.bundle.shouldNotifyOfConversationAvailability
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import us.neotechnica.panther.modules.content.user.models.MediaItemViewData
import us.neotechnica.panther.modules.content.user.models.MessageDeliveryServiceEffectID
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Activity
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.sources.conversation.remotelyupdatable.ConversationUpdatableKey
import us.neotechnica.panther.modules.networking.sources.conversation.remotelyupdatable.updateValues
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// The counterpart service carries `// swiftlint:disable file_length`.

/**
 * The service that handles the chat info page's user interactions.
 *
 * Use [ChatInfoPageViewService] to load the current conversation's
 * participants and to respond to the chat info page's controls –
 * changing the conversation's name and photo, removing participants,
 * leaving the conversation, and previewing media.
 *
 * Resolve the service through
 * [DependencyValues.chatInfoPageViewService][us.neotechnica.panther.modules.content.user.dependencies.chatInfoPageViewService].
 *
 * @param dependencies The collaborators the service resolves its work
 *   through. The default resolves the live services.
 */
class ChatInfoPageViewService(
    private val dependencies: Dependencies = Dependencies(),
) {
    // MARK: - Dependencies

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

    /**
     * A media preview the chat info page presents.
     *
     * @property filePaths The relative paths of the files available to
     *   preview.
     * @property startingIndex The index of the file to show first.
     * @property title The preview's title.
     */
    data class MediaPreview(
        val filePaths: List<String>,
        val startingIndex: Int,
        val title: String,
    )

    /**
     * The collaborators the service resolves its work through.
     *
     * Each property defaults to the live service; a test replaces the
     * ones it exercises.
     *
     * @property addEffectUponIsSendingMessage Registers the given
     *   effect to run once the current send completes.
     * @property contactPair Returns the given user's contact pair, or
     *   `null` if the user has none.
     * @property currentConversation Returns the current conversation.
     * @property currentUserID Returns the current user's identifier.
     * @property dismissSheets Dismisses every presented sheet.
     * @property navigate Navigates to the given route.
     * @property presentConfirmationAlert Presents the given
     *   confirmation alert, translating the given parts, and returns
     *   whether the user confirmed.
     * @property removeConversation Removes the conversation with the
     *   given ID key from the session store.
     * @property removeFromConversation Removes the user with the given
     *   identifier from the given conversation.
     * @property resignFirstResponders Dismisses the keyboard.
     * @property resolveUsers Resolves the given conversation's users
     *   from the server.
     * @property sendChatInfoPageLoadingStateUpdated Sends the
     *   `chatInfoPageLoadingStateUpdated` shared event.
     * @property sendCurrentConversationActivityChanged Sends the
     *   `currentConversationActivityChanged` shared event.
     * @property sendCurrentConversationMetadataChanged Sends the
     *   `currentConversationMetadataChanged` shared event.
     * @property storeShouldNotifyOfConversationAvailability Stores
     *   whether to notify of conversation availability.
     * @property updateValues Updates the given conversation's hosted
     *   values with the given data, returning the updated
     *   conversation.
     */
    data class Dependencies(
        val addEffectUponIsSendingMessage: (() -> Unit) -> Unit = { effect ->
            MessageDeliveryService.addEffectUponIsSendingMessage(
                state = false,
                id = MessageDeliveryServiceEffectID.updateChatInfoPageView,
                effect = effect,
            )
        },
        val contactPair: (User) -> ContactPair? = { it.contactPair },
        val currentConversation: () -> Conversation? = { DependencyValues.current.clientSession.entity.conversation.currentConversation },
        val currentUserID: () -> String? = { User.currentUserID },
        val dismissSheets: () -> Unit = { Application.dismissSheets() },
        val navigate: (Route) -> Unit = { DependencyValues.current.navigation.navigate(it) },
        val presentConfirmationAlert: suspend (ConfirmationAlert, List<ConfirmationAlert.TranslationOptionKey>) -> Boolean =
            { alert, translating -> alert.present(translating = translating) },
        val removeConversation: (String) -> Unit = {
            DependencyValues.current.clientSession.store
                .removeConversation(idKey = it)
        },
        val removeFromConversation: suspend (String, Conversation) -> Unit = { userID, conversation ->
            DependencyValues.current.clientSession.entity.activity
                .removeFromConversation(userID, conversation)
        },
        val resignFirstResponders: () -> Unit = { KeyboardService.resignFirstResponders() },
        val resolveUsers: suspend (Conversation) -> Unit = { it.resolveUsers(forceUpdate = true) },
        val sendChatInfoPageLoadingStateUpdated: () -> Unit = {
            DependencyValues
                .current
                .sharedEvents
                .chatInfoPageLoadingStateUpdated
                .send(Unit)
        },
        val sendCurrentConversationActivityChanged: () -> Unit = {
            DependencyValues
                .current
                .sharedEvents
                .currentConversationActivityChanged
                .send(Unit)
        },
        val sendCurrentConversationMetadataChanged: () -> Unit = {
            DependencyValues
                .current
                .sharedEvents
                .currentConversationMetadataChanged
                .send(Unit)
        },
        val storeShouldNotifyOfConversationAvailability: (Boolean) -> Unit = {
            RuntimeStorage.store(it, StoredItemKey.shouldNotifyOfConversationAvailability)
        },
        val updateValues: suspend (Conversation, Map<ConversationUpdatableKey, Any>) -> Conversation = { conversation, data ->
            conversation.updateValues(data)
        },
    )

    // MARK: - Properties

    private val cachedChatParticipantsForUserIDs = LockIsolated<Map<String, ChatParticipant>?>(null)
    private val mutableMediaPreview = MutableStateFlow<MediaPreview?>(null)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * The media preview the chat info page presents, or `null` if
     * none is presented.
     */
    val mediaPreview: StateFlow<MediaPreview?> = mutableMediaPreview.asStateFlow()

    /** A Boolean value that indicates whether a media preview is presented. */
    var isPreviewingMedia = false
        private set

    // MARK: - Get Chat Participants

    /**
     * Returns the current conversation's participants, prepared for
     * display.
     *
     * Each participant combines their user record with a matching
     * contact pair, when one exists. Results are cached in memory per
     * user. When the conversation's users are not all resolved, they
     * are resolved from the network first.
     *
     * @return The participants, sorted by display name – names
     *   beginning with a letter first.
     *
     * @throws Exception if no current conversation is set, or if
     *   participant resolution fails.
     */
    suspend fun getChatParticipants(): List<ChatParticipant> {
        val conversation =
            dependencies.currentConversation()
                ?: throw Exception("No current conversation.", metadata = ExceptionMetadata(this))

        val users = conversation.users
        if (users.isNullOrEmpty() || conversation.participants.size - 1 != users.size) {
            dependencies.resolveUsers(conversation)
            return getChatParticipants()
        }

        val cache = cachedChatParticipantsForUserIDs.wrappedValue ?: emptyMap()
        val resolved = mutableMapOf<String, ChatParticipant>()
        val chatParticipants =
            users.map { user ->
                cache[user.id] ?: run {
                    val contactPair = dependencies.contactPair(user) ?: ContactPair.withUser(user, name = user.displayName)
                    val participant = ChatParticipant(displayName = contactPair.contact.fullName, contactPair = contactPair)
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
     * all presented sheets, removes the conversation from the session
     * store, and returns to the conversations list. Failures surface as
     * a toast.
     *
     * @param conversation The conversation to leave.
     */
    fun leaveConversationButtonTapped(conversation: Conversation?) {
        serviceScope.launch {
            conversation ?: return@launch
            var conversationName = "⌘${conversation.metadata.name}⌘"
            if (conversationName.sanitized.isBangQualifiedEmpty) conversationName = "Conversation"

            val currentUserID = dependencies.currentUserID() ?: return@launch
            val didConfirm =
                dependencies.presentConfirmationAlert(
                    ConfirmationAlert(
                        title = "Leave $conversationName",
                        message = "Are you sure you'd like to leave this conversation?",
                        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
                        confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                    ),
                    listOf(
                        ConfirmationAlert.TranslationOptionKey.ConfirmButtonTitle,
                        ConfirmationAlert.TranslationOptionKey.Message,
                        ConfirmationAlert.TranslationOptionKey.Title,
                    ),
                )
            if (!didConfirm) return@launch

            dependencies.sendChatInfoPageLoadingStateUpdated()
            dependencies.storeShouldNotifyOfConversationAvailability(false)

            try {
                dependencies.removeFromConversation(currentUserID, conversation)
                dependencies.dismissSheets()
                dependencies.removeConversation(conversation.id.key)
                dependencies.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }
    }

    /**
     * Previews the conversation's media files, starting at the given
     * index.
     *
     * The preview stays presented until [mediaPreviewDismissed] is
     * called.
     *
     * @param metadata The display metadata of the tapped item.
     * @param filePaths The relative paths of the files available to
     *   preview.
     * @param startingIndex The index of the file to show first.
     */
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun mediaItemViewTapped(
        metadata: MediaItemViewData,
        filePaths: List<String>,
        startingIndex: Int,
    ) {
        isPreviewingMedia = true
        mutableMediaPreview.value =
            MediaPreview(
                filePaths = filePaths,
                startingIndex = startingIndex,
                title = LocalizedStringKey.Attachment.localized(),
            )
    }

    /** Responds to the media preview being dismissed. */
    fun mediaPreviewDismissed() {
        mutableMediaPreview.value = null
        isPreviewingMedia = false
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
                dependencies.presentConfirmationAlert(
                    ConfirmationAlert(
                        title = user.displayName,
                        message = "Are you sure you'd like to remove this person from the conversation?",
                        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
                        confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                    ),
                    listOf(
                        ConfirmationAlert.TranslationOptionKey.ConfirmButtonTitle,
                        ConfirmationAlert.TranslationOptionKey.Message,
                    ),
                )
            if (!didConfirm) return@launch

            dependencies.navigate(Route.Chat(ChatRoute.Sheet(null)))
            dependencies.sendChatInfoPageLoadingStateUpdated()

            try {
                dependencies.removeFromConversation(user.id, conversation)
                dependencies.sendCurrentConversationActivityChanged()
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
     * @throws Exception if the activity cannot be synthesized, or if
     *   the update fails.
     */
    suspend fun updateMetadata(
        conversation: Conversation,
        action: Activity.Action,
        newMetadata: ConversationMetadata,
    ) {
        val activity =
            Activity.from(action)
                ?: throw Exception("Failed to synthesize activity.", metadata = ExceptionMetadata(this))

        val newActivities = ((conversation.activities ?: emptyList()) + activity).filter { it != Activity.empty }
        dependencies.updateValues(
            conversation,
            mapOf(
                ConversationUpdatableKey.ACTIVITIES to newActivities,
                ConversationUpdatableKey.METADATA to newMetadata,
            ),
        )
    }

    /**
     * Responds to the chat info page appearing.
     *
     * This method dismisses the keyboard and schedules a metadata
     * change notification for when any in-flight message send
     * completes.
     */
    fun viewAppeared() {
        dependencies.resignFirstResponders()
        dependencies.addEffectUponIsSendingMessage { dependencies.sendCurrentConversationMetadataChanged() }
    }

    /** Responds to the chat info page finishing its initial load. */
    fun viewLoaded() = Unit

    // MARK: - Clear Cache

    /** Removes every cached chat participant. */
    fun clearCache() {
        cachedChatParticipantsForUserIDs.wrappedValue = null
    }
}
