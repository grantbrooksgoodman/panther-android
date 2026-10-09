//
//  BuildInfoOverlayReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.buildinfooverlayview

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.designsystem.modules.foundation.extensions.registerBuildInfoOverlayDotIndicatorColorDelegate
import us.neotechnica.panther.designsystem.modules.foundation.interfaces.BuildInfoOverlayDotIndicatorColorDelegate
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.effect.Send
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import java.util.Date
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class BuildInfoOverlayReducerTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()
    private val reducer = BuildInfoOverlayReducer()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        Persistent.initializeForTesting()
        AlertPresenter.dismiss()
        Build.initialize(
            appStoreBuildNumber = 0,
            buildNumber = BUILD_NUMBER,
            codeName = "Panther",
            finalName = "Hello",
            bundleVersion = "1.0.0",
            environment = "development",
            milestone = Build.Milestone.BETA,
            buildDate = Date(0),
            firstCompileDate = Date(0),
        )
    }

    @After
    fun tearDown() {
        AlertPresenter.dismiss()
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `view appeared formats the build information label`() {
        val result = reducer.reduce(BuildInfoOverlayReducer.State(), BuildInfoOverlayReducer.Action.ViewAppeared)

        assertEquals(
            "Panther 1.0.0 (${BUILD_NUMBER}b/${Build.bundleRevision.lowercase()})",
            result.state.buildInfoButtonText,
        )
    }

    @Test
    fun `the stats label reads Calculating until measured`() {
        assertEquals("Calculating...", BuildInfoOverlayReducer.State().statsLabelText)
    }

    @Test
    fun `a root tap turns translucent and restores opacity after five seconds`() =
        runTest(dispatcher) {
            val result = reducer.reduce(BuildInfoOverlayReducer.State(), BuildInfoOverlayReducer.Action.RootViewTapped)
            assertTrue(result.state.shouldUseTranslucentAppearance)
            assertEquals(Color.Black.copy(alpha = TRANSLUCENT_ALPHA), result.state.backgroundColor)

            val sent = mutableListOf<BuildInfoOverlayReducer.Action>()
            launch { result.effect.operation(Send { sent.add(it) }) }

            advanceTimeBy(4.seconds)
            runCurrent()
            assertTrue(sent.isEmpty())

            advanceTimeBy(2.seconds)
            runCurrent()
            assertEquals(listOf(BuildInfoOverlayReducer.Action.ShouldUseTranslucentAppearanceChanged(false)), sent)

            val restored = reducer.reduce(result.state, sent.single())
            assertFalse(restored.state.shouldUseTranslucentAppearance)
            assertEquals(Color.Black, restored.state.backgroundColor)
        }

    @Test
    fun `the indicator dot follows developer mode and the registered color`() {
        Persistent.setBoolean(PersistentStorageKey.isDeveloperModeEnabled, false)
        assertFalse(BuildInfoOverlayReducer.State().isDeveloperModeEnabled)

        Persistent.setBoolean(PersistentStorageKey.isDeveloperModeEnabled, true)
        assertTrue(BuildInfoOverlayReducer.State().isDeveloperModeEnabled)

        AppSubsystem.delegates.registerBuildInfoOverlayDotIndicatorColorDelegate(
            object : BuildInfoOverlayDotIndicatorColorDelegate {
                override val developerModeIndicatorDotColor: Color = Color.Magenta
            },
        )

        val result =
            reducer.reduce(
                BuildInfoOverlayReducer.State(developerModeIndicatorDotColor = Color.Red),
                BuildInfoOverlayReducer.Action.RestoreIndicatorColor,
            )

        assertEquals(Color.Magenta, result.state.developerModeIndicatorDotColor)
    }

    @Test
    fun `user interaction is disabled while an alert is presented`() =
        runTest(dispatcher) {
            assertFalse(BuildInfoOverlayReducer.State().isUserInteractionDisabled)

            AlertPresenter.present(
                PresentedAlert.Standard(title = null, message = "Message", actions = listOf(Action("OK") {})) {},
            )
            advanceTimeBy(ALERT_PRESENTATION_DELAY_MILLISECONDS)

            assertTrue(BuildInfoOverlayReducer.State().isUserInteractionDisabled)
        }

    // MARK: - Companion

    private companion object {
        const val ALERT_PRESENTATION_DELAY_MILLISECONDS = 200L
        const val BUILD_NUMBER = 39_500
        const val TRANSLUCENT_ALPHA = 0.35f
    }
}
