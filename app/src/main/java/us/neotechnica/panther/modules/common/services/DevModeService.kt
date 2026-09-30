//
//  DevModeService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.services.Build

/**
 * Presents the password-protected prompt that enables or disables
 * developer mode.
 *
 * Use [DevModeService] to toggle developer mode from the settings
 * page. Enabling requires entering the build's expiration override
 * code; disabling requires a destructive confirmation. Toggling has
 * no effect on general-release builds.
 */
object DevModeService {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Toggling

    /**
     * Presents a password-protected prompt to enable or disable
     * developer mode.
     *
     * When developer mode is currently disabled, the user is asked to
     * enter the build's expiration override code. When it is already
     * enabled, a destructive confirmation alert is shown instead.
     *
     * This method has no effect on general-release builds.
     */
    fun promptToToggle() {
        if (Build.milestone == Milestone.GENERAL_RELEASE) return
        scope.launch {
            if (Build.isDeveloperModeEnabled) {
                val confirmed =
                    ConfirmationAlert(
                        title = "Disable Developer Mode",
                        message = "Are you sure you'd like to disable Developer Mode?",
                        confirmButtonTitle = "Disable",
                        confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                    ).present(translating = emptyList())

                if (!confirmed) return@launch
                return@launch toggleDeveloperMode(false)
            }

            val input =
                TextInputAlert(
                    title = "Enable Developer Mode",
                    message = "Enter the Developer Mode password to continue.",
                    placeholder = "••••••",
                    isSecure = true,
                    confirmButtonTitle = "Done",
                ).present(translating = emptyList())

            val enteredInput = input ?: return@launch
            if (enteredInput != Build.expirationOverrideCode) {
                Alert(
                    title = "Enable Developer Mode",
                    message = "The password entered was not correct. Please try again.",
                    actions =
                        listOf(
                            Action("Try Again", style = ActionStyle.PREFERRED) { promptToToggle() },
                            Action("Cancel", style = ActionStyle.CANCEL) {},
                        ),
                ).present(translating = emptyList())
                return@launch
            }

            toggleDeveloperMode(true)
        }
    }

    // MARK: - Auxiliary

    private fun toggleDeveloperMode(enabled: Boolean) {
        Build.setIsDeveloperModeEnabled(enabled)
        HUD.showSuccess()
    }
}
