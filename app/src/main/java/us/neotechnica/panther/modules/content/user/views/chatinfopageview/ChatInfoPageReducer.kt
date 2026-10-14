//
//  ChatInfoPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.bundle.navigators.ChatNavigatorState
import us.neotechnica.panther.bundle.navigators.ChatRoute
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.modules.content.user.dependencies.chatInfoPageViewService
import us.neotechnica.panther.modules.content.user.dependencies.inputBarService
import us.neotechnica.panther.modules.content.user.extensions.mediaItemMetadata
import us.neotechnica.panther.modules.content.user.extensions.withMessagesOffsetFromCurrentUserAdditionDate
import us.neotechnica.panther.modules.content.user.models.ChatParticipant
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.models.MediaItemViewData
import us.neotechnica.panther.modules.content.user.services.ChatInfoPageViewService
import us.neotechnica.panther.modules.content.user.services.ChatPageViewService
import us.neotechnica.panther.modules.content.user.services.ConversationCellViewService
import us.neotechnica.panther.modules.content.user.services.MediaActionHandlerService
import us.neotechnica.panther.modules.content.user.services.presentChangeMetadataActionSheet
import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import java.util.UUID

/**
 * The reducer that drives the chat info page.
 *
 * This page presents details about a conversation and the actions
 * available on it. It lists the conversation's participants and shared
 * media and – depending on the conversation's kind – lets the user
 * rename the conversation, change its photo, add or remove
 * participants, and leave the conversation. Most of these actions are
 * performed through [ChatInfoPageViewService].
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings
 *   and its participant list concurrently, remaining in the loading
 *   state until the participants resolve. If resolving the
 *   participants fails, the page enters the error state.
 * - A segmented control switches the page between its participant and
 *   media lists.
 * - Changing the conversation's name or photo and adding or removing a
 *   participant are performed through [ChatInfoPageViewService]. When
 *   metadata changes, the page updates its chat's navigation title and
 *   notifies observers of the change.
 * - Because the page mirrors state that lives outside it, several
 *   actions regenerate a view's identity to force it to rebuild from
 *   the latest values.
 */
class ChatInfoPageReducer : Reducer<ChatInfoPageReducer.State, ChatInfoPageReducer.Action> {
    // MARK: - Dependencies

    private val translator: HostedTranslationDelegate by Dependency { it.networking.hostedTranslation }
    private val viewService: ChatInfoPageViewService by Dependency { it.chatInfoPageViewService }

    // MARK: - Actions

    /** The actions the chat info page can process. */
    sealed interface Action {
        /**
         * An action that indicates the view appeared, carrying the
         * identifier key of the conversation the page describes. Begins
         * resolving the display strings and the participant list.
         */
        data class ViewAppeared(
            val conversationIDKey: String,
        ) : Action

        /** An action that indicates the view disappeared. */
        data object ViewDisappeared : Action

        /**
         * An action that indicates the user tapped the add-contact
         * button. Presents the contact selector.
         */
        data object AddContactButtonTapped : Action

        /**
         * An action that indicates the user tapped the change-metadata
         * button. Presents the change-metadata action sheet.
         */
        data object ChangeMetadataButtonTapped : Action

        /**
         * An action that indicates the user tapped the chat info cell.
         * Expands or collapses the participant list.
         */
        data object ChatInfoCellTapped : Action

        /**
         * An action that indicates the current conversation's metadata
         * changed. Rebuilds the page from the latest values.
         */
        data object CurrentConversationMetadataChanged : Action

        /**
         * An action that indicates the user tapped the done header
         * item. Dismisses the page.
         */
        data object DoneHeaderItemTapped : Action

        /**
         * An action that indicates whether a message send is in flight
         * changed, carrying the new value.
         */
        data class IsSendingMessageChanged(
            val isSendingMessage: Boolean,
        ) : Action

        /**
         * An action that indicates the user tapped the
         * leave-conversation button. Begins leaving the conversation.
         */
        data object LeaveConversationButtonTapped : Action

        /** An action that returns the page to its loading state. */
        data object LoadingStateUpdated : Action

        /**
         * An action that indicates the user tapped a shared media item,
         * carrying its metadata. Presents the media preview.
         */
        data class MediaItemViewTapped(
            val metadata: MediaItemViewData,
        ) : Action

