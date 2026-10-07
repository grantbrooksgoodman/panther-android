//
//  DevModeActions+NetworkingOptions.kt
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
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.NetworkingStorageKey
import us.neotechnica.panther.networking.modules.common.extensions.networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/** The Developer Mode action that groups the networking diagnostic options. */
object NetworkingOptions {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Actions

    val networkingOptionsAction: DevModeAction =
        DevModeAction(title = "Networking Options") {
            scope.launch {
                ActionSheetAlert(
                    title = "Networking Options",
                    actions =
                        listOf(
                            Action("Inspect Network Health") { scope.launch { inspectNetworkHealth() } },
                            Action("Switch Environment") { scope.launch { switchEnvironment() } },
                            Action("Toggle Network Activity Indicator") { toggleNetworkActivityIndicator() },
                        ),
                ).present(translating = emptyList())
            }
        }

    // MARK: - Auxiliary

    private suspend fun inspectNetworkHealth() {
        Alert(
            title = "Network Health",
            message = Networking.health.debugSummary(),
            actions = listOf(Action("OK", style = ActionStyle.PREFERRED) {}),
        ).present(translating = emptyList())
    }

    private suspend fun switchEnvironment() {
        val current = Networking.config.environment
        val targets = NetworkEnvironment.entries.filter { it != current }
        val actions =
            targets.map { environment ->
                val style = if (environment == NetworkEnvironment.PRODUCTION) ActionStyle.DESTRUCTIVE else ActionStyle.DEFAULT
                Action("Switch to ${environment.displayName}", style = style) { scope.launch { switchTo(environment) } }
            }

        ActionSheetAlert(
            title = "Switch from ${current.displayName} Environment",
            actions = actions,
        ).present(translating = emptyList())
    }

    private suspend fun switchTo(environment: NetworkEnvironment) {
        Networking.config.setEnvironment(environment)
        CoreUtilities.clearCaches()
        CoreUtilities.eraseDocumentsDirectory()
        Persistent.reset()

        Alert(
            message = "Switched to ${environment.displayName} environment. You must now restart the app.",
            actions = listOf(Action("Exit", style = ActionStyle.DESTRUCTIVE_PREFERRED) { Application.beginGracefulExit() }),
        ).present(translating = emptyList())
    }

    private fun toggleNetworkActivityIndicator() {
        val key = PersistentStorageKey.networking(NetworkingStorageKey.IS_NETWORK_ACTIVITY_INDICATOR_ENABLED)
        val current = Persistent.booleanOrNull(key)
        if (current == null) {
            Persistent.setBoolean(key, true)
            HUD.showSuccess(text = "ON")
            return
        }

        Persistent.setBoolean(key, !current)
        HUD.showSuccess(text = if (!current) "ON" else "OFF")
    }

    private val NetworkEnvironment.displayName: String
        get() = rawValue.replaceFirstChar { it.uppercase() }
}
