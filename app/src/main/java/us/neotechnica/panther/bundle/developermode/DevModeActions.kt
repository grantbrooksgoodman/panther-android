//
//  DevModeActions.kt
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
import us.neotechnica.panther.designsystem.modules.developermode.interfaces.DevModeAppActionDelegate
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.designsystem.modules.developermode.services.DevModeService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService

/**
 * The delegate that supplies app-specific actions to the Developer Mode
 * menu.
 *
 * Define [DevModeAction] instances and include them in [appActions] to
 * make them available in the Developer Mode action sheet. Developer
 * Mode actions are available only in prerelease builds; the menu hides
 * them entirely in general-release builds.
 */
object AppActions : DevModeAppActionDelegate {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Computed Properties

    /** The actions to display in the Developer Mode action sheet. */
    override val appActions: List<DevModeAction>
        get() =
            listOf(
                UIOptions.uiOptionsAction,
                UserOptions.userOptionsAction,
                dangerZoneAction,
            )

    // MARK: - Methods

    /**
     * Presents the app-domain Developer Mode action sheet.
     *
     * The sheet is the service's app-domain list – [appActions] plus the
     * actions other modules inserted – followed by a `Back` action that
     * returns to the Developer Mode menu.
     */
    fun presentActionSheet() {
        DevModeService.presentAppDomainActionSheet()
    }

    // MARK: - Top-level Actions

    private val dangerZoneAction: DevModeAction
        get() =
            DevModeAction(title = "Danger Zone", isDestructive = true) {
                scope.launch {
                    val currentUser = UserSessionService.currentUser
                    val actions = mutableListOf(DangerZone.resetPushTokensAction)

                    if (currentUser?.previousLanguageCodes?.isNotEmpty() == true) {
                        actions.add(0, DangerZone.clearPreviousLanguageCodesAction)
                    }

                    if (currentUser?.conversations != null) {
                        actions.add(minOf(1, actions.size), DangerZone.deleteConversationsAction)
                    }

                    ActionSheet(
                        title = "Danger Zone",
                        message = "Exercise caution when using these options.",
                        actions =
                            actions.map { devModeAction ->
                                Action(
                                    devModeAction.title,
                                    style = if (devModeAction.isDestructive) ActionStyle.DESTRUCTIVE else ActionStyle.DEFAULT,
                                ) { devModeAction.perform() }
                            },
                    ).present(translating = emptyList())
                }
            }
}
