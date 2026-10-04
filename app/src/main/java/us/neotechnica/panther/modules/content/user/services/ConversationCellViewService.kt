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
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.shouldNotifyOfConversationAvailability
import us.neotechnica.panther.bundle.userSessionService
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.UserSessionServiceStorageKey
import us.neotechnica.panther.modules.session.entity.services.ModerationSessionService
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

/**
 * Handles conversation cell interactions requiring presentation or
 * session work.
 *
 * [ConversationCellReducer][us.neotechnica.panther.modules.content.user.components.conversationcellview.ConversationCellReducer]
 * delegates to this service for blocking, reporting, and deletion
 * confirmation.
 */
object ConversationCellViewService {
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
                LocalizedStringResolver.languageDisplayNames()[user.languageCode.lowercase()]?.let {
                    "$it (${user.languageCode.uppercase()})"
                } ?: user.languageCode.uppercase()
            val regionName = RegionDetailService.localizedRegionName(user.phoneNumber.regionCode)
            val alertMessage =
                "${LocalizedStringKey.Language.localized()}: $languageName\n" +
                    "${LocalizedStringKey.Region.localized()}: $regionName"

            val actions =
                if (Build.isDeveloperModeEnabled) {
                    listOf(Action("Set to Current User", style = ActionStyle.PREFERRED) { setToCurrentUser(user) })
                } else {
                    emptyList()
                }

            ActionSheetAlert(
                title = user.displayName,
                message = alertMessage,
                actions = actions,
                cancelButtonTitle = LocalizedStringKey.Dismiss.localized(),
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
        ModerationSessionService.reportUsers(inConversation = conversation)
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
        navigation.navigate(Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash)))
    }
}
