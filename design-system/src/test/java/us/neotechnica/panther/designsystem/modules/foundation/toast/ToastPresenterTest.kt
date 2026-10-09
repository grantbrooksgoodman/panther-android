//
//  ToastPresenterTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.toast

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.services.BuildInfoOverlay
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

@OptIn(ExperimentalCoroutinesApi::class)
class ToastPresenterTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        Persistent.initializeForTesting()
        // Hide the build-info overlay so presentation does not defer
        // behind the overlay-hide delay.
        BuildInfoOverlay.hide()
        ToastPresenter.hide()
    }

    @After
    fun tearDown() {
        ToastPresenter.hide()
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `a second toast defers until the current one hides`() =
        runTest(dispatcher) {
            val first = Toast(message = "First")
            val second = Toast(message = "Second")

            ToastPresenter.show(first, onTap = null)
            assertEquals(first, ToastPresenter.current.value?.toast)

            ToastPresenter.show(second, onTap = null)
            assertEquals(first, ToastPresenter.current.value?.toast)

            ToastPresenter.hide()
            advanceTimeBy(1_100)
            assertEquals(second, ToastPresenter.current.value?.toast)
        }

    @Test
    fun `an identical toast request is ignored`() =
        runTest(dispatcher) {
            val toast = Toast(message = "Same")
            ToastPresenter.show(toast, onTap = null)
            val presented = ToastPresenter.current.value

            ToastPresenter.show(toast.copy(), onTap = null)
            advanceTimeBy(2_000)
            assertEquals(presented, ToastPresenter.current.value)
        }
}
