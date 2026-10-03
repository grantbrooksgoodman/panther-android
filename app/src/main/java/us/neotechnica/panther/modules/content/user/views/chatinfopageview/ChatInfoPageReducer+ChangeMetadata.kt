//
//  ChatInfoPageReducer+ChangeMetadata.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 21/07/2025.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import us.neotechnica.panther.designsystem.modules.alertkit.models.Action as AlertKitAction
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.modules.networking.conversation.models.ActivityAction
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.session.entity.services.ActivitySessionService
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.Send
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * Presents the change-metadata action sheet for the conversation's name
 * and photo. Choosing to change the name presents a text input alert;
 * choosing to change the photo offers the camera or the photo library.
 * The remove-photo option appears only when the conversation has a photo.
 */
internal fun ChatInfoPageReducer.changeMetadataEffect(state: ChatInfoPageReducer.State): Effect<ChatInfoPageReducer.Action> =
    Effect.run { send ->
        val conversation = state.conversation ?: return@run

        var choice: MetadataChoice? = null
        val actions =
            mutableListOf(
                AlertKitAction("Change name") { choice = MetadataChoice.CHANGE_NAME },
                AlertKitAction("Change photo") { choice = MetadataChoice.CHANGE_PHOTO },
            )
        if (conversation.metadata.imageData != null) {
            actions.add(
                AlertKitAction("Remove photo", style = ActionStyle.DESTRUCTIVE) { choice = MetadataChoice.REMOVE_PHOTO },
            )
        }

        ActionSheetAlert(
            title = "Change name and photo",
            actions = actions,
            cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
        ).present(
            translating =
                listOf(
                    ActionSheetAlert.TranslationOptionKey.Actions(),
                    ActionSheetAlert.TranslationOptionKey.Title,
                ),
        )

        when (choice) {
            MetadataChoice.CHANGE_NAME -> presentChangeNameAlert(conversation, send)
            MetadataChoice.CHANGE_PHOTO -> presentPhotoSourceSheet()?.let { send(ChatInfoPageReducer.Action.RequestPhotoCapture(it)) }
            MetadataChoice.REMOVE_PHOTO -> removePhoto(conversation, send)
            null -> Unit
        }
    }

/** Applies the given compressed image as the conversation's new photo. */
internal fun ChatInfoPageReducer.changePhotoMutation(
    state: ChatInfoPageReducer.State,
    imageData: ByteArray,
): ReduceResult<ChatInfoPageReducer.State, ChatInfoPageReducer.Action> {
    val conversation = state.conversation ?: return ReduceResult(state)
    return ReduceResult(
        state,
        Effect.run { send ->
            try {
                val newMetadata =
                    conversation.metadata.copyWith(
                        imageData = imageData,
                        imageHash = ConversationMetadata.computeImageHash(imageData),
                    )
                ActivitySessionService.updateMetadata(conversation, ActivityAction.ChangedGroupPhoto, newMetadata)
                send(ChatInfoPageReducer.Action.Reload)
            } catch (exception: Exception) {
                send(ChatInfoPageReducer.Action.Failed(exception))
            }
        },
    )
}

// MARK: - Auxiliary

/** A metadata change the user chose from the change-metadata action sheet. */
private enum class MetadataChoice {
    CHANGE_NAME,
    CHANGE_PHOTO,
    REMOVE_PHOTO,
}

/**
 * Presents the photo-source action sheet, offering the camera or the
 * photo library, and returns the chosen source, or `null` if cancelled.
 */
private suspend fun presentPhotoSourceSheet(): ChatInfoPageReducer.PhotoCaptureSource? {
    var source: ChatInfoPageReducer.PhotoCaptureSource? = null
    ActionSheetAlert(
        title = "Change name and photo",
        actions =
            listOf(
                AlertKitAction("Take photo") { source = ChatInfoPageReducer.PhotoCaptureSource.CAMERA },
                AlertKitAction("Choose photo from library") { source = ChatInfoPageReducer.PhotoCaptureSource.LIBRARY },
            ),
        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
    ).present(
        translating =
            listOf(
                ActionSheetAlert.TranslationOptionKey.Actions(),
                ActionSheetAlert.TranslationOptionKey.Title,
            ),
    )
    return source
}

/** Clears the conversation's photo, recording a removed-group-photo activity. */
private suspend fun removePhoto(
    conversation: Conversation,
    send: Send<ChatInfoPageReducer.Action>,
) {
    try {
        val newMetadata = conversation.metadata.copyWith(imageData = null, imageHash = null)
        ActivitySessionService.updateMetadata(conversation, ActivityAction.RemovedGroupPhoto, newMetadata)
        send(ChatInfoPageReducer.Action.Reload)
    } catch (exception: Exception) {
        send(ChatInfoPageReducer.Action.Failed(exception))
    }
}

/**
 * Presents the change-name text input alert, live-disabling the confirm
 * button while the field contains reserved characters, then applies the
 * rename.
 */
private suspend fun presentChangeNameAlert(
    conversation: Conversation,
    send: Send<ChatInfoPageReducer.Action>,
) {
    val currentName =
        conversation.metadata.name
            .takeUnless { it.isBangQualifiedEmpty }
            .orEmpty()
    val input =
        TextInputAlert(
            message = "Choose a new name for this conversation:",
            attributes =
                TextFieldAttributes(
                    clearButtonMode = TextFieldAttributes.ClearButtonMode.ALWAYS,
                    sampleText = currentName,
                ),
            cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            confirmButtonTitle = LocalizedStringKey.Done.localized(),
            isConfirmEnabled = { it.none { character -> character in "⌘:" } },
        ).present(translating = listOf(TextInputAlert.TranslationOptionKey.Message)) ?: return
    val (action, newMetadata) = resolveNameChange(conversation, input) ?: return

    try {
        ActivitySessionService.updateMetadata(conversation, action, newMetadata)
        send(ChatInfoPageReducer.Action.Reload)
    } catch (exception: Exception) {
        send(ChatInfoPageReducer.Action.Failed(exception))
    }
}

/**
 * Resolves the given input into the activity action and metadata for
 * renaming the conversation, or `null` when the input is invalid or a
 * no-op.
 *
 * Names containing reserved characters are rejected, an unchanged name
 * is a no-op, and clearing the name records a removed-name activity.
 */
private fun resolveNameChange(
    conversation: Conversation,
    input: String,
): Pair<ActivityAction, ConversationMetadata>? {
    if (input.any { it in "⌘:" }) return null
    if (input == conversation.metadata.name) return null
    if (input.isBangQualifiedEmpty && conversation.metadata.name.isBangQualifiedEmpty) return null

    val trimmed = input.trim()
    val newName = if (trimmed.isBangQualifiedEmpty) BANG_QUALIFIED_EMPTY else trimmed
    val action =
        if (newName.isBangQualifiedEmpty) ActivityAction.RemovedName else ActivityAction.RenamedConversation(newName)
    return action to conversation.metadata.copyWith(name = newName)
}
