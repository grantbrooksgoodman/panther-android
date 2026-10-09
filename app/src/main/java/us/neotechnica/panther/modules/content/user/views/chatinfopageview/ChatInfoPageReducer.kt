//
//  ChatInfoPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewConstants
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.models.MediaItemViewData
import us.neotechnica.panther.modules.content.user.services.ChatInfoPageViewService
import us.neotechnica.panther.modules.content.user.services.ConversationCellViewService
import us.neotechnica.panther.modules.content.user.services.presentChangeMetadataActionSheet
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.ActivityAction
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.messages
import us.neotechnica.panther.modules.session.entity.extensions.offsetFromCurrentUserAdditionDate
import us.neotechnica.panther.modules.session.entity.extensions.sortedByDescendingSentDate
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.navigation.ChatNavigatorState
import us.neotechnica.panther.navigation.ChatRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * The reducer that drives the chat info page.
 *
 * This page presents details about a conversation and the actions
 * available on it. It lists the conversation's participants and shared
 * media and – for groups – lets the user rename the conversation,
 * change its photo, add or remove participants, and leave the
 * conversation. Most of these actions are performed through
 * [ChatInfoPageViewService].
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings
 *   and its participant list.
 * - A segmented control switches the page between its participant and
 *   media lists.
 * - Changing the conversation's name or photo and adding or removing a
 *   participant are performed through [ChatInfoPageViewService]. When
 *   metadata changes, the page reloads and notifies observers.
 */
class ChatInfoPageReducer : Reducer<ChatInfoPageReducer.State, ChatInfoPageReducer.Action> {
    // MARK: - Types

    /** The source from which to capture a new conversation photo. */
    enum class PhotoCaptureSource {
        CAMERA,
        LIBRARY,
    }

    // MARK: - Action

    sealed interface Action {
        data class ViewFirstAppeared(
            val conversationIDKey: String,
        ) : Action

        data object ViewDisappeared : Action

        data object AddContactButtonTapped : Action

        data class CameraPickerDismissed(
            val exception: Exception?,
        ) : Action

        data class ChangeMetadataActionSheetDismissed(
            val change: ChatInfoPageViewService.MetadataChangeType?,
        ) : Action

        data object ChangeMetadataButtonTapped : Action

        data object ChatInfoCellTapped : Action

        data object CurrentConversationMetadataChanged : Action

        data object DoneHeaderItemTapped : Action

        data class GetChatParticipantsFailed(
            val exception: Exception,
        ) : Action

        data class GetChatParticipantsReturned(
            val chatParticipants: List<ChatParticipant>,
        ) : Action

        data class IsSendingMessageChanged(
            val isSendingMessage: Boolean,
        ) : Action

        data object LeaveConversationButtonTapped : Action

        data object LoadingStateUpdated : Action

        data class PhotoCaptureHandled(
            val source: PhotoCaptureSource,
        ) : Action

        data class PhotoPickerDismissed(
            val exception: Exception?,
        ) : Action

        data class RemoveUserButtonTapped(
            val chatParticipant: ChatParticipant,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class SegmentedControlSelectionIndexChanged(
            val index: Int,
        ) : Action

        data class SelectedImageChanged(
            val imageData: ByteArray,
        ) : Action

        data object TraitCollectionChanged : Action

        data class UpdateMetadataFailed(
            val exception: Exception,
        ) : Action

        data object UpdateMetadataReturned : Action

        data class UserInfoBadgeTapped(
            val user: User?,
        ) : Action
    }

    // MARK: - State

    data class State(
        val conversationIDKey: String = "",
        val conversation: Conversation? = null,
        val chatParticipants: List<ChatParticipant> = emptyList(),
        val visibleParticipants: List<ChatParticipant> = emptyList(),
        val mediaItems: List<MediaItemViewData> = emptyList(),
        val segmentedControlSelectionIndex: Int = 0,
        val isChangeMetadataButtonEnabled: Boolean = true,
        val isSendingMessage: Boolean = false,
        val photoCaptureRequest: PhotoCaptureSource? = null,
        val strings: List<TranslationOutputMap> = ChatInfoPageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
        val viewID: UUID = UUID.randomUUID(),
    ) {
        /**
         * A Boolean value that indicates whether the conversation is a
         * group – more than two participants.
         */
        val isGroup: Boolean
            get() = (conversation?.participants?.size ?: 0) > 2

        /** The conversation's title. */
        val chatTitleLabelText: String
            get() = conversation?.let { ConversationCellViewData.title(it) }.orEmpty()

        /**
         * A Boolean value that indicates whether the add-contact button
         * is enabled. Disabled while a message is being sent.
         */
        val isAddContactButtonEnabled: Boolean
            get() = !isSendingMessage

        /** A Boolean value that indicates whether Developer Mode is enabled. */
        val isDeveloperModeEnabled: Boolean
            get() = Build.isDeveloperModeEnabled

        /**
         * A Boolean value that indicates whether the leave-conversation
         * button is enabled. Enabled only when the conversation has more
         * than two participants and no message is being sent.
         */
        val isLeaveConversationButtonEnabled: Boolean
            get() = chatParticipants.size > 1 && !isSendingMessage

        /**
         * A Boolean value that indicates whether participants can be
         * removed with a swipe action. Available only in conversations
         * that are not awaiting the initiator's consent and have more
         * than two participants.
         */
        val showsRemoveUserSwipeAction: Boolean
            get() = conversation?.metadata?.requiresConsentFromInitiator == null && visibleParticipants.size > 1

        /**
         * A Boolean value that indicates whether the change-metadata
         * button is shown – only for groups.
         */
        val showsChangeMetadataButton: Boolean
            get() = isGroup

        /**
         * The amount by which to increase the number of participant rows
         * shown, for the add-contact row. Its value is `1` in an
         * expanded conversation that is not awaiting the initiator's
         * consent and has fewer than nine other participants; otherwise
         * `0`.
         */
        val visibleParticipantsIncrement: Int
            get() =
                if (conversation?.metadata?.requiresConsentFromInitiator == null &&
                    visibleParticipants.isNotEmpty() &&
                    visibleParticipants.size < MAX_OTHER_PARTICIPANTS_FOR_ADD_CONTACT
                ) {
                    1
                } else {
                    0
                }
    }

