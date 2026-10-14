//
//  Timeout.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * A deferred action that runs after a duration unless canceled
 * first.
 *
 * Use [Timeout] to react to work that takes longer than expected –
 * showing a progress indicator, say – and call [cancel] once the work
 * finishes:
 *
 * ```kotlin
 * val timeout = Timeout(after = 1.seconds) { HUD.showProgress() }
 * loadData()
 * timeout.cancel()
 * ```
 *
 * The action runs on the main dispatcher. Canceling after the action
 * has run has no effect.
 *
 * @param after The delay before the action runs.
 * @param action The action to run when the delay elapses.
 */
class Timeout(
    after: Duration,
    action: () -> Unit,
) {
    // MARK: - Properties

    private val job =
        scope.launch {
            delay(after)
            action()
        }

    // MARK: - Methods

    /** Cancels the action if it has not yet run. */
    fun cancel() {
        job.cancel()
    }

    // MARK: - Companion

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}
