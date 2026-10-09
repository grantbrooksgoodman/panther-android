//
//  ForcedUpdateModalPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.forcedupdatemodalpageview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.designsystem.modules.foundation.views.root.RootWindowStatus
import us.neotechnica.panther.subsystem.modules.effect.Send
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ForcedUpdateModalPageReducerTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()
    private val reducer = ForcedUpdateModalPageReducer()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        Persistent.initializeForTesting()
        AlertPresenter.dismiss()
        CoreUtilities.setLanguageCode("en")
        initializeBuild(finalName = "Hello", codeName = "Panther")
    }

    @After
    fun tearDown() {
        AlertPresenter.dismiss()
        RootWindowStatus.setRootOverlayWindowAlpha(1f)
        RuntimeStorage.remove(StoredItemKey.languageCode)
        Dispatchers.resetMain()
    }

    // MARK: - Strings

    @Test
    fun `the title depends on the app language`() {
        assertEquals("Update Required", ForcedUpdateModalPageViewStringKey.TITLE_LABEL_TEXT.rawValue)

        CoreUtilities.setLanguageCode("fr")
        assertEquals("An Update is Required", ForcedUpdateModalPageViewStringKey.TITLE_LABEL_TEXT.rawValue)
    }

    @Test
    fun `the subtitle falls back from the final name to the code name to the app`() {
        assertEquals(
            "This version of ⌘Hello⌘ is no longer supported. To continue, please download and install the most recent update.",
            ForcedUpdateModalPageViewStringKey.SUBTITLE_LABEL_TEXT.rawValue,
        )

        initializeBuild(finalName = "", codeName = "Panther")
        assertEquals(
            "This version of ⌘Panther⌘ is no longer supported. To continue, please download and install the most recent update.",
            ForcedUpdateModalPageViewStringKey.SUBTITLE_LABEL_TEXT.rawValue,
        )

        initializeBuild(finalName = "", codeName = "")
        assertEquals(
            "This version of the app is no longer supported. To continue, please download and install the most recent update.",
            ForcedUpdateModalPageViewStringKey.SUBTITLE_LABEL_TEXT.rawValue,
        )
    }

    // MARK: - Reduce

    @Test
    fun `view appeared formats the version label and hides the install button without a redirect`() {
        val result = reducer.reduce(ForcedUpdateModalPageReducer.State(), ForcedUpdateModalPageReducer.Action.ViewAppeared)

        assertEquals("v1.0.0 ($BUILD_NUMBER" + "b/${Build.bundleRevision.lowercase()})", result.state.versionLabelText)
        assertFalse(result.state.shouldShowInstallButton)
    }

    @Test
    fun `resolution results load the page`() {
        val strings = mapOf(ForcedUpdateModalPageViewStringKey.INSTALL_BUTTON_TEXT to "Instalar ahora")

        val returned =
            reducer.reduce(
                ForcedUpdateModalPageReducer.State(),
                ForcedUpdateModalPageReducer.Action.ResolveReturned(strings),
            )

        assertEquals(strings, returned.state.strings)
        assertEquals(ViewState.Loaded, returned.state.viewState)

        val failed =
            reducer.reduce(
                ForcedUpdateModalPageReducer.State(),
                ForcedUpdateModalPageReducer.Action.ResolveFailed(Exception(metadata = ExceptionMetadata(this))),
            )

        assertEquals(ForcedUpdateModalPageViewStrings.defaultOutputMap, failed.state.strings)
        assertEquals(ViewState.Loaded, failed.state.viewState)
    }

    @Test
    fun `interactive content stays hidden until developer mode is enabled`() =
        runTest(dispatcher) {
            Persistent.setBoolean(PersistentStorageKey.isDeveloperModeEnabled, false)

            presentAlert()
            assertNotNull(AlertPresenter.current.value)

            val result = reducer.reduce(ForcedUpdateModalPageReducer.State(), ForcedUpdateModalPageReducer.Action.ViewAppeared)
            val job = launch { result.effect.operation(Send {}) }

            advanceTimeBy(HIDE_INTERVAL_MILLISECONDS / 2)
            assertNull(AlertPresenter.current.value)
            assertEquals(0f, RootWindowStatus.rootOverlayWindowAlpha.value)

            presentAlert()
            assertNotNull(AlertPresenter.current.value)

            advanceTimeBy(HIDE_INTERVAL_MILLISECONDS * 2)
            assertNull(AlertPresenter.current.value)

            job.cancel()
        }

    // MARK: - Auxiliary

    private fun initializeBuild(
        finalName: String,
        codeName: String,
    ) {
        Build.initialize(
            appStoreBuildNumber = 0,
            buildNumber = BUILD_NUMBER,
            codeName = codeName,
            finalName = finalName,
            bundleVersion = "1.0.0",
            environment = "development",
            milestone = Build.Milestone.BETA,
            buildDate = Date(0),
            firstCompileDate = Date(0),
        )
    }

    private fun TestScope.presentAlert() {
        AlertPresenter.present(
            PresentedAlert.Standard(title = null, message = "Message", actions = listOf(Action("OK") {})) {},
        )
        advanceTimeBy(ALERT_PRESENTATION_DELAY_MILLISECONDS)
    }

    // MARK: - Companion

    private companion object {
        const val ALERT_PRESENTATION_DELAY_MILLISECONDS = 20L
        const val BUILD_NUMBER = 39_500
        const val HIDE_INTERVAL_MILLISECONDS = 100L
    }
}
