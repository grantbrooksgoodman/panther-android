//
//  PermissionServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.modules.common.services.sources.PermissionService.PermissionStatus

class PermissionServiceTest {
    // MARK: - Tests

    @Test
    fun `a granted permission is granted whatever was asked before`() {
        assertEquals(PermissionStatus.GRANTED, status(isGranted = true, hasRequested = false, canShowRationale = false))
        assertEquals(PermissionStatus.GRANTED, status(isGranted = true, hasRequested = true, canShowRationale = true))
    }

    @Test
    fun `a permission never requested is undetermined`() {
        assertEquals(PermissionStatus.UNKNOWN, status(isGranted = false, hasRequested = false, canShowRationale = false))
    }

    @Test
    fun `a refused permission the system still prompts for stays askable`() {
        assertEquals(PermissionStatus.UNKNOWN, status(isGranted = false, hasRequested = true, canShowRationale = true))
    }

    @Test
    fun `a refused permission the system no longer prompts for is denied`() {
        assertEquals(PermissionStatus.DENIED, status(isGranted = false, hasRequested = true, canShowRationale = false))
    }

    @Test
    fun `android 9 needs the storage permission to save media`() {
        assertTrue(PermissionService.requiresStoragePermission(Build.VERSION_CODES.P))
    }

    @Test
    fun `android 10 and later save media without the storage permission`() {
        assertFalse(PermissionService.requiresStoragePermission(Build.VERSION_CODES.Q))
        assertFalse(PermissionService.requiresStoragePermission(Build.VERSION_CODES.VANILLA_ICE_CREAM))
    }

    // MARK: - Auxiliary

    private fun status(
        isGranted: Boolean,
        hasRequested: Boolean,
        canShowRationale: Boolean,
    ): PermissionStatus = PermissionService.permissionStatus(isGranted, hasRequested, canShowRationale)
}
