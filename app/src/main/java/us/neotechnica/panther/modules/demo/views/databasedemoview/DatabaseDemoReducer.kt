//
//  DatabaseDemoReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.demo.views.databasedemoview

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.Send
import us.neotechnica.panther.subsystem.modules.effect.cancel
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * Drives the Phase 2 backend-foundation debug page.
 *
 * The page reads, writes, and observes arbitrary values at the
 * active (development) environment's RTDB paths, and establishes
 * an anonymous authentication session – the Phase 2 acceptance
 * surface.
 */
class DatabaseDemoReducer : Reducer<DatabaseDemoReducer.State, DatabaseDemoReducer.Action> {
    // MARK: - Actions

    sealed interface Action {
        data object ReadButtonTapped : Action

        data object SignInButtonTapped : Action

        data object ObserveToggled : Action

        data object WriteButtonTapped : Action

        data class PathChanged(
            val path: String,
        ) : Action

        data class ValueChanged(
            val value: String,
        ) : Action

        data class ObserveEmitted(
            val value: String,
        ) : Action

        data class OperationFailed(
            val label: String,
            val exception: Exception,
        ) : Action

        data class ReadReturned(
            val value: String,
        ) : Action

        data class SignInReturned(
            val userID: String,
        ) : Action

        data object WriteReturned : Action
    }

    // MARK: - State

    data class State(
        val authUserID: String? = null,
        val isObserving: Boolean = false,
        val log: List<String> = listOf(),
        val path: String = "debug/androidPhase2",
        val value: String = "hello from Android",
    )

    // MARK: - Types

    private object ObserveCancelID

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ReadButtonTapped ->
                ReduceResult(
                    state.logging("Reading ${state.path}…"),
                    Effect.run { send ->
                        runOperation("Read", send) {
                            val value: Any? =
                                Networking.config.databaseDelegate.getValues(
                                    path = state.path,
                                    cacheStrategy = CacheStrategy.DISREGARD_CACHE,
                                )
                            Action.ReadReturned(value.toString())
                        }
                    },
                )

            Action.SignInButtonTapped ->
                ReduceResult(
                    state.logging("Signing in anonymously…"),
                    Effect.run { send ->
                        runOperation("Sign-in", send) {
                            Action.SignInReturned(Networking.config.authDelegate.signInAnonymously())
                        }
                    },
                )

            Action.ObserveToggled ->
                if (state.isObserving) {
                    ReduceResult(
                        state.copy(isObserving = false).logging("Stopped observing."),
                        Effect.cancel(ObserveCancelID),
                    )
                } else {
                    ReduceResult(
                        state.copy(isObserving = true).logging("Observing ${state.path}…"),
                        Effect
                            .run<Action> { send ->
                                try {
                                    Networking.config.databaseDelegate
                                        .observe<Any?>(state.path)
                                        .collect { send(Action.ObserveEmitted(it.toString())) }
                                } catch (exception: Exception) {
                                    send(Action.OperationFailed("Observe", exception))
                                }
                            }.cancellable(ObserveCancelID),
                    )
                }

            Action.WriteButtonTapped ->
                ReduceResult(
                    state.logging("Writing \"${state.value}\" to ${state.path}…"),
                    Effect.run { send ->
                        runOperation("Write", send) {
                            Networking.config.databaseDelegate.setValue(
                                value = state.value,
                                key = state.path,
                            )
                            Action.WriteReturned
                        }
                    },
                )

            is Action.PathChanged -> ReduceResult(state.copy(path = action.path))
            is Action.ValueChanged -> ReduceResult(state.copy(value = action.value))

            is Action.ObserveEmitted -> ReduceResult(state.logging("Observed: ${action.value}"))
            is Action.OperationFailed ->
                ReduceResult(
                    state.logging("${action.label} failed: ${action.exception.descriptor}"),
                )

            is Action.ReadReturned -> ReduceResult(state.logging("Read: ${action.value}"))
            is Action.SignInReturned ->
                ReduceResult(
                    state.copy(authUserID = action.userID).logging("Signed in: ${action.userID}"),
                )

            Action.WriteReturned -> ReduceResult(state.logging("Write succeeded."))
        }

    // MARK: - Auxiliary

    private suspend fun runOperation(
        label: String,
        send: Send<Action>,
        operation: suspend () -> Action,
    ) {
        try {
            send(operation())
        } catch (exception: Exception) {
            send(Action.OperationFailed(label, exception))
        }
    }

    private fun State.logging(entry: String): State = copy(log = listOf(entry) + log.take(MAX_LOG - 1))

    private companion object {
        const val MAX_LOG = 20
    }
}
