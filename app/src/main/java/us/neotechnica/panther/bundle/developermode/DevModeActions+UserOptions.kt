//
//  DevModeActions+UserOptions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.developermode

import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.userSessionService
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.modules.session.entity.extensions.UserSessionServiceStorageKey
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/** The Developer Mode actions that operate on the current user. */
object UserOptions {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - User Options Action

    /**
     * The action that presents the User Options sheet, ending in a
     * `Back` action that returns to the app-domain sheet.
     */
    val userOptionsAction: DevModeAction
        get() =
            DevModeAction(title = "User Options") {
                scope.launch {
                    val actions =
                        listOf(setCurrentUserIDAction)
                            .map { devModeAction ->
                                Action(
                                    devModeAction.title,
                                    style = if (devModeAction.isDestructive) ActionStyle.DESTRUCTIVE else ActionStyle.DEFAULT,
                                ) { devModeAction.perform() }
                            }.toMutableList()

                    actions.add(Action("Back", style = ActionStyle.CANCEL) { AppActions.presentActionSheet() })

                    ActionSheet(
                        title = "User Options",
                        actions = actions,
                    ).present(translating = emptyList())
                }
            }

    // MARK: - Auxiliary

    private val setCurrentUserIDAction: DevModeAction
        get() =
            DevModeAction(title = "Set Current User ID") {
                scope.launch { setCurrentUserID() }
            }

    private suspend fun setCurrentUserID() {
        val key = PersistentStorageKey.userSessionService(UserSessionServiceStorageKey.CURRENT_USER_ID)
        val currentUserID = Persistent.string(key)

        val input =
            TextInputAlert(
                message = "Set Current User ID",
                attributes =
                    TextFieldAttributes(
                        capitalizationType = KeyboardCapitalization.None,
                        correctionType = TextFieldAttributes.CorrectionType.NO,
                    ),
                confirmButtonTitle = "Done",
            ).present(translating = emptyList())

        val enteredInput = input ?: return
        if (enteredInput != currentUserID) Application.reset()

        Persistent.setString(key, enteredInput)
        DependencyValues.current.navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Splash)))
    }
}
