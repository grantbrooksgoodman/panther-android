//
//  AlertKitConfigTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.models.overriddenLanguageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

class AlertKitConfigTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        clearLanguageCodes()
    }

    @After
    fun tearDown() {
        clearLanguageCodes()
    }

    // MARK: - Tests

    @Test
    fun `the target language follows every language code change`() {
        CoreUtilities.setLanguageCode("fr", override = true)
        assertEquals("fr", AlertKitConfig.targetLanguageCode)

        CoreUtilities.setLanguageCode("de", override = true)
        assertEquals("de", AlertKitConfig.targetLanguageCode)

        RuntimeStorage.remove(StoredItemKey.overriddenLanguageCode)
        CoreUtilities.setLanguageCode("es")
        assertEquals("es", AlertKitConfig.targetLanguageCode)
    }

    @Test
    fun `an overridden language code takes precedence over the stored one`() {
        CoreUtilities.setLanguageCode("es")
        CoreUtilities.setLanguageCode("fr", override = true)
        CoreUtilities.setLanguageCode("de")

        assertEquals("fr", AlertKitConfig.targetLanguageCode)
    }

    // MARK: - Auxiliary

    private fun clearLanguageCodes() {
        RuntimeStorage.remove(StoredItemKey.languageCode)
        RuntimeStorage.remove(StoredItemKey.overriddenLanguageCode)
    }
}
