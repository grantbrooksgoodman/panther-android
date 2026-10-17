//
//  DevModeSubsystemActionsTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.developermode.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

@OptIn(ExperimentalCoroutinesApi::class)
class DevModeSubsystemActionsTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        Persistent.initializeForTesting()
        AlertPresenter.dismiss()
    }

    @After
    fun tearDown() {
        AlertPresenter.dismiss()
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `the erase sheet lists the application support directory action`() =
        runTest(dispatcher) {
            DevModeSubsystemActions.available
                .first { it.title == "Erase Content & Settings" }
                .perform()
            advanceUntilIdle()

            val sheet = AlertPresenter.current.value as PresentedAlert.ActionSheet
            assertEquals(
                listOf(
                    "Clear Caches",
                    "Erase Application Support Directory",
                    "Erase Documents Directory",
                    "Erase Temporary Directory",
                    "Reset Preferences",
                    "Erase All Content & Settings",
                ),
                sheet.actions.map { it.title },
            )
        }

    @Test
    fun `the subsystem actions keep their order`() {
        assertEquals(
            listOf("Erase Content & Settings", "Toggle Build Info Overlay", "Override Language Code"),
            DevModeSubsystemActions.available.map { it.title.replace(Regex("^(Show|Hide) "), "Toggle ").replace(Regex("^(Override|Restore) "), "Override ") },
        )
    }
}
