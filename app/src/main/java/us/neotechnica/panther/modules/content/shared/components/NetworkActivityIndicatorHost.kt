//
//  NetworkActivityIndicatorHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.foundation.overlay.NetworkActivityIndicator
import us.neotechnica.panther.modules.common.services.NetworkActivityIndicatorService
import us.neotechnica.panther.networking.modules.common.extensions.NetworkingStorageKey
import us.neotechnica.panther.networking.modules.common.extensions.networking
import us.neotechnica.panther.networking.modules.health.extensions.networkHealth
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState

/**
 * Hosts the [NetworkActivityIndicator] at the app root, gated on the
 * persisted enablement flag. Tapping the
 * indicator runs the delegate's tap action, or presents a summary of
 * the current network health.
 *
 * @param modifier The modifier for the indicator.
 */
@Composable
fun NetworkActivityIndicatorHost(modifier: Modifier = Modifier) {
    val isEnabled =
        Persistent.booleanOrNull(
            PersistentStorageKey.networking(NetworkingStorageKey.IS_NETWORK_ACTIVITY_INDICATOR_ENABLED),
        ) ?: false
    if (!isEnabled) return

    val isActive by NetworkActivityIndicatorService.isActive.collectAsState()
    val scope = rememberCoroutineScope()

    NetworkActivityIndicator(
        isActive = isActive,
        backgroundColor = NetworkActivityIndicatorService.backgroundColor?.let { Color(it) },
        progressViewTintColor = NetworkActivityIndicatorService.progressViewTintColor?.let { Color(it) } ?: Color.White,
        onTap = {
            val tapAction = NetworkActivityIndicatorService.tapAction
            if (tapAction != null) tapAction() else scope.launch { presentHealthSummaryAlert() }
        },
        modifier = modifier,
    )
}

private suspend fun presentHealthSummaryAlert() {
    val tier = SharedState { it.networkHealth }.wrappedValue.tier
    val tierDescription = tier?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Unknown"
    Alert(title = "Network Health", message = "Current tier: $tierDescription").present()
}
