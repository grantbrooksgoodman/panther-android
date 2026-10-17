//
//  FailurePageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.failurepageview

import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.foundation.models.ConnectionAlert
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

internal class FailurePageReducer(
    private val dependencies: Dependencies = Dependencies(),
) : Reducer<FailurePageReducer.State, FailurePageReducer.Action> {
    // MARK: - Dependencies

    data class Dependencies(
        val fileReport: (Exception) -> Unit = { AlertKitConfig.reportDelegate?.fileReport(it) },
        val isOnline: () -> Boolean = { Build.isOnline },
        val presentConnectionAlert: suspend () -> Unit = { ConnectionAlert.present() },
    )

    // MARK: - Actions

    sealed interface Action {
        data object ExecuteRetryHandler : Action

        data object ReportBugButtonTapped : Action
    }

    // MARK: - State

    data class State(
        val exception: Exception,
        val retryHandler: (() -> Unit)? = null,
        val didReportBug: Boolean = false,
        val reportBugButtonText: String = Localized(SubsystemStringKey.REPORT_BUG).wrappedValue,
        val retryButtonText: String = Localized(SubsystemStringKey.TRY_AGAIN).wrappedValue,
    )

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ExecuteRetryHandler -> {
                state.retryHandler?.invoke()
                ReduceResult(state)
            }

            Action.ReportBugButtonTapped -> {
                if (!dependencies.isOnline()) {
                    ReduceResult(state, Effect.fireAndForget { dependencies.presentConnectionAlert() })
                } else {
                    val exception = state.exception
                    ReduceResult(
                        state.copy(didReportBug = true),
                        Effect.fireAndForget { dependencies.fileReport(exception) },
                    )
                }
            }
        }
}
