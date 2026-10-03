//
//  ModerationSessionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.syncIfNeeded
import us.neotechnica.panther.modules.content.user.extensions.userIDs
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.update
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.entity.models.ModerationType
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

/**
 * Blocks, unblocks, and reports users.
 *
 * Blocking updates the current user's blocked user identifiers;
 * reporting increments a per-user counter in a transaction. Each
 * action prompts for confirmation, and actions targeting more than one
 * user are presented as an action sheet.
 */
object ModerationSessionService {
    // MARK: - Types

    private data class ModerationAlertData(
        val actions: List<Action>,
        val translationOptionKeys: List<ActionSheetAlert.TranslationOptionKey>,
    )

    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate
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
    suspend fun blockUsers(inConversation: Conversation) {
        moderate(ModerationType.BLOCK, inConversation, null)
    }

    /**
     * Prompts the user to report one or more participants in the given
     * conversation.
     *
     * @param inConversation The conversation whose participants may be
     *   reported.
     *
     * @throws Exception if reporting fails.
     */
    suspend fun reportUsers(inConversation: Conversation) {
        moderate(ModerationType.REPORT, inConversation, null)
    }

    /**
     * Prompts the user to unblock one or more of their blocked users.
     *
     * @throws Exception if the blocked users cannot be resolved or
     *   unblocking fails.
     */
    suspend fun unblockUsers() {
        moderate(ModerationType.UNBLOCK, null, getBlockedUsers())
    }

    // MARK: - Auxiliary

    private suspend fun moderate(
        type: ModerationType,
        conversation: Conversation?,
        users: List<User>?,
    ) {
        val resolvedUsers =
            conversation?.users ?: users
                ?: throw Exception("No data source provided.", metadata = ExceptionMetadata(this))

        runCatching { ContactService.syncIfNeeded() }.onFailure { Logger.log("$it") }

        val contactPairs =
            resolvedUsers
                .map { it.contactPair ?: ContactPair.withUser(it) }
                .sortedBy { it.contact.fullName }
                .distinct()

        if (contactPairs.size <= 1 && type != ModerationType.UNBLOCK) {
            val contactPair = contactPairs.firstOrNull() ?: return
            if (!confirmModeration(type, "⌘${contactPair.contact.fullName}⌘")) return
            performModeration(type, contactPair.userIDs)
            HUD.showSuccess()
            return
        }

        val alertData = alertData(type, contactPairs)
        ActionSheetAlert(
            title = "${type.firstUppercase()} Users",
            actions = alertData.actions,
            cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
        ).present(translating = alertData.translationOptionKeys)
    }

    private fun alertData(
        type: ModerationType,
        contactPairs: List<ContactPair>,
    ): ModerationAlertData {
        val actions =
            contactPairs
                .map { contactPair ->
                    Action(contactPair.contact.fullName) {
                        moderationScope.launch {
                            confirmAndPerform(type, "⌘${contactPair.contact.fullName}⌘", contactPair.userIDs)
                        }
                    }
                }.toMutableList()

        val allUsersAction =
            Action("${type.firstUppercase()} All Users", style = ActionStyle.DESTRUCTIVE) {
                moderationScope.launch { confirmAndPerform(type, "All Users", contactPairs.userIDs) }
            }

        val translationOptionKeys =
            listOf(
                ActionSheetAlert.TranslationOptionKey.Actions(listOf(allUsersAction)),
                ActionSheetAlert.TranslationOptionKey.Message,
                ActionSheetAlert.TranslationOptionKey.Title,
            )

        if (actions.size <= 1) return ModerationAlertData(actions, translationOptionKeys)

        actions.add(allUsersAction)
        return ModerationAlertData(actions, translationOptionKeys)
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
        val defaultTitle = "${type.firstUppercase()} $title"
        var message = if (title == "All Users") type.allUsersConfirmationMessage else type.singleUserConfirmationMessage
        if (type == ModerationType.UNBLOCK) message = defaultTitle
        return ConfirmationAlert(
            title = if (message == defaultTitle) null else defaultTitle,
            message = message,
            cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            confirmButtonTitle = type.firstUppercase(),
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

    private suspend fun getBlockedUsers(): List<User> {
        val currentUserID =
            User.currentUserID
                ?: throw Exception("Current user ID has not been set.", metadata = ExceptionMetadata(this))

        val path = listOf(NetworkPath.users.rawValue, currentUserID, BLOCKED_USER_IDS_KEY).joinToString("/")
        val rawValue: Map<String, Any?> = database.getValues(path, cacheStrategy = CacheStrategy.ADAPTIVE)
        return UserService.getUsers(rawValue.keys.toList())
    }

    private suspend fun performModeration(
        type: ModerationType,
        userIDs: List<String>,
    ) {
        val filteredUserIDs = userIDs.filter { !it.isBangQualifiedEmpty }
        if (filteredUserIDs.isBangQualifiedEmpty) {
            throw Exception("No user IDs provided.", metadata = ExceptionMetadata(this))
        }

        when (type) {
            ModerationType.BLOCK -> blockUsers(filteredUserIDs)
            ModerationType.REPORT -> reportUsers(filteredUserIDs)
            ModerationType.UNBLOCK -> unblockUsers(filteredUserIDs)
        }
    }

    internal suspend fun blockUsers(userIDs: List<String>) {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        val blockedUserIDs =
            ((currentUser.blockedUserIDs ?: emptyList()) + userIDs)
                .filter { it != BANG_QUALIFIED_EMPTY }
                .distinct()

        currentUser.update(
            UserUpdatableKey.BLOCKED_USER_IDS,
            to = if (blockedUserIDs.isBangQualifiedEmpty) bangQualifiedEmptyList else blockedUserIDs,
        )
    }

    internal suspend fun reportUsers(userIDs: List<String>) {
        database.runTransaction(NetworkPath.reportedUsers.rawValue) { current ->
            @Suppress("UNCHECKED_CAST")
            val reportedUserIDs = (current as? Map<String, Any?>).orEmpty().toMutableMap()
            for (userID in userIDs) {
                reportedUserIDs[userID] = ((reportedUserIDs[userID] as? Number)?.toInt() ?: 0) + 1
            }
            reportedUserIDs
        }
    }

    internal suspend fun unblockUsers(userIDs: List<String>) {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        val blockedUserIDs =
            (currentUser.blockedUserIDs ?: emptyList())
                .filter { it !in userIDs }
                .filter { it != BANG_QUALIFIED_EMPTY }
                .distinct()

        currentUser.update(
            UserUpdatableKey.BLOCKED_USER_IDS,
            to = if (blockedUserIDs.isBangQualifiedEmpty) bangQualifiedEmptyList else blockedUserIDs,
        )

        DependencyValues.current.sharedEvents.traitCollectionChanged.send(Unit)
    }

    private fun ModerationType.firstUppercase(): String = rawValue.replaceFirstChar { it.uppercase() }

    // MARK: - Companion

    private const val BLOCKED_USER_IDS_KEY = "blockedUserIDs"
}
