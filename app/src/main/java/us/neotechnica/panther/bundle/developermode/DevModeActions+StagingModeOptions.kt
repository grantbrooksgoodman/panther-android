//
//  DevModeActions+StagingModeOptions.kt
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
import us.neotechnica.panther.bundle.application
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.modules.common.extensions.ApplicationStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/** The Developer Mode actions that control staging mode. */
object StagingModeOptions {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Staging Mode Options Action

    /**
     * The action that presents the Staging Mode Options sheet, ending
     * in a `Back` action that returns to the app-domain sheet. Its
     * actions reflect the app's current state.
     */
    val stagingModeOptionsAction: DevModeAction
        get() =
            DevModeAction(title = "Staging Mode Options") {
                scope.launch {
                    val actions =
                        listOf(toggleStagingModeAction)
                            .map { devModeAction ->
                                Action(
                                    devModeAction.title,
                                    style = if (devModeAction.isDestructive) ActionStyle.DESTRUCTIVE else ActionStyle.DEFAULT,
                                ) { devModeAction.perform() }
                            }.toMutableList()

                    actions.add(Action("Back", style = ActionStyle.CANCEL) { AppActions.presentActionSheet() })

                    ActionSheet(
                        title = "Staging Mode Options",
                        actions = actions,
                    ).present(translating = emptyList())
                }
            }

    // MARK: - Auxiliary

    private val toggleStagingModeAction: DevModeAction
        get() =
            DevModeAction(title = "${if (Application.isInStagingMode) "Disable" else "Enable"} Staging Mode") {
                toggleStagingMode()
            }

    private fun toggleStagingMode() {
        val isInStagingMode = !Application.isInStagingMode
        Persistent.setBoolean(
            PersistentStorageKey.application(ApplicationStorageKey.IS_IN_STAGING_MODE),
            isInStagingMode,
        )

        HUD.flash("Staging Mode ${if (isInStagingMode) "Enabled" else "Disabled"}", HUD.HUDImage.SUCCESS)
    }
}
