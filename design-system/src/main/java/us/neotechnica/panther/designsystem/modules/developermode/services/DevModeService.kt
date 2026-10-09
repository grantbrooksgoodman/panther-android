//
//  DevModeService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.developermode.services

import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.designsystem.modules.developermode.interfaces.DevModeAppActionDelegate
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeSubsystemActions
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Build

/**
 * The interface for managing and presenting Developer Mode actions at
 * runtime.
 *
 * [DevModeService] maintains an ordered list of app-level [DevModeAction]
 * values alongside a set of built-in subsystem actions. When the
 * Developer Mode action sheet is presented, the service groups actions
 * into two domains:
 *
 * - **Application domain.** Actions registered by the host app through
 *   a [DevModeAppActionDelegate].
 * - **Subsystem domain.** Built-in diagnostic actions (cache clearing,
 *   language overrides, and others).
 *
 * Call [presentActionSheet] to show the menu. When app-domain actions
 * are available, the user first chooses between the app and subsystem
 * domains before seeing the individual actions.
 *
 * Call [promptToToggle] to present a password-protected prompt that
 * enables or disables Developer Mode. Developer Mode is not available on
 * general-release builds.
 */
object DevModeService {
    // MARK: - Types

    private enum class ActionDomain {
        APPLICATION,
        SUBSYSTEM,
    }

    // MARK: - Properties

    private val appActions = LockIsolated(listOf<DevModeAction>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Computed Properties

    private val subsystemActions: List<DevModeAction>
        get() = DevModeSubsystemActions.available

    // MARK: - Registration

    /**
     * Registers the delegate that supplies the app-domain actions.
     *
     * The delegate's actions replace any previously registered
     * app-domain actions.
     *
     * @param delegate The delegate supplying the app-domain actions.
     */
    fun registerAppActionDelegate(delegate: DevModeAppActionDelegate) {
        appActions.wrappedValue = delegate.appActions
    }

    // MARK: - Action Addition

    /**
     * Adds an action to the end of the app-domain list.
     *
     * If an action with matching metadata already exists, it is replaced.
     *
     * @param action The action to add.
     */
    fun addAction(action: DevModeAction) {
        appActions.withValue {
            it.value = it.value.filterNot { existing -> existing.metadata(isEqual = action) } + action
        }
    }

    /**
     * Adds multiple actions to the end of the app-domain list.
     *
     * @param actions The actions to add.
     */
    fun addActions(actions: List<DevModeAction>) {
        actions.forEach { addAction(it) }
    }

    // MARK: - Action Insertion

    /**
     * Inserts an action immediately after an existing action.
     *
     * The position is determined by matching the metadata of
     * [precedingAction]. If no match is found, the action is not
     * inserted.
     *
     * @param action The action to insert.
     * @param after The action after which to insert.
     */
    fun insertAction(
        action: DevModeAction,
        after: DevModeAction,
    ) {
        appActions.withValue {
            val index = it.value.indexOfFirst { existing -> existing.metadata(isEqual = after) }
            if (index < 0) return@withValue
            val filtered = it.value.filterNot { existing -> existing.metadata(isEqual = action) }.toMutableList()
            filtered.add(minOf(index + 1, filtered.size), action)
            it.value = filtered
        }
    }

    /**
     * Inserts an action at the given index in the app-domain list.
     *
     * If an action with matching metadata already exists, it is removed
     * before the insertion. Out-of-bounds indices are ignored.
     *
     * @param action The action to insert.
     * @param at The position at which to insert the action.
     */
    fun insertAction(
        action: DevModeAction,
        at: Int,
    ) {
        appActions.withValue {
            if (at < 0 || at > it.value.size) return@withValue
            val filtered = it.value.filterNot { existing -> existing.metadata(isEqual = action) }.toMutableList()
            filtered.add(minOf(at, filtered.size), action)
            it.value = filtered
        }
    }

    // MARK: - Action Removal

    /**
     * Removes the action at the given index from the app-domain list.
     *
     * Out-of-bounds indices are ignored.
     *
     * @param at The position of the action to remove.
     */
    fun removeAction(at: Int) {
        appActions.withValue {
            if (at < 0 || at >= it.value.size) return@withValue
            it.value = it.value.filterIndexed { index, _ -> index != at }
        }
    }

    /**
     * Removes all actions with the given title from the app-domain list.
     *
     * @param withTitle The title of the action to remove.
     */
    fun removeAction(withTitle: String) {
        appActions.withValue {
            it.value = it.value.filterNot { existing -> existing.title == withTitle }
        }
    }

    // MARK: - Menu Presentation

    /**
     * Presents the Developer Mode action sheet.
     *
     * When the app-domain list contains actions, the sheet first offers a
     * choice between the app and subsystem domains. When the list is
     * empty, the subsystem-domain actions are shown directly.
     */
    fun presentActionSheet() {
        scope.launch {
            if (appActions.wrappedValue.isEmpty()) return@launch presentActionSheet(ActionDomain.SUBSYSTEM)

            val actions =
                listOf(
                    Action("App Domain") { presentActionSheet(ActionDomain.APPLICATION) },
                    Action("Subsystem Domain") { presentActionSheet(ActionDomain.SUBSYSTEM) },
                    Action("Disable Developer Mode", style = ActionStyle.DESTRUCTIVE) { promptToToggle() },
                )

            ActionSheet(
                title = DEVELOPER_MODE_OPTIONS_TITLE,
                actions = actions,
            ).present(translating = emptyList())
        }
    }

    private fun presentActionSheet(domain: ActionDomain) {
        scope.launch {
            val domainActions = if (domain == ActionDomain.APPLICATION) appActions.wrappedValue else subsystemActions
            val actions =
                domainActions
                    .map { devModeAction ->
                        Action(
                            devModeAction.title,
                            style = if (devModeAction.isDestructive) ActionStyle.DESTRUCTIVE else ActionStyle.DEFAULT,
                        ) { devModeAction.perform() }
                    }.toMutableList()

            if (appActions.wrappedValue.isNotEmpty()) {
                actions.add(Action("Back", style = ActionStyle.CANCEL) { presentActionSheet() })
            }

            ActionSheet(
                title = DEVELOPER_MODE_OPTIONS_TITLE,
                actions = actions,
            ).present(translating = emptyList())
        }
    }

    // MARK: - Toggling

    /**
     * Presents a password-protected prompt to enable or disable
     * Developer Mode.
     *
     * When Developer Mode is currently disabled, the user is asked to
     * enter the build's expiration override code. When it is already
     * enabled, a destructive confirmation alert is shown instead.
     *
     * This method has no effect on general-release builds.
     */
    fun promptToToggle() {
        if (Build.milestone == Build.Milestone.GENERAL_RELEASE) return
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
                    attributes =
                        TextFieldAttributes(
                            isSecureTextEntry = true,
                            keyboardType = KeyboardType.NumberPassword,
                            placeholderText = "••••••",
                        ),
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
        HUD.showSuccess(text = "Developer Mode ${if (enabled) "Enabled" else "Disabled"}")
    }

    // MARK: - Companion

    private const val DEVELOPER_MODE_OPTIONS_TITLE = "Developer Mode Options"
}
