//
//  BuildDeveloperModeTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import java.util.Date

/** Verifies the developer-mode members ported in Parity II Phase 9.4. */
class BuildDeveloperModeTest {
    @Before
    fun setUp() {
        Persistent.initializeForTesting()
    }

    @Test
    fun `expiration override code derives from code name letter positions`() {
        configure(Milestone.BETA)
        // "Hello": H (08), middle 'l' (12), o (15).
        assertEquals("081215", Build.expirationOverrideCode)
    }

    @Test
    fun `developer mode toggles and persists on prerelease builds`() {
        configure(Milestone.BETA)
        assertFalse(Build.isDeveloperModeEnabled)

        Build.setIsDeveloperModeEnabled(true)
        assertTrue(Build.isDeveloperModeEnabled)

        Build.setIsDeveloperModeEnabled(false)
        assertFalse(Build.isDeveloperModeEnabled)
    }

    @Test
    fun `developer mode is always disabled on general-release builds`() {
        configure(Milestone.GENERAL_RELEASE)
        Build.setIsDeveloperModeEnabled(true)
        assertFalse(Build.isDeveloperModeEnabled)
    }

    private fun configure(milestone: Milestone) {
        Build.initialize(
            appStoreBuildNumber = 0,
            buildNumber = 1,
            codeName = "Hello",
            finalName = "Hello",
            bundleVersion = "1.0.0",
            environment = "development",
            milestone = milestone,
            buildDate = Date(),
            firstCompileDate = Date(),
        )
    }
}
