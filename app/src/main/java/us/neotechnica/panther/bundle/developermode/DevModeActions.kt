//
//  DevModeActions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
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
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.designsystem.modules.developermode.interfaces.DevModeAppActionDelegate
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.modules.session.entity.extensions.UserSessionServiceStorageKey
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import us.neotechnica.panther.subsystem.modules.shared.models.isForcedUpdateRequired

/**
 * The delegate that supplies app-specific actions to the Developer Mode
 * menu.
 */
object AppDevModeActions : DevModeAppActionDelegate {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Computed Properties

    override val appActions: List<DevModeAction>
        get() = listOf(uiOptionsAction, userOptionsAction, DangerZone.dangerZoneAction)

    private val uiOptionsAction: DevModeAction
        get() =
            DevModeAction(title = "UI Options") {
                scope.launch {
                    ActionSheetAlert(
                        title = "UI Options",
                        actions = listOf(Action("Trigger Forced Update Modal") { triggerForcedUpdateModal() }),
                    ).present(translating = emptyList())
                }
            }

    private val userOptionsAction: DevModeAction
        get() =
            DevModeAction(title = "User Options") {
                scope.launch {
                    ActionSheetAlert(
                        title = "User Options",
                        actions = listOf(Action("Set Current User ID") { scope.launch { setCurrentUserID() } }),
                    ).present(translating = emptyList())
                }
            }

    // MARK: - Auxiliary

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
        DependencyValues.current.navigation.navigate(Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash)))
    }

    private fun triggerForcedUpdateModal() {
        SharedState { it.isForcedUpdateRequired }.wrappedValue = true
    }
}
