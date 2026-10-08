//
//  DevModeAction+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.AttributedStringConfig
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthTier
import us.neotechnica.panther.networking.modules.health.services.NetworkHealthService
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import kotlin.system.exitProcess

/** The Developer Mode action that inspects the current network health. */
val DevModeAction.Companion.inspectNetworkHealthAction: DevModeAction
    get() =
        DevModeAction(title = "Inspect Network Health") {
            scope.launch { inspectNetworkHealth() }
        }

/** The Developer Mode action that groups the networking diagnostic options. */
val DevModeAction.Companion.networkingOptionsAction: DevModeAction
    get() =
        DevModeAction(title = "Networking Options") {
            scope.launch {
                ActionSheet(
                    title = "Networking Options",
                    actions =
                        listOf(
                            DevModeAction.inspectNetworkHealthAction,
                            switchEnvironmentAction,
                            toggleNetworkActivityIndicatorAction,
                        ).map { devModeAction ->
                            Action(devModeAction.title) { devModeAction.perform() }
                        },
                ).present(translating = emptyList())
            }
        }

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

private val switchEnvironmentAction: DevModeAction
    get() =
        DevModeAction(title = "Switch Environment") {
            scope.launch { switchEnvironment() }
        }

private val toggleNetworkActivityIndicatorAction: DevModeAction
    get() =
        DevModeAction(title = "Toggle Network Activity Indicator") {
            toggleNetworkActivityIndicator()
        }

private suspend fun inspectNetworkHealth() {
    val networkHealthService = Networking.config.healthDelegate
    if (networkHealthService !is NetworkHealthService) {
        Logger.log(
            Exception(
                "The registered NetworkHealthDelegate is incompatible.",
                isReportable = false,
                metadata = ExceptionMetadata(NetworkHealthService),
            ),
            domain = LoggerDomain.Networking.health,
            with = AlertType.toast,
        )

        return
    }

    val summary = networkHealthService.debugSummary()
    val inspectionAlert =
        Alert(
            title = "Network Health",
            message = summary,
            actions = listOf(Action("OK", style = ActionStyle.PREFERRED) {}),
        )

    val labelStyle =
        SpanStyle(
            fontSize = INSPECTION_FONT_SIZE.sp,
            fontWeight = FontWeight.SemiBold,
        )

    val secondaryAttributes =
        mutableListOf(
            AttributedStringConfig.StringAttributes(
                labelStyle,
                stringRanges =
                    listOf(
                        "Confidence:",
                        "Failures:",
                        "Flaps:",
                        "Latency:",
                        "Path:",
                        "Probing:",
                        "Score:",
                        "Socket:",
                        "Stalls:",
                        "Throughput:",
                        "Transfer:",
                    ),
            ),
        )

    networkHealthService.health.tier?.let { tier ->
        val tierColor =
            when (tier) {
                NetworkHealthTier.FAIR -> FAIR_TIER_COLOR
                NetworkHealthTier.GOOD -> GOOD_TIER_COLOR
                NetworkHealthTier.POOR -> POOR_TIER_COLOR
            }

        secondaryAttributes.add(
            AttributedStringConfig.StringAttributes(
                labelStyle.copy(color = tierColor),
                stringRanges = listOf("(${tier.rawValue.replaceFirstChar { it.uppercase() }})"),
            ),
        )
    }

    inspectionAlert.setMessageAttributes(
        AttributedStringConfig(
            SpanStyle(fontSize = INSPECTION_FONT_SIZE.sp),
            secondaryAttributes = secondaryAttributes,
        ),
    )

    inspectionAlert.present(translating = emptyList())
}

private suspend fun switchEnvironment() {
    val switchToDevelopmentAction = Action("Switch to Development") { scope.launch { switchTo(NetworkEnvironment.DEVELOPMENT) } }
    val switchToProductionAction =
        Action(
            "Switch to Production",
            style = ActionStyle.DESTRUCTIVE,
        ) { scope.launch { switchTo(NetworkEnvironment.PRODUCTION) } }
    val switchToStagingAction = Action("Switch to Staging") { scope.launch { switchTo(NetworkEnvironment.STAGING) } }

    val actions =
        when (Networking.config.environment) {
            NetworkEnvironment.DEVELOPMENT ->
                listOf(
                    switchToProductionAction,
                    switchToStagingAction,
                )

            NetworkEnvironment.PRODUCTION ->
                listOf(
                    switchToDevelopmentAction,
                    switchToStagingAction,
                )

            NetworkEnvironment.STAGING ->
                listOf(
                    switchToDevelopmentAction,
                    switchToProductionAction,
                )
        }

    ActionSheet(
        title = "Switch from ${Networking.config.environment.description} Environment",
        actions = actions,
    ).present(translating = emptyList())
}

private suspend fun switchTo(environment: NetworkEnvironment) {
    Networking.config.setEnvironment(environment)

    CoreUtilities.clearCaches()
    runCatching { CoreUtilities.eraseDocumentsDirectory() }
    CoreUtilities.eraseTemporaryDirectory()
    Persistent.reset()

    Alert(
        message = "Switched to ${environment.description} environment. You must now restart the app.",
        actions = listOf(Action("Exit", style = ActionStyle.DESTRUCTIVE_PREFERRED) { exitProcess(0) }),
    ).present(translating = emptyList())
}

private fun toggleNetworkActivityIndicator() {
    val key = PersistentStorageKey.networking(NetworkingStorageKey.IS_NETWORK_ACTIVITY_INDICATOR_ENABLED)
    val persistedValue = Persistent.booleanOrNull(key)
    if (persistedValue == null) {
        Persistent.setBoolean(key, true)
        HUD.showSuccess(text = "ON")
        return
    }

    Persistent.setBoolean(key, !persistedValue)
    HUD.showSuccess(text = if (!persistedValue) "ON" else "OFF")
}

private const val INSPECTION_FONT_SIZE = 13

private val FAIR_TIER_COLOR = Color(0xFFFF9500)
private val GOOD_TIER_COLOR = Color(0xFF34C759)
private val POOR_TIER_COLOR = Color(0xFFFF3B30)