    // MARK: - Reduce

    // The reduce dispatch handles every action; the suppressions below
    // cover its size and complexity.
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            is Action.ViewFirstAppeared -> {
                val conversation = SessionStore.getConversation(action.conversationIDKey)
                ChatInfoPageViewService.viewAppeared()
                ReduceResult(
                    state.copy(
                        conversationIDKey = action.conversationIDKey,
                        conversation = conversation,
                        mediaItems = buildMediaItems(conversation),
                        viewState = ViewState.Loading,
                    ),
                    Effect.merge(resolveEffect(), getChatParticipantsEffect()),
                )
            }

            Action.ViewDisappeared -> ReduceResult(state)

            Action.AddContactButtonTapped -> {
                navigateToContactSelector()
                ReduceResult(state)
            }

            is Action.CameraPickerDismissed -> {
                action.exception?.let { Logger.log(it, with = AlertType.toast) }
                ReduceResult(state.copy(isChangeMetadataButtonEnabled = true, photoCaptureRequest = null))
            }

            is Action.PhotoPickerDismissed -> {
                action.exception?.let { Logger.log(it, with = AlertType.toast) }
                ReduceResult(state.copy(isChangeMetadataButtonEnabled = true, photoCaptureRequest = null))
            }

            is Action.ChangeMetadataActionSheetDismissed ->
                reduceChangeMetadataActionSheetDismissed(state, action.change)

            Action.ChangeMetadataButtonTapped ->
                ReduceResult(
                    state.copy(isChangeMetadataButtonEnabled = false),
                    Effect.run { send ->
                        send(Action.ChangeMetadataActionSheetDismissed(ChatInfoPageViewService.presentChangeMetadataActionSheet()))
                    },
                )

            Action.ChatInfoCellTapped ->
                ReduceResult(
                    state.copy(visibleParticipants = if (state.visibleParticipants.isEmpty()) state.chatParticipants else emptyList()),
                )

            Action.CurrentConversationMetadataChanged -> {
                val conversation = SessionStore.getConversation(state.conversationIDKey)
                ReduceResult(
                    state.copy(
                        conversation = conversation,
                        mediaItems = buildMediaItems(conversation),
                        viewID = UUID.randomUUID(),
                    ),
                )
            }