        /**
         * An action that indicates the user tapped a participant's
         * remove button, carrying the participant. Begins removing them
         * from the conversation.
         */
        data class RemoveUserButtonTapped(
            val chatParticipant: ChatParticipant,
        ) : Action

        /**
         * An action that indicates the segmented control's selection
         * changed, carrying the new index.
         */
        data class SegmentedControlSelectionIndexChanged(
            val segmentedControlSelectionIndex: Int,
        ) : Action

        /**
         * An action that indicates the user selected a new conversation
         * photo, carrying the chosen image. Applies it as the
         * conversation's photo.
         */
        data class SelectedImageChanged(
            val image: Uri,
        ) : Action

        /** An action that indicates the trait collection changed. */
        data object TraitCollectionChanged : Action

        /**
         * An action that indicates the user tapped a participant's info
         * badge, carrying the user. Presents that user's info alert.
         */
        data class UserInfoBadgeTapped(
            val user: User?,
        ) : Action

        /**
         * An action that indicates the camera picker was dismissed,
         * carrying any resulting [Exception].
         */
        data class CameraPickerDismissed(
            val exception: Exception?,
        ) : Action

        /**
         * An action that indicates the change-metadata action sheet
         * was dismissed, carrying the change the user chose, or `null`
         * if the sheet was canceled.
         */
        data class ChangeMetadataActionSheetDismissed(
            val change: ChatInfoPageViewService.MetadataChangeType?,
        ) : Action

        /**
         * An action that indicates resolving the participant list
         * failed, carrying the resulting [Exception].
         */
        data class GetChatParticipantsFailed(
            val exception: Exception,
        ) : Action

        /**
         * An action that indicates the participant list resolved,
         * carrying the resolved participants.
         */
        data class GetChatParticipantsReturned(
            val chatParticipants: List<ChatParticipant>,
        ) : Action

        /**
         * An action that indicates the photo picker was dismissed,
         * carrying any resulting [Exception].
         */
        data class PhotoPickerDismissed(
            val exception: Exception?,
        ) : Action

        /**
         * An action that indicates display string resolution failed,
         * carrying the resulting [Exception].
         */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        /**
         * An action that indicates display string resolution
         * succeeded, carrying the resolved strings.
         */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        /**
         * An action that indicates a metadata update failed, carrying
         * the resulting [Exception].
         */
        data class UpdateMetadataFailed(
            val exception: Exception,
        ) : Action

        /**
         * An action that indicates a metadata update finished. Updates
         * the navigation title and notifies observers of the change.
         */
        data object UpdateMetadataReturned : Action
    }

    // MARK: - State

