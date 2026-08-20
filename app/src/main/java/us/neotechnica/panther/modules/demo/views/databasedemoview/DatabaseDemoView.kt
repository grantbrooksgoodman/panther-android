//
//  DatabaseDemoView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.demo.views.databasedemoview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

/**
 * The Phase 2 backend-foundation debug page.
 *
 * Reads, writes, and observes arbitrary RTDB paths in the active
 * environment and establishes an anonymous session.
 */
@Composable
fun DatabaseDebugView(modifier: Modifier = Modifier) {
    // MARK: - Properties

    val viewModel =
        remember {
            ViewModel(
                initialState = DatabaseDemoReducer.State(),
                reducer = DatabaseDemoReducer(),
            )
        }

    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    val state by viewModel.state.collectAsState()

    // MARK: - Body

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Phase 2 · RTDB Debug",
            style = MaterialTheme.typography.titleLarge,
        )

        Text("Auth: ${state.authUserID ?: "signed out"}")

        OutlinedTextField(
            label = { Text("Path (dev environment)") },
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { viewModel.send(DatabaseDemoReducer.Action.PathChanged(it)) },
            singleLine = true,
            value = state.path,
        )

        OutlinedTextField(
            label = { Text("Value") },
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { viewModel.send(DatabaseDemoReducer.Action.ValueChanged(it)) },
            singleLine = true,
            value = state.value,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.send(DatabaseDemoReducer.Action.WriteButtonTapped) }) {
                Text("Write")
            }

            Button(onClick = { viewModel.send(DatabaseDemoReducer.Action.ReadButtonTapped) }) {
                Text("Read")
            }

            Button(onClick = { viewModel.send(DatabaseDemoReducer.Action.ObserveToggled) }) {
                Text(if (state.isObserving) "Stop Observing" else "Observe")
            }

            Button(onClick = { viewModel.send(DatabaseDemoReducer.Action.SignInButtonTapped) }) {
                Text("Sign In Anonymously")
            }
        }

        Text(
            "Log",
            style = MaterialTheme.typography.titleMedium,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            state.log.forEach {
                Text(
                    it,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
