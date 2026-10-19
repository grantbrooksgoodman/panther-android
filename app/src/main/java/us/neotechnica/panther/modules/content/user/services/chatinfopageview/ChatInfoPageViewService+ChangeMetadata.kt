//
//  ChatInfoPageViewService+ChangeMetadata.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatinfopageview

import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.conversation.models.copyWith
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action as AlertKitAction

// MARK: - Present Change Metadata Action Sheet

/**
 * Presents an action sheet for changing the conversation's name and
 * photo.
 *
 * Choosing to change the name presents a text input alert – names
 * containing reserved characters are rejected; choosing to change the
 * photo offers the camera or the photo library. The remove photo
 * option appears only when the conversation has a photo.
 *
 * @return The change the user chose; otherwise, `null` if they
 *   canceled or made no change.
 */
suspend fun ChatInfoPageViewService.presentChangeMetadataActionSheet(): ChatInfoPageViewService.MetadataChangeType? {
    val conversation = DependencyValues.current.clientSession.entity.conversation.currentConversation

    var choice: MetadataChoice? = null
    val actions =
        mutableListOf(
            AlertKitAction("Change name") { choice = MetadataChoice.CHANGE_NAME },
            AlertKitAction("Change photo") { choice = MetadataChoice.CHANGE_PHOTO },
        )
    if (conversation?.metadata?.imageData != null) {
        actions.add(
            AlertKitAction("Remove photo", style = ActionStyle.DESTRUCTIVE) { choice = MetadataChoice.REMOVE_PHOTO },
        )
    }

    ActionSheet(
        actions = actions,
        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
    ).present(translating = listOf(ActionSheet.TranslationOptionKey.Actions()))

    return when (choice) {
        MetadataChoice.CHANGE_NAME -> presentChangeNameAlert(conversation)

        MetadataChoice.CHANGE_PHOTO -> presentChangePhotoAlert()

        MetadataChoice.REMOVE_PHOTO ->
            conversation?.let {
                ChatInfoPageViewService.MetadataChangeType.RemovePhoto(it.metadata.copyWith(nilImageData = true))
            }

        null -> null
    }
}

// MARK: - Auxiliary

// A choice made from the change-metadata action sheet.
private enum class MetadataChoice {
    CHANGE_NAME,
    CHANGE_PHOTO,
    REMOVE_PHOTO,
}

// Presents the change-name text input alert, disabling its confirm
// button while the field contains reserved characters, and returns the
// resulting metadata change, or `null` when canceled or unchanged.
private suspend fun presentChangeNameAlert(conversation: Conversation?): ChatInfoPageViewService.MetadataChangeType? {
    val conversation = conversation ?: return null
    val conversationName = conversation.metadata.name.takeUnless { it.isBangQualifiedEmpty }
    val textInputAlert =
        TextInputAlert(
            message = "Choose a new name for this conversation:",
            attributes =
                TextFieldAttributes(
                    clearButtonMode = TextFieldAttributes.ClearButtonMode.ALWAYS,
                    sampleText = conversationName,
                ),
            cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            confirmButtonTitle = LocalizedStringKey.Done.localized(),
        )

    textInputAlert.onTextFieldChange { text ->
        if (text?.any { it in "⌘:" } == true) {
            textInputAlert.disableAction(1)
        } else {
            textInputAlert.enableAction(1)
        }
    }

    val name = textInputAlert.present(translating = listOf(TextInputAlert.TranslationOptionKey.Message))
    if (conversation == null || name == null) return null
    if (name == conversation.metadata.name) return null
    if (name.isBangQualifiedEmpty && conversation.metadata.name.isBangQualifiedEmpty) return null

    val sanitizedName = if (name.isBangQualifiedEmpty) BANG_QUALIFIED_EMPTY else name
    return ChatInfoPageViewService.MetadataChangeType.Name(conversation.metadata.copyWith(name = sanitizedName.trim()))
}

// Presents the change-photo action sheet, offering the camera or the
// photo library, and returns the chosen change, or `null` if canceled.
private suspend fun presentChangePhotoAlert(): ChatInfoPageViewService.MetadataChangeType? {
    var photoChangeType: ChatInfoPageViewService.MetadataChangeType? = null
    val takePhotoAction =
        AlertKitAction("Take photo") {
            photoChangeType = ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromCamera
        }
    val chooseFromLibraryAction =
        AlertKitAction("Choose photo from library") {
            photoChangeType = ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromLibrary
        }

    ActionSheet(
        actions = listOf(takePhotoAction, chooseFromLibraryAction),
        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
    ).present(translating = listOf(ActionSheet.TranslationOptionKey.Actions()))
    return photoChangeType
}