            Action.DoneHeaderItemTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            is Action.GetChatParticipantsFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Error(action.exception)))
            }

            is Action.GetChatParticipantsReturned ->
                ReduceResult(
                    state.copy(
                        chatParticipants = action.chatParticipants,
                        visibleParticipants = action.chatParticipants,
                        viewState = ViewState.Loaded,
                    ),
                )

            is Action.IsSendingMessageChanged ->
                ReduceResult(state.copy(isSendingMessage = action.isSendingMessage))

            Action.LeaveConversationButtonTapped -> {
                ChatInfoPageViewService.leaveConversationButtonTapped(state.conversation)
                ReduceResult(state)
            }

            Action.LoadingStateUpdated ->
                ReduceResult(state.copy(viewState = ViewState.Loading))

            is Action.PhotoCaptureHandled ->
                ReduceResult(state.copy(photoCaptureRequest = null))

            is Action.RemoveUserButtonTapped -> {
                ChatInfoPageViewService.removeUserButtonTapped(action.chatParticipant, state.conversation)
                ReduceResult(state)
            }

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state)
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings))

            is Action.SegmentedControlSelectionIndexChanged ->
                ReduceResult(state.copy(segmentedControlSelectionIndex = action.index))

            is Action.SelectedImageChanged ->
                reduceSelectedImageChanged(state, action.imageData)

            Action.TraitCollectionChanged -> ReduceResult(state)

            is Action.UpdateMetadataFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
            }

            Action.UpdateMetadataReturned -> {
                val conversation = SessionStore.getConversation(state.conversationIDKey)
                ReduceResult(
                    state.copy(
                        conversation = conversation,
                        mediaItems = buildMediaItems(conversation),
                        isChangeMetadataButtonEnabled = true,
                        viewID = UUID.randomUUID(),
                    ),
                    getChatParticipantsEffect(),
                )
            }

            is Action.UserInfoBadgeTapped -> {
                action.user?.let { ConversationCellViewService.presentUserInfoAlert(it) }
                ReduceResult(state)
            }
        }

    // MARK: - Change Metadata

    private fun reduceChangeMetadataActionSheetDismissed(
        state: State,
        change: ChatInfoPageViewService.MetadataChangeType?,
    ): ReduceResult<State, Action> {
        val conversation = state.conversation
        return when (change) {
            is ChatInfoPageViewService.MetadataChangeType.Name -> {
                if (conversation == null) return ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
                val activityAction =
                    if (change.metadata.name.isBangQualifiedEmpty) {
                        ActivityAction.RemovedName
                    } else {
                        ActivityAction.RenamedConversation(change.metadata.name)
                    }
                ReduceResult(state, updateMetadataEffect(conversation, activityAction, change.metadata))
            }

            is ChatInfoPageViewService.MetadataChangeType.RemovePhoto -> {
                if (conversation == null) return ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
                ReduceResult(state, updateMetadataEffect(conversation, ActivityAction.RemovedGroupPhoto, change.metadata))
            }

            ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromCamera ->
                ReduceResult(state.copy(photoCaptureRequest = PhotoCaptureSource.CAMERA))

            ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromLibrary ->
                ReduceResult(state.copy(photoCaptureRequest = PhotoCaptureSource.LIBRARY))

            null -> ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
        }
    }

    private fun reduceSelectedImageChanged(
        state: State,
        imageData: ByteArray,
    ): ReduceResult<State, Action> {
        val conversation = state.conversation ?: return ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
        val newMetadata =
            conversation.metadata.copyWith(
                imageData = imageData,
                imageHash = ConversationMetadata.computeImageHash(imageData),
            )
        return ReduceResult(state, updateMetadataEffect(conversation, ActivityAction.ChangedGroupPhoto, newMetadata))
    }

    private fun updateMetadataEffect(
        conversation: Conversation,
        action: ActivityAction,
        newMetadata: ConversationMetadata,
    ): Effect<Action> =
        Effect.run { send ->
            try {
                ChatInfoPageViewService.updateMetadata(conversation, action, newMetadata)
                send(Action.UpdateMetadataReturned)
            } catch (exception: Exception) {
                send(Action.UpdateMetadataFailed(exception))
            }
        }

    // MARK: - Effects

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(ChatInfoPageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    private fun getChatParticipantsEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.GetChatParticipantsReturned(ChatInfoPageViewService.getChatParticipants()))
            } catch (exception: Exception) {
                send(Action.GetChatParticipantsFailed(exception))
            }
        }

    private fun navigateToContactSelector() {
        DependencyValues.current.navigation.navigate(
            Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPath.ContactSelector)),
        )
    }

    // MARK: - Media Items

    private fun buildMediaItems(conversation: Conversation?): List<MediaItemViewData> {
        conversation ?: return emptyList()
        val users = conversation.users.orEmpty()
        return conversation.messages
            .orEmpty()
            .offsetFromCurrentUserAdditionDate(conversation.activities)
            .filter { it.contentType.isMedia }
            .sortedByDescendingSentDate
            .mapNotNull { message ->
                val mediaFile = message.richContent?.mediaComponent ?: return@mapNotNull null
                val user = users.firstOrNull { it.id == message.fromAccountID } ?: SessionStore.users[message.fromAccountID]
                MediaItemViewData(
                    file = mediaFile,
                    mediaTypeLabelText = mediaTypeLabel(mediaFile),
                    senderLabelText = senderLabel(message, user),
                    timestampLabelText =
                        SimpleDateFormat(ChatInfoPageViewConstants.TIMESTAMP_FORMAT, Locale.getDefault())
                            .format(message.sentDate),
                )
            }
    }

    private fun mediaTypeLabel(mediaFile: MediaFile): String {
        val fileExtension = mediaFile.fileExtension
        return when {
            fileExtension.isDocument ->
                "${LocalizedStringKey.File.localized()}${ChatInfoPageViewConstants.FILE_TYPE_SEPARATOR}${fileExtension.rawValue}"
            fileExtension.isImage -> LocalizedStringKey.Image.localized()
            fileExtension.isVideo -> LocalizedStringKey.Video.localized()
            else -> LocalizedStringKey.Attachment.localized()
        }
    }

    private fun senderLabel(
        message: Message,
        user: User?,
    ): String {
        if (message.isFromCurrentUser) {
            return LocalizedStringKey.FromYou.localized().replaceFirstChar { it.lowercase() }
        }
        val displayName = user?.displayName ?: message.fromAccountID
        return LocalizedStringKey.FromUser.localized().replace("⌘", displayName)
    }

    // MARK: - Companion

    companion object {
        // The maximum number of other participants for which the add-contact
        // row is shown; beyond it, the group is too large to keep growing inline.
        private const val MAX_OTHER_PARTICIPANTS_FOR_ADD_CONTACT = 9
    }
}
