//
//  HealthEvidenceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.networking.modules.health.models.HealthEvidence
import us.neotechnica.panther.networking.modules.health.models.HealthSampleToken
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * Exercises health evidence classification and the once-only
 * sample token.
 */
@Suppress("MagicNumber")
class HealthEvidenceTest {
    @Test
    fun `success classifies as latency`() {
        val evidence = HealthEvidence.classify(error = null, elapsed = 0.25)
        assertTrue(evidence is HealthEvidence.Latency)
        assertEquals(0.25, (evidence as HealthEvidence.Latency).seconds, 0.0)
    }

    @Test
    fun `no value exists classifies as latency`() {
        val exception =
            Exception(
                "No value exists at the specified key path.",
                metadata = ExceptionMetadata(this),
            )

        assertTrue(HealthEvidence.classify(error = exception, elapsed = 0.1) is HealthEvidence.Latency)
    }

    @Test
    fun `storage item does not exist classifies as latency`() {
        val exception =
            Exception(
                "No item exists at the specified key path.",
                userInfo = mapOf(Exception.UserInfo.STATIC_ERROR_CODE.rawValue to "9207"),
                metadata = ExceptionMetadata(this),
            )

        assertTrue(HealthEvidence.classify(error = exception, elapsed = 0.1) is HealthEvidence.Latency)
    }

    @Test
    fun `other errors classify as no evidence`() {
        val exception =
            Exception(
                "The operation failed.",
                metadata = ExceptionMetadata(this),
            )

        assertEquals(HealthEvidence.NoEvidence, HealthEvidence.classify(error = exception, elapsed = 0.1))
    }

    @Test
    fun `token can only be claimed once`() {
        val token = HealthSampleToken()
        assertTrue(token.claim())
        assertFalse(token.claim())
    }
}
