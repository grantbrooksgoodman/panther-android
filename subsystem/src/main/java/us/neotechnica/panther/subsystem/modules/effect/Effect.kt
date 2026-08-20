//
//  Effect.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.effect

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyScopes
import kotlin.time.Duration

/**
 * A description of work to perform in response to a reducer
 * action.
 *
 * When a [Reducer][us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer]
 * receives an action, it returns an [Effect] that describes any
 * asynchronous work that should follow the state change. The
 * runtime executes the effect and dispatches any resulting
 * actions back into the reducer.
 *
 * ## Creating Effects
 *
 * Use one of the provided factory methods to create an effect:
 *
 * - [none] when no work is needed.
 * - [run] to perform async work that may send one or more
 *   actions back through a [Send] callback.
 * - [fireAndForget] to perform async work that does not produce
 *   actions.
 * - [task] to perform async work that returns a single optional
 *   action.
 *
 * ## Combining Effects
 *
 * Use [merge] to run multiple effects in parallel.
 *
 * ## Cancellation
 *
 * Mark long-running effects with [cancellable] and cancel them
 * later with [cancel].
 *
 * ## Dependency Scope
 *
 * Effects created with [run] and its variants automatically
 * capture the current dependency scope. Dependencies resolved
 * inside the effect's closure see the same values that were
 * active when the effect was created.
 */
class Effect<Action> internal constructor(
    /** The suspending closure that performs this effect's work. */
    val operation: suspend (Send<Action>) -> Unit,
) {
    // MARK: - Companion

    companion object {
        /**
         * A no-op effect.
         *
         * Return `none()` from a reducer when a state change does
         * not require any follow-up work.
         */
        fun <Action> none(): Effect<Action> = Effect { }

        /**
         * Creates an effect that performs asynchronous work
         * without producing actions.
         *
         * Use `fireAndForget` for side effects that do not feed
         * information back into the reducer, such as analytics
         * or logging.
         *
         * @param operation The asynchronous work to perform.
         */
        fun <Action> fireAndForget(operation: suspend () -> Unit): Effect<Action> = run { operation() }

        /**
         * Creates an effect that performs asynchronous work and
         * sends actions back to the reducer.
         *
         * This is the most general-purpose factory method. The
         * provided closure receives a [Send] value that can be
         * called zero or more times to feed actions back into
         * the system.
         *
         * @param operation The asynchronous work to perform.
         *   Call `send` to dispatch actions back to the reducer.
         */
        fun <Action> run(operation: suspend (Send<Action>) -> Unit): Effect<Action> =
            DependencyScopes.withEscapedDependencies { dependencies ->
                Effect { send ->
                    dependencies.withValue {
                        operation(send)
                    }
                }
            }

        /**
         * Creates an effect that sends each element of a flow as
         * an action.
         *
         * The effect completes when the flow terminates.
         *
         * @param flow The flow whose elements are sent as
         *   actions.
         */
        fun <Action> run(flow: Flow<Action>): Effect<Action> =
            run { send ->
                flow.collect { send(it) }
            }

        /**
         * Creates an effect that performs asynchronous work and
         * sends at most one action.
         *
         * Return `null` from the closure to complete without
         * sending an action. Pass a `delay` to defer the work.
         *
         * @param delay An optional duration to wait before
         *   performing the operation. Defaults to `null`.
         * @param operation The asynchronous work to perform.
         *   Return an action to send to the reducer, or `null`
         *   to complete silently.
         */
        fun <Action> task(
            delay: Duration? = null,
            operation: suspend () -> Action?,
        ): Effect<Action> =
            run { send ->
                delay?.let { delay(it) }
                operation()?.let { send(it) }
            }
    }

    // MARK: - Methods

    /**
     * Transforms the actions produced by this effect.
     *
     * Use `map` to convert an effect's action type when composing
     * child reducers into a parent.
     *
     * @param toMapAction A closure that converts the original
     *   action into the target action type.
     *
     * @return An effect that produces the mapped actions.
     */
    fun <MappedAction> map(toMapAction: (Action) -> MappedAction): Effect<MappedAction> =
        Effect { send ->
            // The inner Send performs the cancellation check and
            // main-thread hop; forwarding through the outer Send's
            // raw callback stays synchronous on the main thread.
            operation(
                Send { action ->
                    send.send(toMapAction(action))
                },
            )
        }
}
