//
//  ToastPresenterTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.toast

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.BuildInfoOverlay
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.Task

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
        Overlay.removeOverlay(animated = false)
        ToastPresenter.hide()
        Task.resetScope()
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
    fun `dismissing a toast frees the slot at once and restores the overlay a second later`() =
        runTest(dispatcher) {
            Persistent.setBoolean(PersistentStorageKey.hidesBuildInfoOverlay, false)
            BuildInfoOverlay.hide(persistSetting = false)

            ToastPresenter.show(Toast(message = "Shown"), onTap = null)
            ToastPresenter.dismiss()

            assertEquals(null, ToastPresenter.current.value)
            assertTrue(BuildInfoOverlay.isHidden.value)

            advanceTimeBy(1_100)
            assertFalse(BuildInfoOverlay.isHidden.value)
            BuildInfoOverlay.hide()
        }

    @Test
    fun `a non-modal overlay does not defer a toast`() =
        runTest(dispatcher) {
            Overlay.addOverlay(isModal = false)
            val toast = Toast(message = "Over the overlay")

            ToastPresenter.show(toast, onTap = null)

            assertEquals(toast, ToastPresenter.current.value?.toast)
            Overlay.removeOverlay(animated = false)
        }

    @Test
    fun `a modal overlay defers a toast until it is removed`() =
        runTest(dispatcher) {
            Overlay.addOverlay()
            val toast = Toast(message = "Behind the overlay")

            ToastPresenter.show(toast, onTap = null)
            advanceTimeBy(500)
            assertEquals(null, ToastPresenter.current.value)

            Overlay.removeOverlay(animated = false)
            advanceTimeBy(200)
            assertEquals(toast, ToastPresenter.current.value?.toast)
        }

    @Test
    fun `an animated removal unblocks after the fade`() =
        runTest(dispatcher) {
            Task.setScope(CoroutineScope(dispatcher))
            Overlay.addOverlay()
            val toast = Toast(message = "After the fade")

            ToastPresenter.show(toast, onTap = null)
            Overlay.removeOverlay()
            advanceTimeBy(150)
            assertEquals(null, ToastPresenter.current.value)

            advanceTimeBy(200)
            assertEquals(toast, ToastPresenter.current.value?.toast)
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
