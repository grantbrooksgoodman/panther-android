//
//  ChatInfoPageViewService+ChangeMetadata.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action as AlertKitAction

/**
 * Presents the change-metadata action sheet for the conversation's
 * name and photo, returning the change the user chose.
 *
 * Choosing to change the name presents a text input alert; choosing
 * to change the photo offers the camera or the photo library. The
 * remove-photo option appears only when the conversation has a photo.
 *
 * @return The chosen metadata change, or `null` if the sheet was
 *   canceled.
 */
internal suspend fun ChatInfoPageViewService.presentChangeMetadataActionSheet(): ChatInfoPageViewService.MetadataChangeType? {
    val conversation = ConversationSessionService.currentConversation ?: return null

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

    return when (choice) {
        MetadataChoice.CHANGE_NAME -> presentChangeNameAlert(conversation)
        MetadataChoice.CHANGE_PHOTO -> presentPhotoSourceSheet()
        MetadataChoice.REMOVE_PHOTO ->
            ChatInfoPageViewService.MetadataChangeType.RemovePhoto(
                conversation.metadata.copyWith(imageData = null, imageHash = null),
            )
        null -> null
    }
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
 * photo library, and returns the chosen change, or `null` if canceled.
 */
private suspend fun presentPhotoSourceSheet(): ChatInfoPageViewService.MetadataChangeType? {
    var choice: ChatInfoPageViewService.MetadataChangeType? = null
    ActionSheetAlert(
        title = "Change name and photo",
        actions =
            listOf(
                AlertKitAction("Take photo") { choice = ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromCamera },
                AlertKitAction("Choose photo from library") {
                    choice = ChatInfoPageViewService.MetadataChangeType.SelectPhotoFromLibrary
                },
            ),
        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
    ).present(
        translating =
            listOf(
                ActionSheetAlert.TranslationOptionKey.Actions(),
                ActionSheetAlert.TranslationOptionKey.Title,
            ),
    )
    return choice
}

/**
 * Presents the change-name text input alert, live-disabling the
 * confirm button while the field contains reserved characters, and
 * returns the resulting metadata change, or `null` when canceled or a
 * no-op.
 */
private suspend fun presentChangeNameAlert(conversation: Conversation): ChatInfoPageViewService.MetadataChangeType? {
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
        ).present(translating = listOf(TextInputAlert.TranslationOptionKey.Message)) ?: return null

    if (input.any { it in "⌘:" }) return null
    if (input == conversation.metadata.name) return null
    if (input.isBangQualifiedEmpty && conversation.metadata.name.isBangQualifiedEmpty) return null

    val trimmed = input.trim()
    val newName = if (trimmed.isBangQualifiedEmpty) BANG_QUALIFIED_EMPTY else trimmed
    return ChatInfoPageViewService.MetadataChangeType.Name(conversation.metadata.copyWith(name = newName))
}
