//
//  NetworkHealthProberTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.networking.modules.health.extensions.isNetworkLevelFailure
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthEvent
import us.neotechnica.panther.networking.modules.health.services.HealthEventListener
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkHealthProberTest {
    // MARK: - Types

    private class Recorder(
        isMetricsEnabled: Boolean = true,
        latencyCeiling: Double = 5.0,
    ) {
        val events = mutableListOf<NetworkHealthEvent>()
        val throughputSamples = mutableListOf<Pair<Int, Double>>()
        var nanos = 0L

        val listener =
            HealthEventListener(
                onEvent = { events.add(it) },
                recordThroughputSample = { bytes, seconds -> throughputSamples.add(bytes to seconds) },
                isMetricsEnabled = { isMetricsEnabled },
                latencyCeiling = { latencyCeiling },
                now = { nanos },
            )
    }

    // MARK: - Tests

    @Test
    fun `a fresh connection records the DNS plus connect handshake and the body throughput`() {
        val recorder = Recorder()
        with(recorder) {
            listener.dnsStarted()
            nanos = 100_000_000L
            listener.dnsEnded()
            listener.connectStarted()
            nanos = 300_000_000L
            listener.connectEnded()
            listener.connectionAcquired()
            listener.responseBodyStarted()
            nanos = 800_000_000L
            listener.responseBodyEnded(2_048)
            listener.callEnded(error = null)
        }

        val handshake = recorder.events.single() as NetworkHealthEvent.Handshake
        assertEquals(0.3, handshake.seconds, EPSILON)
        assertEquals(listOf(2_048 to 0.5), recorder.throughputSamples)
    }

    @Test
    fun `a reused connection records no handshake`() {
        val recorder = Recorder()
        with(recorder) {
            listener.connectionAcquired()
            listener.responseBodyStarted()
            nanos = 500_000_000L
            listener.responseBodyEnded(10)
            listener.callEnded(error = null)
        }

        assertTrue(recorder.events.isEmpty())
        assertEquals(listOf(10 to 0.5), recorder.throughputSamples)
    }

    @Test
    fun `an absurd handshake on a failed call is skipped`() {
        val recorder = Recorder(latencyCeiling = 1.0)
        with(recorder) {
            listener.connectStarted()
            nanos = 3_000_000_000L
            listener.connectEnded()
            listener.callEnded(error = IOException("Reset"))
        }

        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `disabled metrics record nothing`() {
        val recorder = Recorder(isMetricsEnabled = false)
        with(recorder) {
            listener.connectStarted()
            nanos = 200_000_000L
            listener.connectEnded()
            listener.callEnded(error = null)
        }

        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `only the four connection failures are network level`() {
        assertTrue(UnknownHostException("host").isNetworkLevelFailure)
        assertTrue(ConnectException("refused").isNetworkLevelFailure)
        assertTrue(SocketTimeoutException("timeout").isNetworkLevelFailure)
        assertTrue(SocketException("reset").isNetworkLevelFailure)
        assertTrue(!IOException("other").isNetworkLevelFailure)
        assertTrue(!IllegalStateException("state").isNetworkLevelFailure)
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
