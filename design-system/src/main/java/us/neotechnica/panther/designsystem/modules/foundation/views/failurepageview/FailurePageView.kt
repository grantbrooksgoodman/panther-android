//
//  FailurePageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.failurepageview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Type Aliases

private typealias FailurePageViewModel = ViewModel<FailurePageReducer.State, FailurePageReducer.Action>

// MARK: - Body

@Composable
internal fun FailurePageView(viewModel: FailurePageViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Components.Symbol(
            systemName = "exclamationmark.octagon.fill",
            foregroundColor = FAILURE_GLYPH_COLOR,
            modifier = Modifier.size(FAILURE_GLYPH_SIZE.dp),
        )

        Text(
            state.exception.userFacingDescriptor,
            color = colors.titleText,
            fontSize = FAILURE_DESCRIPTOR_SIZE.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        if (state.retryHandler != null) {
            TextButton(onClick = { viewModel.send(FailurePageReducer.Action.ExecuteRetryHandler) }) {
                Text(state.retryButtonText, color = colors.accent)
            }
        }

        if (!Logger.reportsErrorsAutomatically &&
            state.exception.isReportable
        ) {
            TextButton(
                enabled = !state.didReportBug,
                onClick = { viewModel.send(FailurePageReducer.Action.ReportBugButtonTapped) },
            ) {
                Text(
                    state.reportBugButtonText,
                    color = if (state.didReportBug) colors.disabled else colors.accent,
                )
            }
        }
    }
}

private const val FAILURE_GLYPH_SIZE = 120
private const val FAILURE_DESCRIPTOR_SIZE = 22.5f
private val FAILURE_GLYPH_COLOR = Color(0xFFFF3B30)
