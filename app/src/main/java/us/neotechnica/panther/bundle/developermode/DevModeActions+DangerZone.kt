//
//  DevModeActions+DangerZone.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.developermode

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.Application
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.modules.common.extensions.ConversationDeletionGranularity
import us.neotechnica.panther.modules.common.extensions.clearPreviousLanguageCodes
import us.neotechnica.panther.modules.common.extensions.deleteConversations
import us.neotechnica.panther.modules.common.extensions.matches
import us.neotechnica.panther.modules.common.extensions.resetPushTokens
import us.neotechnica.panther.modules.content.user.extensions.ignoredConversationIDKeys
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import kotlin.time.Duration.Companion.seconds
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action as AlertKitAction

/** The destructive Developer Mode actions grouped under the Danger Zone. */
object DangerZone {
    // MARK: - Dependencies

    private val sessionStore: SessionStore by Dependency { it.clientSession.store }
    private val userSession: UserSessionService by Dependency { it.clientSession.entity.user }

    // MARK: - Types

    private enum class Action {
        CLEAR_PREVIOUS_LANGUAGE_CODES,
        DELETE_CONVERSATIONS_INVISIBLE_TO_CURRENT_USER,
        DELETE_CURRENT_USER_CONVERSATIONS,
        DELETE_GROUP_CHATS_WITHOUT_NAME_OR_PHOTO,
        DELETE_MRC_CONVERSATIONS,
        DELETE_ONE_TO_ONE_CONVERSATIONS_WITH_FEWER_THAN_FIVE_MESSAGES,
        RESET_PUSH_TOKENS,
        ;

        val confirmationAlertTitle: String
            get() =
                when (this) {
                    CLEAR_PREVIOUS_LANGUAGE_CODES -> "Clear Previous Language Codes"
                    DELETE_CONVERSATIONS_INVISIBLE_TO_CURRENT_USER -> "Delete Conversations Invisible to Current User"
                    DELETE_CURRENT_USER_CONVERSATIONS -> "Delete Current User Conversations"
                    DELETE_GROUP_CHATS_WITHOUT_NAME_OR_PHOTO -> "Delete Group Chats Without Name or Photo"
                    DELETE_MRC_CONVERSATIONS -> "Delete MRC Conversations"
                    DELETE_ONE_TO_ONE_CONVERSATIONS_WITH_FEWER_THAN_FIVE_MESSAGES -> "Delete 1:1 Conversations with <5 Messages"
                    RESET_PUSH_TOKENS -> "Reset Push Tokens"
                }

        val confirmationAlertMessage: String
            get() =
                when (this) {
                    CLEAR_PREVIOUS_LANGUAGE_CODES ->
                        "This will clear all previous language code history for the current user.\n\nThis operation cannot be undone."

                    DELETE_CONVERSATIONS_INVISIBLE_TO_CURRENT_USER ->
                        "This will delete all conversations that are not visible to the current user.\n\nThis operation cannot be undone."

                    DELETE_CURRENT_USER_CONVERSATIONS ->
                        "This will delete all conversations for the current user.\n\nThis operation cannot be undone."

                    DELETE_GROUP_CHATS_WITHOUT_NAME_OR_PHOTO ->
                        "This will delete all group chats without a name or photo attached to their metadata." +
                            "\n\nThis operation cannot be undone."

                    DELETE_MRC_CONVERSATIONS ->
                        "This will delete all Message Recipient Consent-enabled conversations for the current user." +
                            "\n\nThis operation cannot be undone."

                    DELETE_ONE_TO_ONE_CONVERSATIONS_WITH_FEWER_THAN_FIVE_MESSAGES ->
                        "This will delete all 1:1 conversations with fewer than 5 messages.\n\nThis operation cannot be undone."

                    RESET_PUSH_TOKENS ->
                        "This will remove all push tokens for all users in the " +
                            "${Networking.config.environment.description.uppercase()} environment." +
                            "\n\nThis operation cannot be undone."
                }
    }

    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Actions

    /** The action that clears the current user's previous language codes. */
    val clearPreviousLanguageCodesAction: DevModeAction
        get() =
            DevModeAction(title = Action.CLEAR_PREVIOUS_LANGUAGE_CODES.confirmationAlertTitle, isDestructive = true) {
                performAction(Action.CLEAR_PREVIOUS_LANGUAGE_CODES)
            }

    /**
     * The action that presents the conversation deletion sheet, listing
     * each granularity with the number of conversations it covers.
     */
    val deleteConversationsAction: DevModeAction
        get() =
            DevModeAction(title = "Delete Conversations", isDestructive = true) {
                scope.launch { presentDeleteConversationsActionSheet() }
            }

    /** The action that removes every user's push tokens. */
    val resetPushTokensAction: DevModeAction
        get() =
            DevModeAction(title = Action.RESET_PUSH_TOKENS.confirmationAlertTitle, isDestructive = true) {
                performAction(Action.RESET_PUSH_TOKENS)
            }

    // MARK: - Delete Conversations

