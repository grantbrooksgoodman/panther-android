//
//  DevModeActions+UIOptions.kt
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
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import us.neotechnica.panther.subsystem.modules.shared.models.isForcedUpdateRequired

/** The Developer Mode actions that exercise the app's interface. */
object UIOptions {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - UI Options Action

    /**
     * The action that presents the UI Options sheet, ending in a `Back`
     * action that returns to the app-domain sheet.
     */
    val uiOptionsAction: DevModeAction
        get() =
            DevModeAction(title = "UI Options") {
                scope.launch {
                    val actions =
                        listOf(triggerForcedUpdateModalAction)
                            .map { devModeAction ->
                                Action(
                                    devModeAction.title,
                                    style = if (devModeAction.isDestructive) ActionStyle.DESTRUCTIVE else ActionStyle.DEFAULT,
                                ) { devModeAction.perform() }
                            }.toMutableList()

                    actions.add(Action("Back", style = ActionStyle.CANCEL) { AppActions.presentActionSheet() })

                    ActionSheet(
                        title = "UI Options",
                        actions = actions,
                    ).present(translating = emptyList())
                }
            }

    // MARK: - Auxiliary

    private val triggerForcedUpdateModalAction: DevModeAction
        get() =
            DevModeAction(title = "Trigger Forced Update Modal") {
                SharedState { it.isForcedUpdateRequired }.wrappedValue = true
            }
}
