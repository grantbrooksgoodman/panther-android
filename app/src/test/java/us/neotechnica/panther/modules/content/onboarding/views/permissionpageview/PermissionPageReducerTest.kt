//
//  PermissionPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.permissionpageview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.modules.common.extensions.contactAccessDenied
import us.neotechnica.panther.modules.common.services.PermissionService
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * Exercises the permission page reducer: contact permission outcomes,
 * including the access-denied exception treated as a denial, finish
 * button gating, and the non-modal overlay presented on finish.
 */
class PermissionPageReducerTest {
    // MARK: - Setup

    private val reducer = PermissionPageReducer()

    @After
    fun tearDown() {
        Overlay.removeOverlay(animated = false)
    }

    // MARK: - Contact Permission

    @Test
    fun `a contact access denied failure is recorded as a denial`() {
        val exception =
            Exception(
                "Contact access denied.",
                userInfo = mapOf(Exception.UserInfo.STATIC_ERROR_CODE.rawValue to AppException.contactAccessDenied.errorCode),
                metadata = ExceptionMetadata(this),
            )

        val result =
            reducer.reduce(
                PermissionPageReducer.State(),
                PermissionPageReducer.Action.RequestContactPermissionFailed(exception),
            )

        assertEquals(false, result.state.isContactPermissionGranted)
        assertTrue(result.state.isBackButtonEnabled)
        assertNotNull(result.effect)
    }

    @Test
    fun `another contact failure leaves the status undetermined`() {
        val exception = Exception("Contacts unavailable.", metadata = ExceptionMetadata(this))

        val result =
            reducer.reduce(
                PermissionPageReducer.State(isFinishButtonEnabled = true),
                PermissionPageReducer.Action.RequestContactPermissionFailed(exception),
            )

        assertNull(result.state.isContactPermissionGranted)
        assertTrue(result.state.isBackButtonEnabled)
        assertFalse(result.state.isFinishButtonEnabled)
    }

    @Test
    fun `a granted contact permission is recorded`() {
        val result =
            reducer.reduce(
                PermissionPageReducer.State(),
                PermissionPageReducer.Action.RequestContactPermissionReturned(PermissionService.PermissionStatus.GRANTED),
            )

        assertEquals(true, result.state.isContactPermissionGranted)
    }

    @Test
    fun `a denied notification permission is recorded`() {
        val result =
            reducer.reduce(
                PermissionPageReducer.State(),
                PermissionPageReducer.Action.RequestNotificationPermissionReturned(PermissionService.PermissionStatus.DENIED),
            )

        assertEquals(false, result.state.isNotificationPermissionGranted)
        assertNotNull(result.effect)
    }

    // MARK: - Finish Button

    @Test
    fun `tapping a permission button enables finish once the other permission is determined`() {
        val undetermined =
            reducer.reduce(
                PermissionPageReducer.State(),
                PermissionPageReducer.Action.ContactPermissionCapsuleButtonTapped,
            )
        val determined =
            reducer.reduce(
                PermissionPageReducer.State(isNotificationPermissionGranted = false),
                PermissionPageReducer.Action.ContactPermissionCapsuleButtonTapped,
            )

        assertFalse(undetermined.state.isFinishButtonEnabled)
        assertTrue(determined.state.isFinishButtonEnabled)
    }

    @Test
    fun `finish disables the buttons and presents a non modal dimmed overlay`() {
        val result =
            reducer.reduce(
                PermissionPageReducer.State(isFinishButtonEnabled = true),
                PermissionPageReducer.Action.FinishButtonTapped,
            )

        assertFalse(result.state.isBackButtonEnabled)
        assertFalse(result.state.isFinishButtonEnabled)
        assertTrue(Overlay.isVisible.value)
        assertFalse(Overlay.isModal.value)
        assertEquals(0.5f, Overlay.alpha.value)
    }

    @Test
    fun `declining the conduct policy restores the buttons`() {
        Overlay.addOverlay(alpha = 0.5f, isModal = false)

        val result =
            reducer.reduce(
                PermissionPageReducer.State(isBackButtonEnabled = false, isFinishButtonEnabled = false),
                PermissionPageReducer.Action.EulaAlertDismissed(cancelled = true),
            )

        assertTrue(result.state.isBackButtonEnabled)
        assertTrue(result.state.isFinishButtonEnabled)
        assertFalse(Overlay.isVisible.value)
    }
}
