//
//  DataSampleTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.networking.modules.common.models.DataSample
import us.neotechnica.panther.networking.modules.translation.models.TranslationDataSample

class DataSampleTest {
    // MARK: - Tests

    @Test
    fun `a sample expires on whole seconds only`() {
        val now = System.currentTimeMillis()

        assertFalse(DataSample("value", expiryThreshold = 250L, date = now - 900L).isExpired)
        assertTrue(DataSample("value", expiryThreshold = 250L, date = now - 1_000L).isExpired)
    }

    @Test
    fun `a translation snapshot expires on whole seconds only`() {
        val now = System.currentTimeMillis()

        assertFalse(TranslationDataSample(emptyMap(), expiryThreshold = 250L, date = now - 900L).isExpired)
        assertTrue(TranslationDataSample(emptyMap(), expiryThreshold = 250L, date = now - 1_000L).isExpired)
    }

    @Test
    fun `only the placeholder snapshot is unpopulated`() {
        assertFalse(TranslationDataSample.empty.isPopulated)
        assertTrue(TranslationDataSample(emptyMap(), expiryThreshold = 250L).isPopulated)
    }
}
