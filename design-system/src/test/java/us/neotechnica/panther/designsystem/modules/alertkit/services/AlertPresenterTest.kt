//
//  AlertPresenterTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

@OptIn(ExperimentalCoroutinesApi::class)
class AlertPresenterTest {
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
        Overlay.removeOverlay(animated = false)
        AlertPresenter.dismiss()
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `a second alert queues until the current one dismisses`() =
        runTest(dispatcher) {
            val first = standardAlert("First")
            val second = standardAlert("Second")

            AlertPresenter.present(first)
            advanceUntilIdle()
            assertEquals(first, AlertPresenter.current.value)

            AlertPresenter.present(second)
            advanceTimeBy(1_000)
            assertEquals(first, AlertPresenter.current.value)

            AlertPresenter.dismiss()
            advanceTimeBy(200)
            assertEquals(second, AlertPresenter.current.value)
        }

    @Test
    fun `dismissing a presented alert invokes its displaced closure`() =
        runTest(dispatcher) {
            var displaced = false
            AlertPresenter.present(standardAlert("Displaced"), onDisplaced = { displaced = true })
            advanceUntilIdle()

            AlertPresenter.dismiss()
            assertTrue(displaced)
            assertNull(AlertPresenter.current.value)
        }

    @Test
    fun `queued alerts present in the order they were requested`() =
        runTest(dispatcher) {
            val alerts = listOf("First", "Second", "Third", "Fourth").map { standardAlert(it) }

            alerts.forEach { AlertPresenter.present(it) }
            runCurrent()

            alerts.forEach { alert ->
                assertEquals(alert, AlertPresenter.current.value)
                AlertPresenter.dismiss()
                advanceTimeBy(200)
            }

            assertNull(AlertPresenter.current.value)
        }

    @Test
    fun `withdrawing a queued alert leaves the presented alert in place`() =
        runTest(dispatcher) {
            val first = standardAlert("First")
            val second = standardAlert("Second")

            AlertPresenter.present(first)
            AlertPresenter.present(second)
            runCurrent()

            AlertPresenter.withdraw(second)
            runCurrent()
            assertEquals(first, AlertPresenter.current.value)

            AlertPresenter.dismiss()
            advanceTimeBy(200)
            assertNull(AlertPresenter.current.value)
        }

    @Test
    fun `withdrawing the presented alert dismisses it and presents the next`() =
        runTest(dispatcher) {
            val first = standardAlert("First")
            val second = standardAlert("Second")

            AlertPresenter.present(first)
            AlertPresenter.present(second)
            runCurrent()

            AlertPresenter.withdraw(first)
            advanceTimeBy(200)
            assertEquals(second, AlertPresenter.current.value)

            AlertPresenter.dismiss()
            advanceTimeBy(200)
        }

    @Test
    fun `isPresenting reflects whether an alert is presented`() =
        runTest(dispatcher) {
            assertFalse(AlertPresenter.isPresenting)

            AlertPresenter.present(standardAlert("Presented"))
            runCurrent()
            assertTrue(AlertPresenter.isPresenting)

            AlertPresenter.dismiss()
            assertFalse(AlertPresenter.isPresenting)
        }

    @Test
    fun `a modal overlay defers presentation until it is removed`() =
        runTest(dispatcher) {
            val alert = standardAlert("Deferred")
            Overlay.addOverlay()

            AlertPresenter.present(alert)
            advanceTimeBy(1_000)
            assertNull(AlertPresenter.current.value)

            Overlay.removeOverlay(animated = false)
            advanceTimeBy(200)
            assertEquals(alert, AlertPresenter.current.value)
        }

    @Test
    fun `a modal overlay dismisses the presented alert`() =
        runTest(dispatcher) {
            var displaced = false
            AlertPresenter.present(standardAlert("Presented"), onDisplaced = { displaced = true })
            runCurrent()

            Overlay.addOverlay()
            assertNull(AlertPresenter.current.value)
            assertTrue(displaced)

            Overlay.removeOverlay(animated = false)
            advanceTimeBy(200)
        }

    // MARK: - Auxiliary

    private fun standardAlert(title: String): PresentedAlert.Standard =
        PresentedAlert.Standard(
            title = title,
            message = null,
            actions = listOf(Action("OK") {}),
        ) {}
}
