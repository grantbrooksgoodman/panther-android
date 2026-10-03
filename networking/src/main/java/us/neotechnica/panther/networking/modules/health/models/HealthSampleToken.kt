//
//  HealthSampleToken.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * A once-only recording guard for health instrumentation, mirroring the
 * iOS `HealthSampleToken`.
 *
 * Each instrumented operation creates a single token shared between the
 * timeout handler and the operation's completion path. Exactly one of
 * {success sample, censored timeout sample, discard} is recorded per
 * operation – the first caller to successfully [claim] the token owns
 * the recording; all subsequent callers are rejected.
 */
internal class HealthSampleToken {
    // MARK: - Properties

    private val didRecord = LockIsolated(false)

    // MARK: - Methods

    /**
     * Atomically attempts to claim the token.
     *
     * @return `true` if this caller won the claim (and should record a
     *   sample); `false` if another caller already claimed it.
     */
    fun claim(): Boolean =
        didRecord.withValue {
            if (it.value) {
                false
            } else {
                it.value = true
                true
            }
        }
}
