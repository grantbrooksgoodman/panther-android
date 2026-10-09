//
//  TransferProgressProbe.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.storage.models.StorageTransferProgress
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * A per-transfer health probe that converts storage transfer progress
 * into mid-flight throughput samples and stall evidence.
 *
 * Feed each progress snapshot to [handleProgress]; whenever the
 * accumulated segment reaches `minimumThroughputSampleBytes`, the probe
 * records a throughput sample for that segment. A watchdog reports a
 * single [NetworkHealthEvent.TransferStall] if progress freezes for
 * `transferStallSeconds` while the transfer is active.
 *
 * Call [finish] on success – it records the final partial segment, or a
 * single whole-transfer sample when no segment was ever emitted – or
 * [invalidate] on failure. Either call cancels the watchdog.
 */
internal class TransferProgressProbe {
    // MARK: - Types

    private class MutableState(
        val startedAt: Long = System.currentTimeMillis(),
    ) {
        var didEmitSegment = false
        var didReportStall = false
        var isFinished = false
        var lastCompletedBytes: Long = 0
        var lastProgressAt: Long = startedAt
        var segmentBytes: Int = 0
        var segmentStartedAt: Long = startedAt
    }

    // MARK: - Properties

    private val state = LockIsolated(MutableState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val watchdogJob: Job = makeWatchdogTask()

    // MARK: - Methods

    /**
     * Incorporates a progress snapshot, recording a throughput sample
     * whenever the accumulated segment reaches the minimum sample size.
     *
     * @param progress The transfer's latest progress snapshot.
     */
    fun handleProgress(progress: StorageTransferProgress) {
        val minimumThroughputSampleBytes = Networking.config.networkHealthConfiguration.minimumThroughputSampleBytes
        val now = System.currentTimeMillis()

        val sample: Pair<Int, Double>? =
            state.withValue { reference ->
                val mutableState = reference.value
                if (mutableState.isFinished) return@withValue null

                val deltaBytes = progress.completedBytes - mutableState.lastCompletedBytes
                if (deltaBytes <= 0) return@withValue null

                mutableState.lastCompletedBytes = progress.completedBytes
                mutableState.lastProgressAt = now
                mutableState.segmentBytes += deltaBytes.toInt()

                if (mutableState.segmentBytes < minimumThroughputSampleBytes) return@withValue null

                val sample = mutableState.segmentBytes to (now - mutableState.segmentStartedAt) / MILLIS_PER_SECOND
                mutableState.didEmitSegment = true
                mutableState.segmentBytes = 0
                mutableState.segmentStartedAt = now
                sample
            }

        record(sample)
    }

    /**
     * Marks the transfer as successfully completed, recording the final
     * partial segment – or, when no segment was ever emitted, a single
     * whole-transfer sample for the given byte count.
     *
     * @param totalBytes The total number of bytes transferred, or `null`.
     */
    fun finish(totalBytes: Int?) {
        val now = System.currentTimeMillis()

        val sample: Pair<Int, Double>? =
            state.withValue { reference ->
                val mutableState = reference.value
                if (mutableState.isFinished) return@withValue null
                mutableState.isFinished = true

                if (mutableState.didEmitSegment) {
                    if (mutableState.segmentBytes <= 0) {
                        null
                    } else {
                        mutableState.segmentBytes to (now - mutableState.segmentStartedAt) / MILLIS_PER_SECOND
                    }
                } else {
                    totalBytes?.let { it to (now - mutableState.startedAt) / MILLIS_PER_SECOND }
                }
            }

        watchdogJob.cancel()
        record(sample)
    }

    /** Marks the transfer as failed, cancelling the watchdog without recording a completion sample. */
    fun invalidate() {
        val shouldCancel =
            state.withValue {
                if (it.value.isFinished) {
                    false
                } else {
                    it.value.isFinished = true
                    true
                }
            }

        if (shouldCancel) watchdogJob.cancel()
    }

    // MARK: - Auxiliary

    private fun makeWatchdogTask(): Job =
        scope.launch {
            runWatchdog()
        }

    private suspend fun runWatchdog() {
        while (scope.isActive) {
            val checkInterval = Networking.config.networkHealthConfiguration.transferStallCheckInterval
            delay((checkInterval * MILLIS_PER_SECOND).toLong())
            if (!scope.isActive) return

            val transferStallSeconds = Networking.config.networkHealthConfiguration.transferStallSeconds
            val shouldReportStall =
                state.withValue { reference ->
                    val mutableState = reference.value
                    if (mutableState.isFinished ||
                        mutableState.didReportStall ||
                        (System.currentTimeMillis() - mutableState.lastProgressAt) / MILLIS_PER_SECOND < transferStallSeconds
                    ) {
                        false
                    } else {
                        mutableState.didReportStall = true
                        true
                    }
                }

            if (!shouldReportStall) continue
            Networking.health.record(NetworkHealthEvent.TransferStall)
            return
        }
    }

    private fun record(sample: Pair<Int, Double>?) {
        sample ?: return
        Networking.health.recordThroughputSample(sample.first, sample.second)
    }

    // MARK: - Companion

    companion object {
        private const val MILLIS_PER_SECOND = 1000.0

        /**
         * Runs a transfer with an attached probe, composing the
         * probe's progress sink with the caller's, finishing the
         * probe on success and invalidating it on failure.
         *
         * The `totalBytes` closure is evaluated only after the
         * transfer succeeds, so it may reference artifacts the
         * transfer produces – such as a downloaded file on disk.
         *
         * @param totalBytes A closure that returns the total
         *   number of bytes transferred, or `null` when unknown.
         * @param onProgress The caller's progress sink, invoked
         *   alongside the probe's for each snapshot.
         * @param transfer The transfer to run, given the composed
         *   progress sink.
         *
         * @throws Exception if the transfer fails, after
         *   invalidating the probe.
         */
        suspend fun measure(
            totalBytes: () -> Int?,
            onProgress: ((StorageTransferProgress) -> Unit)?,
            transfer: suspend (onProgress: (StorageTransferProgress) -> Unit) -> Unit,
        ) {
            val probe = TransferProgressProbe()

            try {
                transfer { progress ->
                    probe.handleProgress(progress)
                    onProgress?.invoke(progress)
                }
            } catch (throwable: Throwable) {
                probe.invalidate()
                throw throwable
            }

            probe.finish(totalBytes = totalBytes())
        }
    }
}
