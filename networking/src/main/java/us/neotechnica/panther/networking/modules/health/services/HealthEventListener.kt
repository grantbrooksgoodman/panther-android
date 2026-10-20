//
//  HealthEventListener.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.services

import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthEvent
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

// An allocation-cheap per-call listener that converts a call's
// connection events into health evidence.
//
// From a fresh connection, the DNS-plus-connect handshake
// contributes a latency sample, and the response body contributes a
// throughput sample subject to the estimator's minimum sample size.
// Total request duration is never recorded. Events are stashed as
// they arrive and processed when the call ends, where the call's
// error is authoritative. The listener never throws.
internal class HealthEventListener(
    private val onEvent: (NetworkHealthEvent) -> Unit = { Networking.config.healthDelegate.record(it) },
    private val recordThroughputSample: (bytes: Int, seconds: Double) -> Unit = { bytes, seconds ->
        Networking.config.healthDelegate.recordThroughputSample(bytes, seconds)
    },
    private val isMetricsEnabled: () -> Boolean = { Networking.config.networkHealthConfiguration.isURLSessionMetricsEnabled },
    private val latencyCeiling: () -> Double = { Networking.config.networkHealthConfiguration.latencyCeiling },
    private val now: () -> Long = System::nanoTime,
) : EventListener() {
    // MARK: - Types

    private data class Metrics(
        val connectEnd: Long? = null,
        val connectStart: Long? = null,
        val dnsEnd: Long? = null,
        val dnsStart: Long? = null,
        val isReusedConnection: Boolean = true,
        val responseBodyBytes: Long? = null,
        val responseBodyEnd: Long? = null,
        val responseBodyStart: Long? = null,
    )

    // MARK: - Properties

    private val metrics = LockIsolated(Metrics())

    // MARK: - EventListener Conformance

    override fun dnsStart(
        call: Call,
        domainName: String,
    ) {
        dnsStarted()
    }

    override fun dnsEnd(
        call: Call,
        domainName: String,
        inetAddressList: List<InetAddress>,
    ) {
        dnsEnded()
    }

    override fun connectStart(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
    ) {
        connectStarted()
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: okhttp3.Protocol?,
    ) {
        connectEnded()
    }

    override fun connectionAcquired(
        call: Call,
        connection: Connection,
    ) {
        connectionAcquired()
    }

    override fun responseBodyStart(call: Call) {
        responseBodyStarted()
    }

    override fun responseBodyEnd(
        call: Call,
        byteCount: Long,
    ) {
        responseBodyEnded(byteCount)
    }

    override fun callEnd(call: Call) {
        callEnded(error = null)
    }

    override fun callFailed(
        call: Call,
        ioe: IOException,
    ) {
        callEnded(error = ioe)
    }

    // MARK: - Methods

    fun dnsStarted() {
        metrics.withValue { it.value = it.value.copy(dnsStart = now()) }
    }

    fun dnsEnded() {
        metrics.withValue { it.value = it.value.copy(dnsEnd = now()) }
    }

    fun connectStarted() {
        metrics.withValue { it.value = it.value.copy(connectStart = now(), isReusedConnection = false) }
    }

    fun connectEnded() {
        metrics.withValue { it.value = it.value.copy(connectEnd = now()) }
    }

    // A connection acquired without a connect is a reused one.
    fun connectionAcquired() {
        metrics.withValue { reference ->
            if (reference.value.connectStart == null) reference.value = reference.value.copy(isReusedConnection = true)
        }
    }

    fun responseBodyStarted() {
        metrics.withValue { it.value = it.value.copy(responseBodyStart = now()) }
    }

    fun responseBodyEnded(byteCount: Long) {
        metrics.withValue { it.value = it.value.copy(responseBodyBytes = byteCount, responseBodyEnd = now()) }
    }

    fun callEnded(error: Throwable?) {
        if (!isMetricsEnabled()) return
        val collected = metrics.withValue { reference -> reference.value.also { reference.value = Metrics() } }
        recordHandshakeSampleIfAvailable(collected, error)
        recordThroughputSampleIfAvailable(collected)
    }

    // MARK: - Auxiliary

    private fun recordHandshakeSampleIfAvailable(
        collected: Metrics,
        error: Throwable?,
    ) {
        if (collected.isReusedConnection) return

        var handshakeSeconds = 0.0
        var hasComponent = false

        val dnsStart = collected.dnsStart
        val dnsEnd = collected.dnsEnd
        if (dnsStart != null && dnsEnd != null) {
            handshakeSeconds += (dnsEnd - dnsStart) / NANOS_PER_SECOND
            hasComponent = true
        }

        val connectStart = collected.connectStart
        val connectEnd = collected.connectEnd
        if (connectStart != null && connectEnd != null) {
            handshakeSeconds += (connectEnd - connectStart) / NANOS_PER_SECOND
            hasComponent = true
        }

        if (!hasComponent || handshakeSeconds <= 0) return

        // A slow handshake on a successful call is valid evidence;
        // implausible values are skipped only when the call also
        // failed.
        if (error != null && handshakeSeconds > latencyCeiling()) return

        onEvent(NetworkHealthEvent.Handshake(seconds = handshakeSeconds))
    }

    private fun recordThroughputSampleIfAvailable(collected: Metrics) {
        val bytes = collected.responseBodyBytes ?: return
        val start = collected.responseBodyStart ?: return
        val end = collected.responseBodyEnd ?: return
        val seconds = (end - start) / NANOS_PER_SECOND
        if (bytes <= 0 || seconds <= 0) return

        recordThroughputSample(bytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), seconds)
    }

    // MARK: - Companion

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
