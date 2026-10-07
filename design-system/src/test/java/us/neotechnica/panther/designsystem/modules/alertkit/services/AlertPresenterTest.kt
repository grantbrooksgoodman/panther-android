//
//  AlertPresenterTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action

@OptIn(ExperimentalCoroutinesApi::class)
class AlertPresenterTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        AlertPresenter.dismiss()
    }

    @After
    fun tearDown() {
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

    // MARK: - Auxiliary

    private fun standardAlert(title: String): PresentedAlert.Standard =
        PresentedAlert.Standard(
            title = title,
            message = null,
            actions = listOf(Action("OK") {}),
        ) {}
}
