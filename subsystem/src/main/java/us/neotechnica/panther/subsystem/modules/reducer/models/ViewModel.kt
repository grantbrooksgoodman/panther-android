//
//  ViewModel.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.reducer.models

import android.os.Looper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.Send
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer

/**
 * The runtime that drives a feature's state management cycle.
 *
 * [ViewModel] pairs a [Reducer] with an observable [state]
 * value. It forms the bridge between composables and the
 * unidirectional data flow defined by the reducer:
 *
 * 1. The view collects [state] and recomposes when it changes.
 * 2. The view calls [send] to dispatch an action.
 * 3. The reducer applies the action to the state and returns an
 *    [Effect].
 * 4. The runtime executes the effect, which may send additional
 *    actions back into step 3.
 *
 * ```kotlin
 * @Composable
 * fun CounterView(viewModel: ViewModel<CounterReducer.State, CounterReducer.Action>) {
 *     val state by viewModel.state.collectAsState()
 *     Text("${state.count}")
 *     Button(onClick = { viewModel.send(CounterReducer.Action.Increment) }) {
 *         Text("Increment")
 *     }
 * }
 * ```
 *
 * ## Long-Running Effects
 *
 * Use [sendWhile] to dispatch an action and suspend until the
 * state no longer satisfies a predicate. This is useful for
 * loading flows where the caller needs to wait for a result.
 *
 * ## Observing Shared Values
 *
 * Use [observing] to subscribe the view model to a flow, such as
 * a [StateStream.changes][us.neotechnica.panther.subsystem.modules.shared.models.StateStream.changes]
 * or [EventStream.events][us.neotechnica.panther.subsystem.modules.shared.models.EventStream.events]
 * stream, mapping each element to an action. Subscriptions are
 * cancelled when the view model is closed.
 *
 * **Important:** [ViewModel] is confined to the main thread.
 * Always call [send] from the main thread. Call [close] when the
 * feature leaves scope; closing cancels all in-flight effects
 * and subscriptions.
 *
 * **Warning:** The [Job] returned by [send] represents the
 * effect's lifetime, not the state mutation. The state is
 * updated synchronously *before* the job begins. Joining the job
 * waits for the effect to complete, which may never finish for
 * long-running effects such as observation streams. Use
 * [sendCancellableAction] or [sendWhile] when you need
 * structured cancellation.
 */
