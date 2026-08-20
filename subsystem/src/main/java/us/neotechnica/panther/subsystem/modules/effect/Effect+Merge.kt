//
//  Effect+Merge.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.effect

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Combines the given effects into a single effect that runs them
 * all in parallel.
 *
 * Use `merge` when a single action should trigger multiple
 * independent pieces of work. All merged effects share a single
 * [Send] callback. Actions sent by any of the effects are
 * delivered in the order they arrive.
 *
 * @param effects The effects to run in parallel.
 *
 * @return A single effect that runs all provided effects
 *   concurrently.
 */
fun <Action> Effect.Companion.merge(vararg effects: Effect<Action>): Effect<Action> = merge(effects.toList())

/**
 * Combines a list of effects into a single effect that runs them
 * all in parallel.
 *
 * @param effects A list of effects to run in parallel.
 *
 * @return A single effect that runs all provided effects
 *   concurrently.
 */
fun <Action> Effect.Companion.merge(effects: List<Effect<Action>>): Effect<Action> =
    effects.fold(none()) { merged, next ->
        merged.merge(next)
    }

/**
 * Combines this effect with another, running both in parallel.
 *
 * @param other The effect to run alongside this one.
 *
 * @return A single effect that runs both effects concurrently.
 */
fun <Action> Effect<Action>.merge(other: Effect<Action>): Effect<Action> =
    Effect.run { send ->
        coroutineScope {
            launch { operation(send) }
            launch { other.operation(send) }
        }
    }
