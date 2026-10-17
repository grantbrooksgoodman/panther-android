//
//  HUDPresenterTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.hud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheet
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets
import us.neotechnica.panther.designsystem.modules.foundation.services.UserInteractionBlocker
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class HUDPresenterTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        Persistent.initializeForTesting()
        HUDPresenter.hide()
        AlertPresenter.dismiss()
    }

    @After
    fun tearDown() {
        HUDPresenter.hide()
        Overlay.removeOverlay(animated = false)
        AlertPresenter.dismiss()
        RootSheets.dismiss()
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `hide clears the presentation immediately, before the delay elapses`() =
        runTest(dispatcher) {
            HUDPresenter.showProgress(text = null, after = null, isModal = false)
            assertNotNull(HUDPresenter.presentation.value)

            HUDPresenter.hide(after = 250.milliseconds)
            assertNull(HUDPresenter.presentation.value)
            assertFalse(HUDPresenter.isBlockingUserInteraction)
            advanceTimeBy(300)
        }

    @Test
    fun `a modal progress display dismisses a presented alert`() =
        runTest(dispatcher) {
            AlertPresenter.present(
                PresentedAlert.Standard(title = null, message = "Hello", actions = listOf(Action("OK") {})) {},
            )
            advanceTimeBy(200)
            assertNotNull(AlertPresenter.current.value)

            HUDPresenter.showProgress(text = null, after = null, isModal = true)
            advanceTimeBy(150)
            assertNull(AlertPresenter.current.value)

            HUDPresenter.hide()
            advanceTimeBy(300)
        }

    @Test
    fun `a modal progress display keeps root sheets presented`() =
        runTest(dispatcher) {
            RootSheets.present(RootSheet {})
            HUDPresenter.showProgress(text = null, after = null, isModal = true)
            advanceTimeBy(350)

            assertNotNull(RootSheets.sheet.value)

            HUDPresenter.hide()
            advanceTimeBy(300)
        }

    @Test
    fun `hiding under a modal overlay keeps interaction blocked`() =
        runTest(dispatcher) {
            Overlay.addOverlay()
            HUDPresenter.showProgress(text = null, after = null, isModal = true)
            assertTrue(UserInteractionBlocker.isBlockingUserInteraction)

            HUDPresenter.hide()
            assertFalse(HUDPresenter.isBlockingUserInteraction)
            assertTrue(UserInteractionBlocker.isBlockingUserInteraction)

            Overlay.removeOverlay(animated = false)
            assertFalse(UserInteractionBlocker.isBlockingUserInteraction)
            advanceTimeBy(300)
        }
}
