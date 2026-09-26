//
//  ModerationSessionService+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.content.user.models.ModerationType
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.entity.services.ModerationSessionService
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

// MARK: - Types

private data class ContactEntry(
    val name: String,
    val userID: String,
)

private data class ModerationAlertData(
    val actions: List<Action>,
    val translationOptionKeys: List<ActionSheetAlert.TranslationOptionKey>,
)

// MARK: - Properties

private val moderationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

// MARK: - Content Moderation

/**
 * Prompts the user to block one or more participants in the given
 * conversation.
 *
 * @param inConversation The conversation whose participants may be
 *   blocked.
 *
 * @throws Exception if blocking fails.
 */
suspend fun ModerationSessionService.blockUsers(inConversation: Conversation): Unit = moderate(ModerationType.BLOCK, inConversation)

/**
 * Prompts the user to report one or more participants in the given
 * conversation.
 *
 * @param inConversation The conversation whose participants may be
 *   reported.
 *
 * @throws Exception if reporting fails.
 */
suspend fun ModerationSessionService.reportUsers(inConversation: Conversation): Unit = moderate(ModerationType.REPORT, inConversation)

// MARK: - Auxiliary

private suspend fun moderate(
    type: ModerationType,
    conversation: Conversation,
) {
    val users =
        conversation.users
            ?: throw Exception("No data source provided.", metadata = ExceptionMetadata(ModerationSessionService))

    runCatching { ContactService.syncIfNeeded() }.onFailure { Logger.log("$it") }

    val entries =
        users
            .map { ContactEntry(ContactService.match(it.id)?.fullName ?: it.phoneNumber.formattedString(), it.id) }
            .sortedBy { it.name }
            .distinctBy { it.userID }

    if (entries.size <= 1) {
        val entry = entries.firstOrNull() ?: return
        if (!confirmModeration(type, "⌘${entry.name}⌘")) return
        performModeration(type, listOf(entry.userID))
        HUD.showSuccess()
        return
    }

    val alertData = alertData(type, entries)
    ActionSheetAlert(
        title = "${type.uppercasedRawValue()} Users",
        actions = alertData.actions,
        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
    ).present(translating = alertData.translationOptionKeys)
}

private fun alertData(
    type: ModerationType,
    entries: List<ContactEntry>,
): ModerationAlertData {
    val actions =
        entries
            .map { entry ->
                Action(entry.name) {
                    moderationScope.launch { confirmAndPerform(type, "⌘${entry.name}⌘", listOf(entry.userID)) }
                }
            }.toMutableList()

    val allUsersAction =
        Action("${type.uppercasedRawValue()} All Users", style = ActionStyle.DESTRUCTIVE) {
            moderationScope.launch { confirmAndPerform(type, "All Users", entries.map { it.userID }) }
        }
    actions.add(allUsersAction)

    return ModerationAlertData(
        actions = actions,
        translationOptionKeys =
            listOf(
                ActionSheetAlert.TranslationOptionKey.Actions(listOf(allUsersAction)),
                ActionSheetAlert.TranslationOptionKey.Message,
                ActionSheetAlert.TranslationOptionKey.Title,
            ),
    )
}

private suspend fun confirmAndPerform(
    type: ModerationType,
    title: String,
    userIDs: List<String>,
) {
    if (!confirmModeration(type, title)) return
    try {
        performModeration(type, userIDs)
        HUD.showSuccess()
    } catch (exception: Exception) {
        Logger.log(exception, with = AlertType.toast)
    }
}

private suspend fun confirmModeration(
    type: ModerationType,
    title: String,
): Boolean {
    val defaultTitle = "${type.uppercasedRawValue()} $title"
    val message = if (title == "All Users") type.allUsersConfirmationMessage else type.singleUserConfirmationMessage
    return ConfirmationAlert(
        title = if (message == defaultTitle) null else defaultTitle,
        message = message,
        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
        confirmButtonTitle = type.uppercasedRawValue(),
        confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
    ).present(
        translating =
            listOf(
                ConfirmationAlert.TranslationOptionKey.ConfirmButtonTitle,
                ConfirmationAlert.TranslationOptionKey.Message,
                ConfirmationAlert.TranslationOptionKey.Title,
            ),
    )
}

private suspend fun performModeration(
    type: ModerationType,
    userIDs: List<String>,
) {
    when (type) {
        ModerationType.BLOCK -> ModerationSessionService.blockUsers(userIDs)
        ModerationType.REPORT -> ModerationSessionService.reportUsers(userIDs)
        ModerationType.UNBLOCK -> ModerationSessionService.unblockUsers(userIDs)
    }
}

private fun ModerationType.uppercasedRawValue(): String = rawValue.replaceFirstChar { it.uppercase() }
