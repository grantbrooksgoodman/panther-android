//
//  SettingsPageViewServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.foundation.components.ListRowView
import us.neotechnica.panther.modules.common.services.sources.AnalyticsService.AnalyticsEvent
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Exercises the settings page service: the developer rows across
 * milestones, developer mode, and the language override, the sign-out
 * sequence with and without a current user, and the contact fetch
 * failures – each through injected collaborators.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPageViewServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    private var currentUser: User? = SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001")
    private var isDeveloperModeEnabled = false
    private var milestone = Build.Milestone.BETA
    private var overriddenLanguageCode: String? = null
    private var removePushTokenFails = false
    private val delays = mutableListOf<Duration>()
    private val events = mutableListOf<AnalyticsEvent>()
    private val steps = mutableListOf<String>()

    private val service =
        SettingsPageViewService(
            SettingsPageViewService.Dependencies(
                currentUser = { currentUser },
                delayed = { by, _ -> delays.add(by) },
                dismissSheets = { steps.add("dismissSheets") },
                isDeveloperModeEnabled = { isDeveloperModeEnabled },
                logEvent = { events.add(it) },
                milestone = { milestone },
                navigate = { steps.add("navigate") },
                overriddenLanguageCode = { overriddenLanguageCode },
                promptToToggleDeveloperMode = { steps.add("promptToToggle") },
                removeCurrentPushToken = {
                    steps.add("removeCurrentPushToken")
                    if (removePushTokenFails) throw Exception("Failed.", metadata = ExceptionMetadata(this))
                },
                reset = { steps.add("reset") },
                setBadgeNumber = { steps.add("setBadgeNumber:$it") },
                stopObservingCurrentUserChanges = { steps.add("stopObserving") },
            ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("settings-page-view-service-test")
        RuntimeStorage.languageCodeDictionary = mapOf("en" to "English", "es" to "Spanish (Español)")
    }

    @After
    fun tearDown() {
        RuntimeStorage.languageCodeDictionary = null
        Dispatchers.resetMain()
    }

    private fun rowTitles(): List<String>? = service.developerModeListItems()?.map { it.innerText }

    // MARK: - Developer Mode List Items

    @Test
    fun `general-release builds have no developer rows`() {
        milestone = Build.Milestone.GENERAL_RELEASE
        assertNull(rowTitles())
    }

    @Test
    fun `prerelease builds offer the toggle until developer mode is enabled`() {
        assertEquals(listOf("Toggle Developer Mode"), rowTitles())

        isDeveloperModeEnabled = true
        assertEquals(emptyList<String>(), rowTitles())
    }

    @Test
    fun `developer mode offers the English override to non-English users and the restore once overridden`() {
        isDeveloperModeEnabled = true
        currentUser = SessionTestEnvironment.user(id = "me", languageCode = "es")

        assertEquals(listOf("Override Language Code to English"), rowTitles())

        overriddenLanguageCode = "en"
        assertEquals(listOf("Restore Language to Spanish"), rowTitles())
    }

    @Test
    fun `the toggle row prompts through the injected collaborator`() {
        val row = service.developerModeListItems()?.single()
        (row?.interaction as? ListRowView.Interaction.Button)?.action?.invoke()

        assertEquals(listOf("promptToToggle"), steps)
    }

    // MARK: - Sign Out

    @Test
    fun `signing out clears the badge, drops the push token, resets, logs, and defers the navigation`() =
        runBlocking {
            service.signOut()

            assertEquals(
                listOf("setBadgeNumber:0", "stopObserving", "removeCurrentPushToken", "dismissSheets", "reset"),
                steps,
            )
            assertEquals(listOf(AnalyticsEvent.LOG_OUT), events)
            assertEquals(listOf(500.milliseconds), delays)
        }

    @Test
    fun `signing out without a current user still resets and returns to onboarding`() =
        runBlocking {
            currentUser = null

            service.signOut()

            assertEquals(listOf("setBadgeNumber:0", "dismissSheets", "reset"), steps)
            assertEquals(listOf(AnalyticsEvent.LOG_OUT), events)
            assertEquals(1, delays.size)
        }

    @Test
    fun `a failed push token removal does not stop the sign-out`() =
        runBlocking {
            removePushTokenFails = true

            service.signOut()

            assertEquals(
                listOf("setBadgeNumber:0", "stopObserving", "removeCurrentPushToken", "dismissSheets", "reset"),
                steps,
            )
        }

    // MARK: - Fetch CNContact for Current User

    @Test
    fun `fetching the contact throws without a current user or a matching contact`() {
        currentUser = null
        val noUser = assertThrows(Exception::class.java) { service.fetchCNContactForCurrentUser() }

        currentUser = SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001")
        val noContact = assertThrows(Exception::class.java) { service.fetchCNContactForCurrentUser() }

        assertEquals("Current user has not been set.", noUser.descriptor)
        assertEquals("No matching contact could be resolved.", noContact.descriptor)
    }
}
