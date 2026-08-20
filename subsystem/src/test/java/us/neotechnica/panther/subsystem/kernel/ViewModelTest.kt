//
//  ViewModelTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.Send
import us.neotechnica.panther.subsystem.modules.effect.cancel
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.models.EventStream
import kotlin.time.Duration.Companion.seconds

private class CounterReducer : Reducer<CounterReducer.State, CounterReducer.Action> {
    // MARK: - Actions

    sealed interface Action {
        data object DelayedReset : Action

        data object Increment : Action

        data object StartPolling : Action

        data object StopPolling : Action

        data class EchoReturned(
            val value: Int,
        ) : Action

        data object ResetReturned : Action

        data object Ticked : Action
    }

    // MARK: - State

    data class State(
        val count: Int = 0,
        val echoes: List<Int> = listOf(),
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
            Action.DelayedReset ->
                ReduceResult(
                    state,
                    Effect.task(delay = 1.seconds) { Action.ResetReturned },
                )

            Action.Increment -> ReduceResult(state.copy(count = state.count + 1))

            Action.StartPolling ->
                ReduceResult(
                    state,
                    Effect
                        .run<Action> { send ->
                            while (true) {
                                delay(1.seconds)
                                send(Action.Ticked)
                            }
                        }.cancellable(PollingCancelID),
                )

            Action.StopPolling ->
                ReduceResult(
                    state,
                    Effect.cancel(PollingCancelID),
                )

            is Action.EchoReturned ->
                ReduceResult(
                    state.copy(echoes = state.echoes + action.value),
                )

            Action.ResetReturned -> ReduceResult(state.copy(count = 0))
            Action.Ticked -> ReduceResult(state.copy(ticks = state.ticks + 1))
        }
}

class ViewModelTest {
    // MARK: - Properties

    private val dispatcher = UnconfinedTestDispatcher()

    // MARK: - Setup & Teardown

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `send updates state synchronously`() =
        runTest(dispatcher.scheduler) {
            val viewModel = makeViewModel()

            viewModel.send(CounterReducer.Action.Increment)
            assertEquals(1, viewModel.currentState.count)

            viewModel.send(CounterReducer.Action.Increment)
            assertEquals(2, viewModel.currentState.count)

            viewModel.close()
        }

    @Test
    fun `effects send actions back into the reducer`() =
        runTest(dispatcher.scheduler) {
            val viewModel = makeViewModel()

            viewModel.send(CounterReducer.Action.Increment)
            viewModel.send(CounterReducer.Action.DelayedReset)
            assertEquals(1, viewModel.currentState.count)

            advanceUntilIdle()
            assertEquals(0, viewModel.currentState.count)

            viewModel.close()
        }

    @Test
    fun `cancellable effects stop when cancelled`() =
        runTest(dispatcher.scheduler) {
            val viewModel = makeViewModel()

            viewModel.send(CounterReducer.Action.StartPolling)
            advanceTimeBy(3.5.seconds)
            assertEquals(3, viewModel.currentState.ticks)

            viewModel.send(CounterReducer.Action.StopPolling)
            advanceTimeBy(3.seconds)
            assertEquals(3, viewModel.currentState.ticks)

            viewModel.close()
        }

    @Test
    fun `merged effects run in parallel`() =
        runTest(dispatcher.scheduler) {
            val viewModel = makeViewModel()
            val effect =
                Effect.merge(
                    Effect.task<CounterReducer.Action> { CounterReducer.Action.Increment },
                    Effect.task<CounterReducer.Action> { CounterReducer.Action.Increment },
                )

            viewModel.send(CounterReducer.Action.Increment)
            val job =
                launch {
                    effect.operation(
                        Send { viewModel.send(it) },
                    )
                }

            advanceUntilIdle()
            job.join()
            assertEquals(3, viewModel.currentState.count)

            viewModel.close()
        }

    @Test
    fun `observing maps stream elements to actions`() =
        runTest(dispatcher.scheduler) {
            val stream = EventStream<Int>()
            val viewModel =
                makeViewModel().observing(stream.events) {
                    CounterReducer.Action.EchoReturned(it)
                }

            advanceUntilIdle()
            stream.send(1)
            stream.send(2)
            advanceUntilIdle()

            assertEquals(listOf(1, 2), viewModel.currentState.echoes)
            viewModel.close()
        }

    @Test
    fun `sendWhile suspends until the predicate clears`() =
        runTest(dispatcher.scheduler) {
            val viewModel = makeViewModel()
            viewModel.send(CounterReducer.Action.Increment)

            viewModel.sendWhile(CounterReducer.Action.DelayedReset) { it.count > 0 }
            assertEquals(0, viewModel.currentState.count)

            viewModel.close()
        }

    // MARK: - Auxiliary

    private fun makeViewModel() =
        ViewModel(
            initialState = CounterReducer.State(),
            reducer = CounterReducer(),
        )
}
