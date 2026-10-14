//
//  DevModeServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.developermode.services

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
import us.neotechnica.panther.designsystem.modules.developermode.interfaces.DevModeAppActionDelegate
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Exercises the Developer Mode service's app-domain list: delegate
 * registration, positional and relative insertion, and removal – each
 * observed through the presented app-domain sheet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DevModeServiceTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()

    private val first = DevModeAction(title = "First") {}
    private val second = DevModeAction(title = "Second") {}
    private val third = DevModeAction(title = "Third") {}

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        Persistent.initializeForTesting()
        AlertPresenter.dismiss()
        DevModeService.registerAppActionDelegate(delegate(listOf(first, second, third)))
    }

    @After
    fun tearDown() {
        DevModeService.registerAppActionDelegate(delegate(emptyList()))
        AlertPresenter.dismiss()
        Dispatchers.resetMain()
    }

    private fun delegate(actions: List<DevModeAction>): DevModeAppActionDelegate =
        object : DevModeAppActionDelegate {
            override val appActions: List<DevModeAction> = actions
        }

    // Presents the menu, chooses the app domain, and returns the sheet's
    // action titles.
    private fun kotlinx.coroutines.test.TestScope.appDomainTitles(): List<String> {
        DevModeService.presentActionSheet()
        advanceUntilIdle()
        (AlertPresenter.current.value as PresentedAlert.ActionSheet).onSelect(0)
        advanceUntilIdle()
        return (AlertPresenter.current.value as PresentedAlert.ActionSheet).actions.map { it.title }
    }

    // MARK: - Tests

    @Test
    fun `the registered delegate's actions precede the Back item`() =
        runTest(dispatcher) {
            assertEquals(listOf("First", "Second", "Third"), appDomainTitles())
            assertEquals("Back", (AlertPresenter.current.value as PresentedAlert.ActionSheet).cancelButtonTitle)
        }

    @Test
    fun `inserting actions at an index keeps their order`() =
        runTest(dispatcher) {
            DevModeService.insertActions(listOf(DevModeAction(title = "A") {}, DevModeAction(title = "B") {}), at = 0)

            assertEquals(listOf("A", "B", "First", "Second", "Third"), appDomainTitles())
        }

    @Test
    fun `inserting before and after an action places it beside that action`() =
        runTest(dispatcher) {
            DevModeService.insertActionBefore(DevModeAction(title = "Before") {}, before = second)
            DevModeService.insertAction(DevModeAction(title = "After") {}, after = second)

            assertEquals(listOf("First", "Before", "Second", "After", "Third"), appDomainTitles())
        }

    @Test
    fun `inserting beside a missing action does nothing`() =
        runTest(dispatcher) {
            DevModeService.insertActionBefore(DevModeAction(title = "Before") {}, before = DevModeAction(title = "Missing") {})

            assertEquals(listOf("First", "Second", "Third"), appDomainTitles())
        }

    @Test
    fun `removing by index and by title drops the matching actions`() =
        runTest(dispatcher) {
            DevModeService.removeAction(at = 0)
            DevModeService.removeAction(withTitle = "Third")

            assertEquals(listOf("Second"), appDomainTitles())
        }
}
