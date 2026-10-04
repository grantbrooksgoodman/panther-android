//
//  DevModeActions+DangerZone.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.developermode

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.modules.common.extensions.ConversationDeletionGranularity
import us.neotechnica.panther.modules.common.extensions.clearPreviousLanguageCodes
import us.neotechnica.panther.modules.common.extensions.deleteConversations
import us.neotechnica.panther.modules.common.extensions.matches
import us.neotechnica.panther.modules.common.extensions.resetPushTokens
import us.neotechnica.panther.modules.session.entity.extensions.conversations
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import kotlin.time.Duration.Companion.seconds

/** The destructive Developer Mode actions grouped under the Danger Zone. */
object DangerZone {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Actions

    val clearPreviousLanguageCodesAction: DevModeAction =
        DevModeAction(title = "Clear Previous Language Codes", isDestructive = true) {
            scope.launch {
                if (!confirm("Clear Previous Language Codes", CLEAR_PREVIOUS_LANGUAGE_CODES_MESSAGE)) return@launch
                runCatching { CoreUtilities.clearPreviousLanguageCodes() }
                    .onSuccess { HUD.flash("Cleared Previous Language Codes", HUD.HUDImage.SUCCESS) }
                    .onFailure { Logger.log(it.toException(), with = AlertType.toast) }
            }
        }

    val deleteConversationsAction: DevModeAction =
        DevModeAction(title = "Delete Conversations", isDestructive = true) {
            scope.launch { presentDeleteConversationsSheet() }
        }

    val resetPushTokensAction: DevModeAction =
        DevModeAction(title = "Reset Push Tokens", isDestructive = true) {
            scope.launch {
                if (!confirm("Reset Push Tokens", RESET_PUSH_TOKENS_MESSAGE)) return@launch
                runCatching { CoreUtilities.resetPushTokens() }
                    .onSuccess { HUD.flash("Reset Push Tokens", HUD.HUDImage.SUCCESS) }
                    .onFailure { Logger.log(it.toException(), with = AlertType.toast) }
            }
        }

    val dangerZoneAction: DevModeAction =
        DevModeAction(title = "Danger Zone", isDestructive = true) {
            scope.launch {
                val currentUser = UserSessionService.currentUser
                val actions = mutableListOf<Action>()

                if (currentUser?.previousLanguageCodes?.isNotEmpty() == true) {
                    actions.add(clearPreviousLanguageCodesAction.toAction())
                }
                if (currentUser?.conversations != null) {
                    actions.add(deleteConversationsAction.toAction())
                }
                actions.add(resetPushTokensAction.toAction())

                ActionSheetAlert(
                    title = "Danger Zone",
                    message = "Exercise caution when using these options.",
                    actions = actions,
                ).present(translating = emptyList())
            }
        }

    // MARK: - Auxiliary

    private suspend fun presentDeleteConversationsSheet() {
        val conversations = UserSessionService.currentUser?.conversations ?: return
        val actions = mutableListOf<Action>()

        actions.add(
            Action("All for Current User (${conversations.size})", style = ActionStyle.DESTRUCTIVE) {
                scope.launch { performDeletion(ConversationDeletionGranularity.ALL_FOR_CURRENT_USER) }
            },
        )

        val notVisibleCount = conversations.count { it.matches(ConversationDeletionGranularity.NOT_VISIBLE_FOR_CURRENT_USER) }
        if (notVisibleCount > 0) {
            actions.add(
                Action("Not Visible to Current User ($notVisibleCount)", style = ActionStyle.DESTRUCTIVE) {
                    scope.launch { performDeletion(ConversationDeletionGranularity.NOT_VISIBLE_FOR_CURRENT_USER) }
                },
            )
        }

        val groupCount = conversations.count { it.matches(ConversationDeletionGranularity.GROUP_CHATS_WITHOUT_NAME_OR_PHOTO) }
        if (groupCount > 0) {
            actions.add(
                Action("Group Chats w/o Name or Photo ($groupCount)", style = ActionStyle.DESTRUCTIVE) {
                    scope.launch { performDeletion(ConversationDeletionGranularity.GROUP_CHATS_WITHOUT_NAME_OR_PHOTO) }
                },
            )
        }

        val oneToOneCount = conversations.count { it.matches(ConversationDeletionGranularity.ONE_TO_ONE_AND_FEWER_THAN_FIVE_MESSAGES) }
        if (oneToOneCount > 0) {
            actions.add(
                Action("1:1 Conversations with <5 Messages ($oneToOneCount)", style = ActionStyle.DESTRUCTIVE) {
                    scope.launch { performDeletion(ConversationDeletionGranularity.ONE_TO_ONE_AND_FEWER_THAN_FIVE_MESSAGES) }
                },
            )
        }

        ActionSheetAlert(
            title = "Delete Conversations",
            message = "Select the granularity of conversations to delete.",
            actions = actions,
        ).present(translating = emptyList())
    }

