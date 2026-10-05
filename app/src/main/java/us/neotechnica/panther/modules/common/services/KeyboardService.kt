//
//  KeyboardService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.resignFirstResponders
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Dismisses the keyboard from anywhere in the app.
 *
 * [KeyboardService] requests resignation of the active first responder,
 * which the root view fulfills by clearing focus and hiding the soft
 * keyboard. Call it from reducers and services wherever the keyboard
 * should give way – when an overlay appears, a page dismisses, a list
 * scrolls, or a selection is made.
 */
object KeyboardService {
    // MARK: - Properties

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Methods

    /**
     * Requests dismissal of the keyboard.
     *
     * When [repeatingFor] is given, the request repeats every 100
     * milliseconds for that duration, in case the keyboard is still
     * being presented when the first request arrives.
     *
     * @param repeatingFor The duration over which to repeat the
     *   request, or `null` to request it once.
     */
    fun resignFirstResponders(repeatingFor: Duration? = null) {
        DependencyValues.current.sharedEvents.resignFirstResponders.send(Unit)

        val duration = repeatingFor ?: return
        serviceScope.launch {
            var elapsed = Duration.ZERO
            while (elapsed < duration) {
                delay(REPEAT_INTERVAL)
                elapsed += REPEAT_INTERVAL
                DependencyValues.current.sharedEvents.resignFirstResponders.send(Unit)
            }
        }
    }

    // MARK: - Companion

    private val REPEAT_INTERVAL = 100.milliseconds
}
