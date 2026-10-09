//
//  NetworkActivityViewModifier.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.modifiers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.drop
import us.neotechnica.panther.networking.modules.common.components.networkactivityindicator.NetworkActivityIndicator
import us.neotechnica.panther.networking.modules.common.components.networkactivityindicator.NetworkActivityIndicatorReducer
import us.neotechnica.panther.networking.modules.common.extensions.isNetworkActivityOccurring
import us.neotechnica.panther.networking.modules.health.extensions.networkHealth
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState

// MARK: - Type Aliases

private typealias NetworkActivityIndicatorViewModel =
    ViewModel<NetworkActivityIndicatorReducer.State, NetworkActivityIndicatorReducer.Action>

/**
 * Overlays a network activity indicator on the given content.
 *
 * Apply this wrapper to your root view to display an activity
 * indicator whenever a networking operation is in progress:
 *
 * ```kotlin
 * IndicatesNetworkActivity {
 *     ContentView()
 * }
 * ```
 *
 * @param content The content to overlay.
 */
@Composable
fun IndicatesNetworkActivity(content: @Composable () -> Unit) {
    Box {
        content()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .zIndex(1f),
        ) {
            NetworkActivityIndicator(
                remember { makeNetworkActivityIndicatorViewModel() },
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// Dropping the first element skips the replayed current value;
// activity effects should only run for changes occurring after
// subscription.
private fun makeNetworkActivityIndicatorViewModel(): NetworkActivityIndicatorViewModel =
    ViewModel(
        NetworkActivityIndicatorReducer.State(),
        NetworkActivityIndicatorReducer(),
    ).observing(
        SharedState { it.isNetworkActivityOccurring }.projectedValue.changes.drop(1),
    ) { NetworkActivityIndicatorReducer.Action.IsVisibleChanged(it) }
        .observing(
            SharedState { it.networkHealth }.projectedValue.changes,
        ) { NetworkActivityIndicatorReducer.Action.HealthChanged }
