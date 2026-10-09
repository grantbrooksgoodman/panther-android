//
//  LoggerPresentationServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey

@OptIn(ExperimentalCoroutinesApi::class)
class LoggerPresentationServiceTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Logger.setReportsErrorsAutomatically(false)
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `a reportable exception invites a report when reporting manually`() {
        Logger.setReportsErrorsAutomatically(false)
        val exception = Exception("Something failed.", metadata = ExceptionMetadata(this))

        val toast = LoggerPresentationService.toast(AlertType.Toast(), exception, exception.userFacingDescriptor)

        assertEquals(exception.userFacingDescriptor, toast.title)
        assertEquals(Localized(SubsystemStringKey.TAP_TO_REPORT).wrappedValue, toast.message)
        assertNotNull(LoggerPresentationService.reportAction(exception))
    }

    @Test
    fun `a reportable exception states it was reported when reporting automatically`() {
        Logger.setReportsErrorsAutomatically(true)
        val exception = Exception("Something failed.", metadata = ExceptionMetadata(this))

        val toast = LoggerPresentationService.toast(AlertType.Toast(), exception, exception.userFacingDescriptor)

        assertEquals(exception.userFacingDescriptor, toast.title)
        assertEquals(Localized(SubsystemStringKey.ERROR_REPORTED).wrappedValue, toast.message)
        assertNull(LoggerPresentationService.reportAction(exception))
    }

    @Test
    fun `a non-reportable exception shows its descriptor without a tap action`() {
        val exception =
            Exception(
                "Expected failure.",
                isReportable = false,
                metadata = ExceptionMetadata(this),
            )

        val toast = LoggerPresentationService.toast(AlertType.Toast(), exception, exception.userFacingDescriptor)

        assertNull(toast.title)
        assertEquals(exception.userFacingDescriptor, toast.message)
        assertNull(LoggerPresentationService.reportAction(exception))
    }

    @Test
    fun `text without an exception shows an informational toast`() {
        val toast = LoggerPresentationService.toast(AlertType.Toast(isPersistent = false), null, "Saved.")

        assertNull(toast.title)
        assertEquals("Saved.", toast.message)
        assertEquals(Toast.ToastType.Capsule(ToastStyle.INFO), toast.type)
        assertNull(LoggerPresentationService.reportAction(null))
    }
}
