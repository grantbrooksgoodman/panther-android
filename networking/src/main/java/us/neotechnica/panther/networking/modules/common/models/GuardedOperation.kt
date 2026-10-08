//
//  GuardedOperation.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.models

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.health.models.HealthSampleToken
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import kotlin.time.Duration

// Shared precondition, timeout, and settlement machinery for
// one-shot network operations. Operation coalescing is deliberately
// not provided here; it remains the sole responsibility of each
// service's performOperation entry point.
internal object GuardedOperation {
    // MARK: - Properties

    private const val MILLIS_PER_SECOND = 1000.0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // MARK: - Methods

    fun checkPreconditions(sender: Any) {
        if (!Networking.isReadWriteEnabled) {
            throw Exception.Networking.readWriteAccessDisabled(ExceptionMetadata(sender))
        }

        if (!Build.isOnline) {
            throw Exception.internetConnectionOffline(ExceptionMetadata(sender))
        }
    }

    suspend fun <T> run(
        timeout: Duration = Networking.defaultOperationTimeout,
        recordsCensoredSampleOnTimeout: Boolean,
        showsActivityIndicator: Boolean,
        sender: Any,
        body: suspend (HealthSampleToken) -> T,
    ): T {
        currentCoroutineContext().ensureActive()
        checkPreconditions(sender)
        if (showsActivityIndicator) Networking.config.activityIndicatorDelegate.show()

        val didSettle = LockIsolated(false)

        fun canSettle(): Boolean =
            didSettle.withValue {
                if (it.value) {
                    false
                } else {
                    it.value = true
                    true
                }
            }

        val healthToken = HealthSampleToken()
        val settlement = CompletableDeferred<Result<T>>()

        val timeoutJob =
            scope.launch {
                delay(timeout)
                if (!canSettle()) return@launch

                if (showsActivityIndicator) Networking.config.activityIndicatorDelegate.hide()
                if (recordsCensoredSampleOnTimeout && healthToken.claim()) {
                    Networking.config.healthDelegate.recordCensoredLatencySample(
                        timeout.inWholeMilliseconds / MILLIS_PER_SECOND,
                    )
                }

                settlement.complete(
                    Result.failure(Exception.timedOut(ExceptionMetadata(sender))),
                )
            }

        // The settlement flow runs in its own scope so that a
        // cancelled caller can abandon the wait; the timeout bounds
        // the operation's settlement regardless of whether anyone is
        // still awaiting it, and late results are absorbed by the
        // single-settlement guard.
        scope.launch {
            val result = runCatching { body(healthToken) }
            timeoutJob.cancel()
            if (!canSettle()) return@launch

            if (showsActivityIndicator) Networking.config.activityIndicatorDelegate.hide()
            settlement.complete(result)
        }

        return settlement.await().getOrThrow()
    }
}