    private suspend fun presentDeleteConversationsActionSheet() {
        val currentUser = userSession.currentUser
        val ignoredConversationIDKeys = sessionStore.ignoredConversationIDKeys
        val allForCurrentUserCount =
            ((currentUser?.conversationIDs?.map { it.key } ?: emptyList()) + ignoredConversationIDKeys).distinct().size

        val actions =
            mutableListOf(
                DevModeAction(title = "All for Current User ($allForCurrentUserCount)", isDestructive = true) {
                    performAction(Action.DELETE_CURRENT_USER_CONVERSATIONS)
                },
            )

        val conversations = currentUser?.conversations ?: return
        val invisibleToCurrentUserCount =
            (
                conversations
                    .filter { it.matches(ConversationDeletionGranularity.NOT_VISIBLE_FOR_CURRENT_USER) }
                    .map { it.id.key } + ignoredConversationIDKeys
            ).distinct().size
        val groupChatsWithoutNameOrPhotoCount =
            conversations.count { it.matches(ConversationDeletionGranularity.GROUP_CHATS_WITHOUT_NAME_OR_PHOTO) }
        val messageRecipientConsentEnabledCount =
            conversations.count { it.matches(ConversationDeletionGranularity.MESSAGE_RECIPIENT_CONSENT_ENABLED) }
        val oneToOneAndFewerThanFiveMessagesCount =
            conversations.count { it.matches(ConversationDeletionGranularity.ONE_TO_ONE_AND_FEWER_THAN_FIVE_MESSAGES) }

        if (invisibleToCurrentUserCount > 0) {
            actions.add(
                DevModeAction(title = "Not Visible to Current User ($invisibleToCurrentUserCount)", isDestructive = true) {
                    performAction(Action.DELETE_CONVERSATIONS_INVISIBLE_TO_CURRENT_USER)
                },
            )
        }

        if (groupChatsWithoutNameOrPhotoCount > 0) {
            actions.add(
                DevModeAction(title = "Group Chats w/o Name or Photo ($groupChatsWithoutNameOrPhotoCount)", isDestructive = true) {
                    performAction(Action.DELETE_GROUP_CHATS_WITHOUT_NAME_OR_PHOTO)
                },
            )
        }

        if (messageRecipientConsentEnabledCount > 0) {
            actions.add(
                DevModeAction(title = "MRC-enabled Conversations ($messageRecipientConsentEnabledCount)", isDestructive = true) {
                    performAction(Action.DELETE_MRC_CONVERSATIONS)
                },
            )
        }

        if (oneToOneAndFewerThanFiveMessagesCount > 0) {
            actions.add(
                DevModeAction(
                    title = "1:1 Conversations with <5 Messages ($oneToOneAndFewerThanFiveMessagesCount)",
                    isDestructive = true,
                ) { performAction(Action.DELETE_ONE_TO_ONE_CONVERSATIONS_WITH_FEWER_THAN_FIVE_MESSAGES) },
            )
        }

        ActionSheet(
            title = "Delete Conversations",
            message = "Select the granularity of conversations to delete.",
            actions =
                actions.map { devModeAction ->
                    AlertKitAction(
                        devModeAction.title,
                        style = if (devModeAction.isDestructive) ActionStyle.DESTRUCTIVE else ActionStyle.DEFAULT,
                    ) { devModeAction.perform() }
                },
        ).present(translating = emptyList())
    }

    // MARK: - Auxiliary

    private fun performAction(action: Action) {
        scope.launch {
            try {
                performActionOrThrow(action)
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }
    }

    private suspend fun performActionOrThrow(action: Action) {
        fun showSuccessAndReset() {
            HUD.flash(image = HUD.HUDImage.SUCCESS)
            Task.delayed(by = 1.seconds) {
                Application.reset(
                    preserveCurrentUserID = true,
                    onCompletion = Application.ResetCompletionProcedure.NAVIGATE_TO_SPLASH,
                )
            }
        }

        val confirmed =
            ConfirmationAlert(
                title = action.confirmationAlertTitle,
                message = action.confirmationAlertMessage,
                confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
            ).present(translating = emptyList())
        if (!confirmed) return

        when (action) {
            Action.CLEAR_PREVIOUS_LANGUAGE_CODES -> {
                CoreUtilities.clearPreviousLanguageCodes()
                HUD.flash("Cleared Previous Language Codes", HUD.HUDImage.SUCCESS)
            }

            Action.DELETE_CONVERSATIONS_INVISIBLE_TO_CURRENT_USER -> {
                CoreUtilities.deleteConversations(ConversationDeletionGranularity.NOT_VISIBLE_FOR_CURRENT_USER)
                showSuccessAndReset()
            }

            Action.DELETE_CURRENT_USER_CONVERSATIONS -> {
                CoreUtilities.deleteConversations(ConversationDeletionGranularity.ALL_FOR_CURRENT_USER)
                showSuccessAndReset()
            }

            Action.DELETE_GROUP_CHATS_WITHOUT_NAME_OR_PHOTO -> {
                CoreUtilities.deleteConversations(ConversationDeletionGranularity.GROUP_CHATS_WITHOUT_NAME_OR_PHOTO)
                showSuccessAndReset()
            }

            Action.DELETE_MRC_CONVERSATIONS -> {
                CoreUtilities.deleteConversations(ConversationDeletionGranularity.MESSAGE_RECIPIENT_CONSENT_ENABLED)
                showSuccessAndReset()
            }

            Action.DELETE_ONE_TO_ONE_CONVERSATIONS_WITH_FEWER_THAN_FIVE_MESSAGES -> {
                CoreUtilities.deleteConversations(ConversationDeletionGranularity.ONE_TO_ONE_AND_FEWER_THAN_FIVE_MESSAGES)
                showSuccessAndReset()
            }

            Action.RESET_PUSH_TOKENS -> {
                CoreUtilities.resetPushTokens()
                HUD.flash("Reset Push Tokens", HUD.HUDImage.SUCCESS)
            }
        }
    }
}