class ViewModel<State, Action>(
    initialState: State,
    private val reducer: Reducer<State, Action>,
    scope: CoroutineScope? = null,
) {
    // MARK: - Properties

    private val internalState = MutableStateFlow(initialState)
    private val scope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // MARK: - Computed Properties

    /**
     * The current state of the feature.
     *
     * This flow triggers recomposition whenever the state
     * changes. The value is read-only from outside the view
     * model; state mutations happen exclusively through the
     * reducer when an action is sent.
     */
    val state: StateFlow<State> = internalState.asStateFlow()

    /** The current state value. */
    val currentState: State
        get() = internalState.value

    // MARK: - Send

    /**
     * Sends an action to the reducer and returns the effect's
     * job.
     *
     * The reducer processes the action synchronously, updating
     * [state] before this method returns. The returned [Job]
     * represents the asynchronous effect, if any. You can
     * discard it when no follow-up is needed, or join it when
     * the effect's completion matters.
     *
     * @param action The action to send.
     *
     * @return A job that completes when the effect finishes.
     */
    fun send(action: Action): Job {
        checkThreadPreconditions()
        val effect = updateState(action)
        return scope.launch {
            effect.operation(
                Send { send(it) },
            )
        }
    }

    /**
     * Sends an action and awaits its effect with structured
     * cancellation.
     *
     * Unlike [send], the effect's job is automatically cancelled
     * when the calling coroutine is cancelled. Use this method
     * when the effect should not outlive the scope that
     * initiated it.
     *
     * @param action The action to send.
     */
    suspend fun sendCancellableAction(action: Action) {
        val job = send(action)
        try {
            job.join()
        } catch (exception: CancellationException) {
            job.cancel()
            throw exception
        }
    }

    /**
     * Sends an action and suspends until the state no longer
     * satisfies the given predicate.
     *
     * This method dispatches the action, then observes state
     * changes until `predicate` returns `false`. It is commonly
     * used to wait for a loading cycle to complete:
     *
     * ```kotlin
     * viewModel.sendWhile(Action.Refresh) { it.isLoading }
     * ```
     *
     * The effect's job is automatically cancelled if the calling
     * coroutine is cancelled, preventing orphaned work.
     *
     * @param action The action to send.
     * @param predicate A closure evaluated against each new
     *   state. The method returns when the predicate evaluates
     *   to `false`.
     */
    suspend fun sendWhile(
        action: Action,
        predicate: (State) -> Boolean,
    ) {
        val job = send(action)
        try {
            yieldWhile(predicate)
        } catch (exception: CancellationException) {
            job.cancel()
            throw exception
        }
    }

    /**
     * Suspends until the state no longer satisfies the given
     * predicate.
     *
     * This method observes the [state] flow – receiving the
     * current value immediately – and returns as soon as
     * `predicate` evaluates to `false`.
     *
     * @param predicate A closure evaluated against each new
     *   state. The method returns when the predicate evaluates
     *   to `false`.
     */
    suspend fun yieldWhile(predicate: (State) -> Boolean) {
        state.first { !predicate(it) }
    }

    // MARK: - Observing

    /**
     * Subscribes this view model to a flow, mapping each element
     * to an action.
     *
     * Each element the flow produces is passed to `toAction`,
     * and the resulting action is dispatched to the reducer on
     * the main thread. Return `null` from `toAction` to skip an
     * element.
     *
     * Because this method returns the view model, subscriptions
     * can be chained directly after construction:
     *
     * ```kotlin
     * ViewModel(
     *     initialState = CounterReducer.State(),
     *     reducer = CounterReducer(),
     * )
     *     .observing(isLoggedIn.changes) { Action.IsLoggedInChanged(it) }
     *     .observing(sessionDidExpire.events) { Action.SessionExpired }
     * ```
     *
     * The subscription lives exactly as long as the view model –
     * it is cancelled when [close] is called.
     *
     * **Note:** A [StateStream.changes][us.neotechnica.panther.subsystem.modules.shared.models.StateStream.changes]
     * flow yields the current value immediately upon
     * subscription, so its mapped action is dispatched once at
     * subscription time.
     *
     * @param source The flow to observe.
     * @param toAction A closure that converts each element into
     *   an action. Return `null` to skip the element.
     *
     * @return This view model, enabling chained `observing`
     *   calls.
     */
    fun <Element> observing(
        source: Flow<Element>,
        toAction: (Element) -> Action?,
    ): ViewModel<State, Action> {
        scope.launch {
            source.collect { element ->
                toAction(element)?.let { send(it) }
            }
        }

        return this
    }

    // MARK: - Close

    /**
     * Cancels all in-flight effects and observation
     * subscriptions.
     *
     * Call this method when the feature leaves scope – for
     * example, from a `DisposableEffect`'s `onDispose` block.
     * Sending actions after closing starts no new effects.
     */
    fun close() {
        scope.cancel()
    }

    // MARK: - Auxiliary

    private fun checkThreadPreconditions() {
        // Looper is unavailable in local unit tests; skip the
        // check when it cannot be evaluated.
        val isMainThread =
            runCatching {
                Looper.getMainLooper().isCurrentThread
            }.getOrDefault(true)

        check(isMainThread) { "Must be called on main thread only" }
    }

    private fun updateState(action: Action): Effect<Action> {
        val result =
            reducer.reduce(
                internalState.value,
                action,
            )

        internalState.value = result.state
        return result.effect
    }
}