    /**
     * The state of the chat info page.
     *
     * @property chatInfoCellViewID The identity of the chat info cell.
     *   Regenerated to rebuild the cell when the participant list
     *   expands or collapses.
     * @property chatParticipants The conversation's participants.
     * @property conversationIDKey The identifier key of the conversation
     *   the page describes.
     * @property isChangeMetadataButtonEnabled A Boolean value that
     *   indicates whether the change-metadata button is enabled.
     *   Disabled while a metadata change is in progress, and while the
     *   conversation is awaiting the initiator's consent.
     * @property isSendingMessage A Boolean value that indicates whether
     *   a message send is in flight.
     * @property segmentedControlSelectionIndex The index of the selected
     *   segmented control option: the participant list or the shared
     *   media list.
     * @property segmentedControlViewID The identity of the segmented
     *   control. Regenerated to rebuild it once display strings
     *   resolve.
     * @property strings The page's translated display strings. Contains
     *   the default, untranslated strings until resolution completes.
     * @property viewID The identity of the page's content. Regenerated
     *   to rebuild it from the latest conversation values.
     * @property viewState The page's loading state. Remains loading
     *   until the participant list resolves.
     * @property visibleParticipants The participants currently shown in
     *   the chat info cell's expandable list. Empty while the list is
     *   collapsed.
     */
    data class State(
        val chatInfoCellViewID: UUID = UUID.randomUUID(),
        val chatParticipants: List<ChatParticipant> = emptyList(),
        val conversationIDKey: String = "",
        val isChangeMetadataButtonEnabled: Boolean = true,
        val isSendingMessage: Boolean = false,
        val segmentedControlSelectionIndex: Int = 0,
        val segmentedControlViewID: UUID = UUID.randomUUID(),
        val strings: List<TranslationOutputMap> = ChatInfoPageViewStrings.defaultOutputMap,
        val viewID: UUID = UUID.randomUUID(),
        val viewState: ViewState = ViewState.Loading,
        val visibleParticipants: List<ChatParticipant> = emptyList(),
        internal val inputBarWasFirstResponder: Boolean = false,
    ) {
        /** The conversation's avatar image, or `null` if none is available. */
        val avatarImage: Bitmap?
            get() = cellViewData?.thumbnailImage

        /** The system image name for the chat info cell's expand-or-collapse chevron. */
        val chatInfoCellImageSystemName: String
            get() = "chevron.${if (visibleParticipants.isEmpty()) "right" else "down"}.circle"

        /** The chat info cell's subtitle: the participants' display names, comma-separated. */
        val chatInfoCellSubtitleLabelText: String
            get() = chatParticipants.joinToString(", ") { it.displayName }

        /** The chat info cell's title: the participant count followed by a localized label. */
        val chatInfoCellTitleLabelText: String
            get() = "${chatParticipants.size} ${strings.value(ChatInfoPageViewStrings.participantCountLabelText)}"

        /** The conversation's title. */
        val chatTitleLabelText: String
            get() = cellViewData?.titleLabelText.orEmpty()

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
            get() = chatParticipants.size > 2 && !isSendingMessage

        /** The metadata for the conversation's shared media items. */
        val mediaItemMetadata: List<MediaItemViewData>
            get() = conversation?.withMessagesOffsetFromCurrentUserAdditionDate?.mediaItemMetadata ?: emptyList()

        /** The maximum width of the segmented control, two-thirds of the screen's width. */
        val segmentedControlMaxWidth: Dp
            get() {
                val displayMetrics = Resources.getSystem().displayMetrics
                return (displayMetrics.widthPixels / displayMetrics.density).dp *
                    ChatInfoPageViewFloats.SEGMENTED_CONTROL_MAX_WIDTH_FRACTION
            }

        /**
         * The titles of the segmented control's options: the participant
         * list and the shared media list.
         */
        val segmentedControlOptionTitles: List<String>
            get() =
                listOf(
                    strings.value(ChatInfoPageViewStrings.segmentedControlParticipantsOptionText),
                    strings.value(ChatInfoPageViewStrings.segmentedControlMediaOptionText),
                )

        /**
         * A Boolean value that indicates whether the segmented control
         * should be elongated to fit long localized option titles.
         */
        val shouldElongateSegmentedControl: Boolean
            get() =
                RuntimeStorage.languageCode != "en" &&
                    segmentedControlOptionTitles.any { it.length >= 25 || it.split(" ").size > 2 }

        /**
         * A Boolean value that indicates whether the change-metadata
         * button is shown. Shown whenever the conversation is set.
         */
        val showsChangeMetadataButton: Boolean
            get() = conversation != null

        /**
         * A Boolean value that indicates whether participants can be
         * removed with a swipe action. Available only in conversations
         * that are not awaiting the initiator's consent and have more
         * than two visible participants.
         */
        val showsRemoveUserSwipeAction: Boolean
            get() {
                val conversation = conversation ?: return false
                return conversation.metadata.requiresConsentFromInitiator == null && visibleParticipants.size > 2
            }

        /**
         * The conversation's sole participant, or `null` when there is
         * not exactly one participant.
         */
        val singleChatParticipant: ChatParticipant?
            get() = chatParticipants.singleOrNull()

        /**
         * The amount by which to increase the number of participant rows
         * shown. Its value is `1` when the participant list is expanded
         * for a conversation that is not awaiting the initiator's
         * consent and has fewer than ten visible participants;
         * otherwise, `0`.
         */
        val visibleParticipantsIncrement: Int
            get() {
                val conversation = conversation ?: return 0
                if (conversation.metadata.requiresConsentFromInitiator != null) return 0
                if (visibleParticipants.isEmpty() || visibleParticipants.size >= 10) return 0
                return 1
            }

        internal val conversation: Conversation?
            get() =
                DependencyValues.current.clientSession.entity.conversation.currentConversation
                    ?: DependencyValues.current.clientSession.store
                        .getConversation(conversationIDKey)

        private val cellViewData: ConversationCellViewData?
            get() = conversation?.let { ConversationCellViewData(it) }
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
            is Action.ViewAppeared -> reduceViewAppeared(state.copy(conversationIDKey = action.conversationIDKey))

            Action.ViewDisappeared -> ReduceResult(state)

            Action.AddContactButtonTapped -> {
                DependencyValues.current.navigation.navigate(
                    Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPaths.ContactSelector)),
                )
                ReduceResult(state)
            }

            Action.ChangeMetadataButtonTapped ->
                ReduceResult(
                    state.copy(isChangeMetadataButtonEnabled = false),
                    Effect.run { send ->
                        send(Action.ChangeMetadataActionSheetDismissed(viewService.presentChangeMetadataActionSheet()))
                    },
                )

            Action.ChatInfoCellTapped ->
                ReduceResult(
                    state.copy(
                        chatInfoCellViewID = UUID.randomUUID(),
                        visibleParticipants = if (state.visibleParticipants.isEmpty()) state.chatParticipants else emptyList(),
                    ),
                )

            Action.CurrentConversationMetadataChanged -> ReduceResult(state.copy(viewID = UUID.randomUUID()))

            Action.DoneHeaderItemTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                if (state.inputBarWasFirstResponder) DependencyValues.current.inputBarService.becomeFirstResponder()
                ReduceResult(state)
            }

            is Action.IsSendingMessageChanged -> ReduceResult(state.copy(isSendingMessage = action.isSendingMessage))

            Action.LeaveConversationButtonTapped -> {
                viewService.leaveConversationButtonTapped(state.conversation)
                ReduceResult(state)
            }

            Action.LoadingStateUpdated -> ReduceResult(state.copy(viewState = ViewState.Loading))

            is Action.MediaItemViewTapped -> {
                val mediaItemMetadata = state.mediaItemMetadata
                viewService.mediaItemViewTapped(
                    action.metadata,
                    filePaths = mediaItemMetadata.map { it.file.relativePath },
                    startingIndex = mediaItemMetadata.map { it.file }.indexOf(action.metadata.file).coerceAtLeast(0),
                )
                ReduceResult(state)
            }

            is Action.RemoveUserButtonTapped -> {
                viewService.removeUserButtonTapped(action.chatParticipant, state.conversation)
                ReduceResult(state)
            }

            is Action.SegmentedControlSelectionIndexChanged ->
                ReduceResult(state.copy(segmentedControlSelectionIndex = action.segmentedControlSelectionIndex))

            is Action.SelectedImageChanged -> reduceSelectedImageChanged(state, action.image)

            Action.TraitCollectionChanged -> ReduceResult(state)

            is Action.UserInfoBadgeTapped -> {
                action.user?.let { ConversationCellViewService.presentUserInfoAlert(it) }
                ReduceResult(state)
            }

            is Action.CameraPickerDismissed -> reducePickerDismissed(state, action.exception)

            is Action.ChangeMetadataActionSheetDismissed -> reduceChangeMetadataActionSheetDismissed(state, action.change)

            is Action.GetChatParticipantsFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Error(action.exception)))
            }

            is Action.GetChatParticipantsReturned -> reduceGetChatParticipantsReturned(state, action.chatParticipants)

            is Action.PhotoPickerDismissed -> reducePickerDismissed(state, action.exception)

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state)
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(segmentedControlViewID = UUID.randomUUID(), strings = action.strings))

            is Action.UpdateMetadataFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
            }

            Action.UpdateMetadataReturned -> reduceUpdateMetadataReturned(state)
        }

    // MARK: - Auxiliary

    private fun reduceViewAppeared(state: State): ReduceResult<State, Action> {
        val newState =
            state.copy(
                inputBarWasFirstResponder = DependencyValues.current.inputBarService.isFirstResponder.value,
                isChangeMetadataButtonEnabled = state.conversation?.metadata?.requiresConsentFromInitiator == null,
                segmentedControlSelectionIndex = 0,
                viewState = ViewState.Loading,
            )

        viewService.viewAppeared()
        return ReduceResult(newState, Effect.merge(resolveEffect(), getChatParticipantsEffect()))
    }

    private fun reduceChangeMetadataActionSheetDismissed(
        state: State,
        change: ChatInfoPageViewService.MetadataChangeType?,
    ): ReduceResult<State, Action> {
        val navigation = DependencyValues.current.navigation
        return when (change) {
            is ChatInfoPageViewService.MetadataChangeType.Name -> {
                val conversation = state.conversation ?: return ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
                val name = change.metadata.name
                val action = if (name.isBangQualifiedEmpty) Activity.Action.RemovedName else Activity.Action.RenamedConversation(name)
                ReduceResult(state, updateMetadataEffect(conversation, action, change.metadata))
            }

            is ChatInfoPageViewService.MetadataChangeType.RemovePhoto -> {
                val conversation = state.conversation ?: return ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
                ReduceResult(state, updateMetadataEffect(conversation, Activity.Action.RemovedGroupPhoto, change.metadata))
            }

            ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromCamera -> {
                navigation.navigate(Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPaths.CameraPicker)))
                ReduceResult(state)
            }

            ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromLibrary -> {
                navigation.navigate(Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPaths.PhotoPicker)))
                ReduceResult(state)
            }

            null -> ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
        }
    }

    private fun reduceGetChatParticipantsReturned(
        state: State,
        chatParticipants: List<ChatParticipant>,
    ): ReduceResult<State, Action> {
        val newState = state.copy(chatParticipants = chatParticipants, visibleParticipants = chatParticipants)
        if (state.viewState != ViewState.Loading) return ReduceResult(newState.copy(viewID = UUID.randomUUID()))

        viewService.viewLoaded()
        return ReduceResult(newState.copy(viewState = ViewState.Loaded))
    }

    private fun reducePickerDismissed(
        state: State,
        exception: Exception?,
    ): ReduceResult<State, Action> {
        DependencyValues.current.navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))
        exception?.let { Logger.log(it, with = AlertType.toast) }
        return ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
    }

    private fun reduceSelectedImageChanged(
        state: State,
        image: Uri,
    ): ReduceResult<State, Action> {
        val conversation =
            state.conversation ?: run {
                Logger.log(compressionFailure(), with = AlertType.toast)
                return ReduceResult(state.copy(isChangeMetadataButtonEnabled = true))
            }

        return ReduceResult(
            state,
            Effect.run { send ->
                val imageData =
                    try {
                        MediaActionHandlerService.compressImageToKB(image, IMAGE_COMPRESSION_SIZE_KB)
                    } catch (exception: Exception) {
                        return@run send(Action.UpdateMetadataFailed(compressionFailure(exception)))
                    }

                try {
                    viewService.updateMetadata(
                        conversation,
                        Activity.Action.ChangedGroupPhoto,
                        conversation.metadata.copyWith(imageData = imageData),
                    )
                    send(Action.UpdateMetadataReturned)
                } catch (exception: Exception) {
                    send(Action.UpdateMetadataFailed(exception))
                }
            },
        )
    }

    private fun reduceUpdateMetadataReturned(state: State): ReduceResult<State, Action> {
        DependencyValues
            .current
            .sharedEvents
            .currentConversationMetadataChanged
            .send(Unit)

        val titleLabelText = state.conversation?.let { ConversationCellViewData(it) }?.titleLabelText
        if (titleLabelText != null) ChatPageViewService.setNavigationTitle(titleLabelText)
        return ReduceResult(state.copy(isChangeMetadataButtonEnabled = true, viewID = UUID.randomUUID()))
    }

    private fun compressionFailure(underlyingException: Exception? = null): Exception =
        Exception(
            "Failed to compress image.",
            underlyingExceptions = underlyingException?.let { listOf(it) },
            metadata = ExceptionMetadata(this),
        )

    private fun getChatParticipantsEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.GetChatParticipantsReturned(viewService.getChatParticipants()))
            } catch (exception: Exception) {
                send(Action.GetChatParticipantsFailed(exception))
            }
        }

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(translator.resolve(ChatInfoPageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    private fun updateMetadataEffect(
        conversation: Conversation,
        action: Activity.Action,
        newMetadata: ConversationMetadata,
    ): Effect<Action> =
        Effect.run { send ->
            try {
                viewService.updateMetadata(conversation, action, newMetadata)
                send(Action.UpdateMetadataReturned)
            } catch (exception: Exception) {
                send(Action.UpdateMetadataFailed(exception))
            }
        }
}

// The size, in kilobytes, a selected conversation photo is compressed to.
private const val IMAGE_COMPRESSION_SIZE_KB = 100
