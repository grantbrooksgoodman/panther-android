//
//  HealthEvidence.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.health.interfaces.NetworkHealthDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

// Centralized classifier for network operation outcomes. All call
// sites funnel through this type to determine what, if anything, to
// record as a health sample, preventing the classification logic
// from drifting across instrumentation seams.
internal sealed interface HealthEvidence {
    // MARK: - Types

    // The operation completed a network round-trip; the elapsed
    // time is a valid latency observation.
    data class Latency(
        val seconds: Double,
    ) : HealthEvidence

    // The operation's outcome carries no evidence about network
    // quality (pre-network guard, validation error, etc.).
    data object NoEvidence : HealthEvidence

    // MARK: - Companion

    companion object {
        private const val MILLIS_PER_SECOND = 1000.0

        fun classify(
            error: Exception?,
            elapsed: Double,
        ): HealthEvidence {
            error ?: return Latency(seconds = elapsed)

            // "No value exists" and "item does not exist" mean the
            // server responded – the round-trip completed
            // successfully from a network perspective.
            if (error.isEqual(
                    toAny =
                        listOf(
                            AppException.Networking.Database.noValueExists,
                            AppException.Networking.Storage.storageItemDoesNotExist,
                        ),
                )
            ) {
                return Latency(seconds = elapsed)
            }

            return NoEvidence
        }

        // Measures a single network round-trip, recording the
        // appropriate health sample for its outcome and rethrowing
        // any failure. Wrap only the network call itself – local
        // pre- and post-processing should stay outside the
        // operation closure.
        suspend fun <T> measure(
            token: HealthSampleToken = HealthSampleToken(),
            delegate: NetworkHealthDelegate = Networking.config.healthDelegate,
            operation: suspend () -> T,
        ): T {
            val startTimeMillis = System.currentTimeMillis()

            try {
                val value = operation()

                record(
                    error = null,
                    startTimeMillis = startTimeMillis,
                    token = token,
                    delegate = delegate,
                )

                return value
            } catch (error: Exception) {
                record(
                    error = error,
                    startTimeMillis = startTimeMillis,
                    token = token,
                    delegate = delegate,
                )

                throw error
            }
        }

        fun record(
            error: Exception?,
            startTimeMillis: Long,
            token: HealthSampleToken,
            delegate: NetworkHealthDelegate = Networking.config.healthDelegate,
        ) {
            if (!token.claim()) return
            val elapsed = (System.currentTimeMillis() - startTimeMillis) / MILLIS_PER_SECOND

            when (
                val evidence =
                    classify(
                        error = error,
                        elapsed = elapsed,
                    )
            ) {
                is Latency -> delegate.recordLatencySample(evidence.seconds)
                NoEvidence -> Unit
            }
        }
    }
}
