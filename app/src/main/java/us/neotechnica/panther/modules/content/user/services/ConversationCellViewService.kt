//
//  ConversationCellViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.modules.content.user.extensions.blockUsers
import us.neotechnica.panther.modules.content.user.extensions.reportUsers
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.entity.services.ModerationSessionService

/**
 * Handles conversation cell interactions requiring presentation or
 * session work.
 *
 * [ConversationCellReducer][us.neotechnica.panther.modules.content.user.components.conversationcellview.ConversationCellReducer]
 * delegates to this service for blocking, reporting, and deletion
 * confirmation.
 */
object ConversationCellViewService {
    // MARK: - Methods

    /**
     * Begins the block users flow for the given conversation.
     *
     * @param conversation The conversation whose users to block.
     *
     * @throws Exception if the operation fails.
     */
    suspend fun blockUsersButtonTapped(conversation: Conversation) {
        ModerationSessionService.blockUsers(inConversation = conversation)
    }

    /**
     * Presents the deletion confirmation action sheet for a conversation.
     *
     * @param title The conversation title the action sheet displays.
     *
     * @return `true` if the user cancelled the deletion; otherwise,
     *   `false`.
     */
    suspend fun presentDeletionActionSheet(title: String): Boolean {
        val confirmed =
            ActionSheetAlert(
                title = title,
                message = "Are you sure you'd like to delete this conversation?\nThis operation cannot be undone.",
                actions = listOf(Action("Delete", style = ActionStyle.DESTRUCTIVE) {}),
                cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            ).present(
                translating =
                    listOf(
                        ActionSheetAlert.TranslationOptionKey.Actions(),
                        ActionSheetAlert.TranslationOptionKey.Message,
                    ),
            )
        return !confirmed
    }

    /**
     * Begins the report users flow for the given conversation.
     *
     * @param conversation The conversation whose users to report.
     *
     * @throws Exception if the operation fails.
     */
    suspend fun reportUsersButtonTapped(conversation: Conversation) {
        ModerationSessionService.reportUsers(inConversation = conversation)
    }
}
