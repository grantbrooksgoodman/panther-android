//
//  ContinuationGuard.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.CancellableContinuation
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import kotlin.coroutines.resume

internal class ContinuationGuard<T>(
    continuation: CancellableContinuation<T>,
    private val fallbackValue: T,
) {
    // MARK: - Properties

    private val continuation = LockIsolated<CancellableContinuation<T>?>(continuation)

    // MARK: - Resume

    fun fallback() {
        resume(fallbackValue)
    }

    fun resume(value: T) {
        val continuation =
            this.continuation.withValue { ref ->
                val current = ref.value
                ref.value = null
                current
            } ?: return

        if (continuation.isActive) continuation.resume(value)
    }
}
