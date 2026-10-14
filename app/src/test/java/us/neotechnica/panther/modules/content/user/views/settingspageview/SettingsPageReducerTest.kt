//
//  SettingsPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.dependencies.SettingsPageViewServiceDependency
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.content.user.models.BuildInfoButtonStrings
import us.neotechnica.panther.modules.content.user.services.SettingsPageViewService
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyScopes
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

/**
 * Exercises the settings page reducer: the build-info cycling and its
 * copyright count, the contact detail fallbacks, the trait-collection
 * rebuild gate, the deferred trait-collection notification, and the
 * developer rows resolved on appearance.
 */
class SettingsPageReducerTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val me = SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001")
    private val reducer = SettingsPageReducer()

    private var milestone = Build.Milestone.GENERAL_RELEASE
    private var isDeveloperModeEnabled = false
    private var isMainPagePresented = true

    @Before
    fun setUp() {
        environment.install("settings-page-reducer-test")
        environment.signIn(me)
    }

    @After
    fun tearDown() {
        isMainPagePresented = true
    }

    private fun service(): SettingsPageViewService =
        SettingsPageViewService(
            SettingsPageViewService.Dependencies(
                currentUser = { me },
                isDeveloperModeEnabled = { isDeveloperModeEnabled },
                milestone = { milestone },
            ),
        ).also { it.isMainPagePresented = isMainPagePresented }

    private fun <T> withService(operation: () -> T): T =
        DependencyScopes.withDependencies({ it[SettingsPageViewServiceDependency] = service() }, operation)

    private fun reduce(
        state: SettingsPageReducer.State,
        action: SettingsPageReducer.Action,
    ) = withService { reducer.reduce(state, action) }

    // MARK: - Build Info Button

    @Test
    fun `tapping the build info button advances the strings and counts copyright visits`() {
        var state = SettingsPageReducer.State()
        var encounters = 0

        repeat(BuildInfoButtonStrings.BuildInfoButtonStringKey.entries.size) {
            state = reduce(state, SettingsPageReducer.Action.BuildInfoButtonTapped).state
            if (state.buildInfoButtonStrings.key == BuildInfoButtonStrings.BuildInfoButtonStringKey.COPYRIGHT) encounters += 1
        }

        assertEquals(1, encounters)
        assertEquals(1, state.timesEncounteredCopyrightText)
        assertEquals(BuildInfoButtonStrings.BuildInfoButtonStringKey.BUNDLE_VERSION_AND_BUILD_NUMBER, state.buildInfoButtonStrings.key)
    }

    // MARK: - Contact Detail

    @Test
    fun `a resolved contact fills the title, blanks a subtitle matching the title, and keeps the pair`() {
        val contactPair = ContactPair.withUser(me)

        val result = reduce(SettingsPageReducer.State(), SettingsPageReducer.Action.FetchCNContactForCurrentUserReturned(contactPair))

        assertEquals(contactPair.contact.fullName, result.state.contactDetailViewTitleLabelText)
        assertEquals("", result.state.contactDetailViewSubtitleLabelText)
        assertNotNull(result.state.contactPair)
        assertNull(result.state.contactDetailViewImage)
    }

    @Test
    fun `a named contact shows the phone number as its subtitle`() {
        val contactPair = ContactPair.withUser(me, name = "Grant")

        val result = reduce(SettingsPageReducer.State(), SettingsPageReducer.Action.FetchCNContactForCurrentUserReturned(contactPair))

        assertEquals("Grant", result.state.contactDetailViewTitleLabelText)
        assertEquals(me.phoneNumber.formattedString(), result.state.contactDetailViewSubtitleLabelText)
    }

    @Test
    fun `a failed contact fetch falls back to the current user's phone number`() {
        val exception = Exception("No contact.", metadata = ExceptionMetadata(this))

        val result = reduce(SettingsPageReducer.State(), SettingsPageReducer.Action.FetchCNContactForCurrentUserFailed(exception))

        assertEquals(me.phoneNumber.formattedString(), result.state.contactDetailViewTitleLabelText)
        assertNull(result.state.contactPair)
    }

    // MARK: - Resolution

    @Test
    fun `resolved strings load the page and a failure still loads it`() {
        val strings = listOf<TranslationOutputMap>()

        val returned = reduce(SettingsPageReducer.State(), SettingsPageReducer.Action.ResolveReturned(strings))
        val failed =
            reduce(
                SettingsPageReducer.State(),
                SettingsPageReducer.Action.ResolveFailed(Exception("Failed.", metadata = ExceptionMetadata(this))),
            )

        assertEquals(ViewState.Loaded, returned.state.viewState)
        assertEquals(strings, returned.state.strings)
        assertEquals(ViewState.Loaded, failed.state.viewState)
    }

    // MARK: - Trait Collection

    @Test
    fun `a trait collection change rebuilds the grouped lists only while the main page is presented`() {
        val state = SettingsPageReducer.State()

        val presented = reduce(state, SettingsPageReducer.Action.TraitCollectionChanged).state
        isMainPagePresented = false
        val pushed = reduce(state, SettingsPageReducer.Action.TraitCollectionChanged).state

        assertTrue(presented.traitCollectionChanged)
        assertNotEquals(state.viewID, presented.viewID)
        assertNotEquals(state.groupedListViewsID, presented.groupedListViewsID)
        assertTrue(pushed.traitCollectionChanged)
        assertNotEquals(state.viewID, pushed.viewID)
        assertEquals(state.groupedListViewsID, pushed.groupedListViewsID)
    }

    @Test
    fun `disappearing re-sends a trait collection change only when one was received`() =
        runBlocking {
            val received = CompletableDeferred<Unit>()
            val collector =
                launch(Dispatchers.Unconfined) {
                    DependencyValues.current.sharedEvents.traitCollectionChanged.events
                        .first()
                    received.complete(Unit)
                }

            reduce(SettingsPageReducer.State(traitCollectionChanged = false), SettingsPageReducer.Action.ViewDisappeared)
            assertNull(withTimeoutOrNull(200) { received.await() })

            reduce(SettingsPageReducer.State(traitCollectionChanged = true), SettingsPageReducer.Action.ViewDisappeared)
            withTimeout(1_000) { received.await() }
            collector.cancel()
        }

    // MARK: - Appearance

    @Test
    fun `appearing resolves the developer rows from the service and enters the loading state`() {
        milestone = Build.Milestone.GENERAL_RELEASE
        val release = reduce(SettingsPageReducer.State(viewState = ViewState.Loaded), SettingsPageReducer.Action.ViewAppeared).state

        milestone = Build.Milestone.BETA
        val beta = reduce(SettingsPageReducer.State(), SettingsPageReducer.Action.ViewAppeared).state

        assertNull(release.developerModeListItems)
        assertEquals(ViewState.Loading, release.viewState)
        assertEquals(listOf("Toggle Developer Mode"), beta.developerModeListItems?.map { it.innerText })
        assertFalse(beta.developerModeListItems.isNullOrEmpty())
    }
}
