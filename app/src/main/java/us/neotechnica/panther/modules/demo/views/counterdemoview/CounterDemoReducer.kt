//
//  CounterDemoReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.demo.views.counterdemoview

import kotlinx.coroutines.delay
import us.neotechnica.panther.modules.demo.extensions.demoPingRequested
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancel
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.services.TimestampDateFormatter
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvent
import us.neotechnica.panther.subsystem.modules.shared.models.send
import java.util.Date
import kotlin.time.Duration.Companion.seconds

/**
 * Drives the Phase 1 kernel demonstration page.
 *
 * The page exercises every kernel primitive: synchronous
 * reduction (increment), delayed effects (reset), cancellable
 * long-running effects (polling), shared-event observation
 * (ping), and dependency resolution (the timestamp formatter).
 */
class CounterDemoReducer : Reducer<CounterDemoReducer.State, CounterDemoReducer.Action> {
    // MARK: - Dependencies

    private val demoPingRequested = SharedEvent { it.demoPingRequested }
    private val timestampDateFormatter: TimestampDateFormatter
        by Dependency { it.timestampDateFormatter }

    // MARK: - Actions

    sealed interface Action {
        data object DelayedResetButtonTapped : Action

        data object IncrementButtonTapped : Action

        data object PingButtonTapped : Action

        data object StartPollingButtonTapped : Action

        data object StopPollingButtonTapped : Action

        data object PingReturned : Action

        data object ResetReturned : Action

        data object Ticked : Action
    }

    // MARK: - State

    data class State(
        val count: Int = 0,
        val isPolling: Boolean = false,
        val lastPingTimestamp: String? = null,
        val pings: Int = 0,
        val ticks: Int = 0,
    )

    // MARK: - Types

    private object PollingCancelID

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.DelayedResetButtonTapped ->
                ReduceResult(
                    state,
                    Effect.task(delay = 1.seconds) { Action.ResetReturned },
                )

            Action.IncrementButtonTapped ->
                ReduceResult(
                    state.copy(count = state.count + 1),
                )

            Action.PingButtonTapped ->
                ReduceResult(
                    state,
                    Effect.fireAndForget { demoPingRequested.wrappedValue.send() },
                )

            Action.StartPollingButtonTapped ->
                ReduceResult(
                    state.copy(isPolling = true),
                    Effect
                        .run<Action> { send ->
                            while (true) {
                                delay(1.seconds)
                                send(Action.Ticked)
                            }
                        }.cancellable(PollingCancelID),
                )

            Action.StopPollingButtonTapped ->
                ReduceResult(
                    state.copy(isPolling = false),
                    Effect.cancel(PollingCancelID),
                )

            Action.PingReturned ->
                ReduceResult(
                    state.copy(
                        lastPingTimestamp = timestampDateFormatter.format(Date()),
                        pings = state.pings + 1,
                    ),
                )

            Action.ResetReturned -> ReduceResult(state.copy(count = 0))
            Action.Ticked -> ReduceResult(state.copy(ticks = state.ticks + 1))
        }
}
