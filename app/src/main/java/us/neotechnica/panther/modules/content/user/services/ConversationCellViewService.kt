//
//  ConversationCellViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.Application
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.bundle.shouldNotifyOfConversationAvailability
import us.neotechnica.panther.bundle.userSessionService
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.foundation.extensions.cancelAction
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.extensions.UserSessionServiceStorageKey
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.extensions.languageExonym
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.localized

/**
 * Handles conversation cell interactions requiring presentation or
 * session work.
 *
 * [ConversationCellReducer][us.neotechnica.panther.modules.content.user.components.conversationcellview.ConversationCellReducer]
 * delegates to this service for blocking, reporting, and deletion
 * confirmation.
 *
 * Resolve the service through
 * [DependencyValues.conversationCellViewService][us.neotechnica.panther.modules.content.user.dependencies.conversationCellViewService].
 *
 * @param dependencies The collaborators the service resolves its work
 *   through. The default resolves the live services.
 */
class ConversationCellViewService(
    private val dependencies: Dependencies = Dependencies(),
) {
    // MARK: - Types

    /**
     * The collaborators the service resolves its work through.
     *
     * Each property defaults to the live service; a test replaces the
     * ones it exercises.
     *
     * @property blockUsers Blocks the users in the given conversation.
     * @property isDeveloperModeEnabled Returns whether developer mode
     *   is enabled.
     * @property localizedRegionName Returns the localized name of the
     *   region with the given code.
     * @property reportUsers Reports the users in the given
     *   conversation.
     */
    data class Dependencies(
        val blockUsers: suspend (Conversation) -> Unit = { conversation ->
            DependencyValues.current.clientSession.entity.moderation
                .blockUsers(inConversation = conversation)
        },
        val isDeveloperModeEnabled: () -> Boolean = { Build.isDeveloperModeEnabled },
        val localizedRegionName: (String) -> String = { regionCode ->
            DependencyValues.current.commonServices.regionDetail
                .localizedRegionName(regionCode)
        },
        val reportUsers: suspend (Conversation) -> Unit = { conversation ->
            DependencyValues.current.clientSession.entity.moderation
                .reportUsers(inConversation = conversation)
        },
    )

    // MARK: - Properties

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Methods

    /**
     * Begins the block users flow for the given conversation.
     *
     * @param conversation The conversation whose users to block.
     *
     * @throws Exception if the operation fails.
     */
    suspend fun blockUsersButtonTapped(conversation: Conversation) {
        dependencies.blockUsers(conversation)
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
        var cancelled = true
        val deleteAction =
            Action(
                "Delete",
                style = ActionStyle.DESTRUCTIVE,
            ) { cancelled = false }

        ActionSheet(
            title = title,
            message = "Are you sure you'd like to delete this conversation?\nThis operation cannot be undone.",
            actions = listOf(deleteAction),
            cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
        ).present(
            translating =
                listOf(
                    ActionSheet.TranslationOptionKey.Actions(),
                    ActionSheet.TranslationOptionKey.Message,
                ),
        )

        return cancelled
    }

    /**
     * Presents an alert with information about the given user.
     *
     * The alert shows the user's language and region. In developer
     * mode, an additional action switches the current account to the
     * given user, resetting the app and returning to the splash page.
     *
     * @param user The user the alert describes.
     */
    fun presentUserInfoAlert(user: User) {
        serviceScope.launch {
            val languageName =
                user.languageCode.languageExonym?.let { "$it (${user.languageCode.uppercase()})" }
                    ?: user.languageCode.uppercase()
            val regionName = dependencies.localizedRegionName(user.phoneNumber.regionCode)
            val alertMessage =
                "${LocalizedStringKey.Language.localized()}: $languageName\n" +
                    "${LocalizedStringKey.Region.localized()}: $regionName"

            val actions = mutableListOf(Action.cancelAction(title = LocalizedStringKey.Dismiss.localized()))
            if (dependencies.isDeveloperModeEnabled()) {
                actions.add(Action("Set to Current User", style = ActionStyle.PREFERRED) { setToCurrentUser(user) })
            }

            Alert(
                title = user.displayName,
                message = alertMessage,
                actions = actions,
            ).present(translating = emptyList())
        }
    }

    /**
     * Begins the report users flow for the given conversation.
     *
     * @param conversation The conversation whose users to report.
     *
     * @throws Exception if the operation fails.
     */
    suspend fun reportUsersButtonTapped(conversation: Conversation) {
        dependencies.reportUsers(conversation)
    }

    // MARK: - Auxiliary

    private fun setToCurrentUser(user: User) {
        RuntimeStorage.store(false, StoredItemKey.shouldNotifyOfConversationAvailability)

        Application.reset()
        Application.dismissSheets()

        Persistent.setString(
            PersistentStorageKey.userSessionService(UserSessionServiceStorageKey.CURRENT_USER_ID),
            user.id,
        )

        val navigation = DependencyValues.current.navigation
        navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
        navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Splash)))
    }
}
