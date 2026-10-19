//
//  MediaPickerServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources.contentpicker

import android.net.TestUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.modules.common.models.ContentPickerResult
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.localization.models.localized

/**
 * Exercises the media picker's confirmation flow: a selection is
 * confirmed through an action sheet before it is loaded and
 * delivered, cancelling delivers nothing, and a slow load shows the
 * modal progress display.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaPickerServiceTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()
    private val service = MediaPickerService()

    private var delivered: Result<ContentPickerResult>? = null
    private var hiddenHUDs = 0
    private var loadDelayMilliseconds = 0L
    private var loadFailure: Exception? = null
    private var loads = 0
    private var shownHUDs = 0
    private var wasCalled = false

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        AlertPresenter.dismiss()
        service.setDependencies(
            MediaPickerService.Dependencies(
                hideHUD = { hiddenHUDs += 1 },
                loadMedia = { selection ->
                    if (loadDelayMilliseconds > 0) delay(loadDelayMilliseconds)
                    loads += 1
                    loadFailure?.let { throw it }
                    selection
                },
                presentActionSheet = { actionSheet, _ -> actionSheet.present(translating = emptyList()) },
                showProgressHUD = { shownHUDs += 1 },
            ),
        )
        service.onDismiss {
            wasCalled = true
            delivered = it
        }
    }

    @After
    fun tearDown() {
        AlertPresenter.dismiss()
        service.setDependencies(null)
        Task.resetScope()
        Dispatchers.resetMain()
    }

    private fun presentedSheet(): PresentedAlert.ActionSheet? = AlertPresenter.current.value as? PresentedAlert.ActionSheet

    // MARK: - Tests

    @Test
    fun `a selection presents a preferred confirm action and a localized cancel button`() =
        runTest(dispatcher) {
            Task.setScope(this)

            service.handleResult(ContentPickerResult.Image(TestUri.allocate()))
            advanceUntilIdle()

            val sheet = presentedSheet()
            assertNotNull(sheet)
            assertEquals(listOf("Confirm"), sheet?.actions?.map { it.title })
            assertEquals(ActionStyle.PREFERRED, sheet?.actions?.first()?.style)
            assertEquals(LocalizedStringKey.Cancel.localized(), sheet?.cancelButtonTitle)
            assertTrue(!wasCalled)
        }

    @Test
    fun `cancelling the confirmation delivers a cancellation and loads nothing`() =
        runTest(dispatcher) {
            Task.setScope(this)

            service.handleResult(ContentPickerResult.Image(TestUri.allocate()))
            advanceUntilIdle()
            presentedSheet()?.onCancel()
            advanceUntilIdle()

            assertTrue(wasCalled)
            assertNull(delivered)
            assertEquals(0, loads)
            assertEquals(0, shownHUDs)
        }

    @Test
    fun `confirming loads the selection after the dismissal delay and delivers it`() =
        runTest(dispatcher) {
            Task.setScope(this)
            val selection = ContentPickerResult.Video(TestUri.allocate())

            service.handleResult(selection)
            advanceUntilIdle()
            presentedSheet()?.onSelect(0)
            advanceTimeBy(DISMISSAL_DELAY_MILLISECONDS)
            assertEquals(0, loads)

            advanceUntilIdle()
            assertSame(selection, delivered?.getOrNull())
            assertEquals(1, loads)
            assertEquals(0, shownHUDs)
            assertEquals(1, hiddenHUDs)
        }

    @Test
    fun `a slow load shows the modal progress display after one second`() =
        runTest(dispatcher) {
            Task.setScope(this)
            loadDelayMilliseconds = 2_000

            service.handleResult(ContentPickerResult.Image(TestUri.allocate()))
            advanceUntilIdle()
            presentedSheet()?.onSelect(0)
            advanceTimeBy(DISMISSAL_DELAY_MILLISECONDS + PROGRESS_TIMEOUT_MILLISECONDS - 1)
            assertEquals(0, shownHUDs)

            advanceTimeBy(2)
            assertEquals(1, shownHUDs)

            advanceUntilIdle()
            assertTrue(delivered?.isSuccess == true)
            assertEquals(1, hiddenHUDs)
        }

    @Test
    fun `a failed load delivers the failure and hides the display`() =
        runTest(dispatcher) {
            Task.setScope(this)
            loadFailure = Exception("Failed to process media.", metadata = ExceptionMetadata(this))

            service.handleResult(ContentPickerResult.Image(TestUri.allocate()))
            advanceUntilIdle()
            presentedSheet()?.onSelect(0)
            advanceUntilIdle()

            assertEquals("Failed to process media.", (delivered?.exceptionOrNull() as? Exception)?.descriptor)
            assertEquals(1, hiddenHUDs)
        }

    @Test
    fun `the picker's own cancellation delivers null without a confirmation`() =
        runTest(dispatcher) {
            Task.setScope(this)

            service.handleResult(null)
            advanceUntilIdle()

            assertTrue(wasCalled)
            assertNull(delivered)
            assertNull(presentedSheet())
        }

    private companion object {
        const val DISMISSAL_DELAY_MILLISECONDS = 250L
        const val PROGRESS_TIMEOUT_MILLISECONDS = 1_000L
    }
}