    private suspend fun performDeletion(granularity: ConversationDeletionGranularity) {
        if (!confirm(granularity.confirmationTitle, granularity.confirmationMessage)) return
        try {
            CoreUtilities.deleteConversations(granularity)
        } catch (exception: Exception) {
            Logger.log(exception, with = AlertType.toast)
            return
        }

        HUD.flash(image = HUD.HUDImage.SUCCESS)
        Task.delayed(by = 1.seconds) {
            Application.reset(
                preserveCurrentUserID = true,
                onCompletion = Application.ResetCompletionProcedure.NAVIGATE_TO_SPLASH,
            )
        }
    }

    private suspend fun confirm(
        title: String,
        message: String,
    ): Boolean =
        ConfirmationAlert(
            title = title,
            message = message,
            confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
        ).present(translating = emptyList())

    private fun DevModeAction.toAction(): Action =
        Action(title, style = if (isDestructive) ActionStyle.DESTRUCTIVE else ActionStyle.DEFAULT) { perform() }

    private fun Throwable.toException(): Exception = this as? Exception ?: Exception.from(this, ExceptionMetadata(this))

    private val ConversationDeletionGranularity.confirmationMessage: String
        get() =
            when (this) {
                ConversationDeletionGranularity.ALL_FOR_CURRENT_USER ->
                    "This will delete all conversations for the current user.\n\nThis operation cannot be undone."

                ConversationDeletionGranularity.GROUP_CHATS_WITHOUT_NAME_OR_PHOTO ->
                    "This will delete all group chats without a name or photo attached to their metadata." +
                        "\n\nThis operation cannot be undone."

                ConversationDeletionGranularity.NOT_VISIBLE_FOR_CURRENT_USER ->
                    "This will delete all conversations that are not visible to the current user." +
                        "\n\nThis operation cannot be undone."

                ConversationDeletionGranularity.ONE_TO_ONE_AND_FEWER_THAN_FIVE_MESSAGES ->
                    "This will delete all 1:1 conversations with fewer than 5 messages.\n\nThis operation cannot be undone."
            }

    private val ConversationDeletionGranularity.confirmationTitle: String
        get() =
            when (this) {
                ConversationDeletionGranularity.ALL_FOR_CURRENT_USER -> "Delete Current User Conversations"
                ConversationDeletionGranularity.GROUP_CHATS_WITHOUT_NAME_OR_PHOTO -> "Delete Group Chats Without Name or Photo"
                ConversationDeletionGranularity.NOT_VISIBLE_FOR_CURRENT_USER -> "Delete Conversations Invisible to Current User"
                ConversationDeletionGranularity.ONE_TO_ONE_AND_FEWER_THAN_FIVE_MESSAGES -> "Delete 1:1 Conversations with <5 Messages"
            }

    // MARK: - Companion

    private const val CLEAR_PREVIOUS_LANGUAGE_CODES_MESSAGE =
        "This will clear all previous language code history for the current user.\n\nThis operation cannot be undone."
    private const val RESET_PUSH_TOKENS_MESSAGE =
        "This will remove all push tokens for all users in the current environment.\n\nThis operation cannot be undone."
}
