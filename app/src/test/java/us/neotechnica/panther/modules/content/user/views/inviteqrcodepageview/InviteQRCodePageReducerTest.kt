//
//  InviteQRCodePageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.inviteqrcodepageview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.SettingsNavigatorState
import us.neotechnica.panther.navigation.SettingsRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * Exercises the invite QR code reducer: Done dismisses the settings
 * sheet, and resolution success or failure loads the page.
 */
class InviteQRCodePageReducerTest {
    // MARK: - Setup

    private val reducer = InviteQRCodePageReducer()

    @Before
    fun setUp() {
        DependencyValues.current.navigation.navigate(Route.Settings(SettingsRoute.Sheet(SettingsNavigatorState.SheetPaths.InviteQRCode)))
    }

    @After
    fun tearDown() {
        DependencyValues.current.navigation.navigate(Route.Settings(SettingsRoute.Sheet(null)))
    }

    // MARK: - Tests

    @Test
    fun `done dismisses the settings sheet`() {
        reducer.reduce(InviteQRCodePageReducer.State(), InviteQRCodePageReducer.Action.DoneButtonTapped)

        assertNull(DependencyValues.current.navigation.state.value.settings.sheet)
    }

    @Test
    fun `resolution success and failure both load the page`() {
        val returned =
            reducer.reduce(
                InviteQRCodePageReducer.State(),
                InviteQRCodePageReducer.Action.ResolveReturned(InviteQRCodePageViewStrings.defaultOutputMap),
            )
        val failed =
            reducer.reduce(
                InviteQRCodePageReducer.State(),
                InviteQRCodePageReducer.Action.ResolveFailed(Exception("Offline.", metadata = ExceptionMetadata(this))),
            )

        assertEquals(ViewState.Loaded, returned.state.viewState)
        assertEquals(ViewState.Loaded, failed.state.viewState)
    }
}
