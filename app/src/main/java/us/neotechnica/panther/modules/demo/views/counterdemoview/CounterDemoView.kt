//
//  CounterDemoView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.demo.views.counterdemoview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.modules.demo.extensions.demoPingRequested
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvent

/**
 * The Phase 1 kernel demonstration page.
 *
 * Renders a counter driven by [CounterDemoReducer], exercising
 * effects, cancellation, dependency resolution, and shared-event
 * observation end to end.
 */
@Composable
fun CounterDemoView(modifier: Modifier = Modifier) {
    // MARK: - Properties

    val viewModel =
        remember {
            val demoPingRequested = SharedEvent { it.demoPingRequested }
            ViewModel(
                initialState = CounterDemoReducer.State(),
                reducer = CounterDemoReducer(),
            ).observing(demoPingRequested.wrappedValue.events) {
                CounterDemoReducer.Action.PingReturned
            }
        }

    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    val state by viewModel.state.collectAsState()

    // MARK: - Body

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement =
            Arrangement.spacedBy(
                12.dp,
                Alignment.CenterVertically,
            ),
    ) {
        Text(
            "Count: ${state.count}",
            style = MaterialTheme.typography.headlineMedium,
        )

        Text("Ticks: ${state.ticks}")
        Text("Pings: ${state.pings}")
        state.lastPingTimestamp?.let { Text("Last ping: $it") }

        Button(onClick = {
            viewModel.send(CounterDemoReducer.Action.IncrementButtonTapped)
        }) {
            Text("Increment")
        }

        Button(onClick = {
            viewModel.send(CounterDemoReducer.Action.DelayedResetButtonTapped)
        }) {
            Text("Reset After 1s")
        }

        Button(onClick = {
            viewModel.send(
                if (state.isPolling) {
                    CounterDemoReducer.Action.StopPollingButtonTapped
                } else {
                    CounterDemoReducer.Action.StartPollingButtonTapped
                },
            )
        }) {
            Text(if (state.isPolling) "Stop Polling" else "Start Polling")
        }

        Button(onClick = {
            viewModel.send(CounterDemoReducer.Action.PingButtonTapped)
        }) {
            Text("Ping via Shared Event")
        }
    }
}
