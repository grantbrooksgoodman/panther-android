//
//  SplashPageReducerTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.views.splashpageview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.content.shared.services.SplashPageViewService
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.navigation.navigation
import java.io.File

class SplashPageReducerTest {
    // MARK: - Setup

    private val reducer = SplashPageReducer()

    @Before
    fun setUp() {
        val directory = File(System.getProperty("java.io.tmpdir"), "splash-reducer-test-${System.nanoTime()}")
        directory.mkdirs()
        FileStore.initializeForTesting(directory)
        Persistent.initializeForTesting()
        SplashPageViewService.setInitializationProgress(0f)
    }

    // MARK: - Tests

    @Test
    fun `view appeared resets the automatic recovery flag`() {
        val result =
            reducer.reduce(
                SplashPageReducer.State(didAttemptAutomaticErrorRecovery = true),
                SplashPageReducer.Action.ViewAppeared,
            )

        assertFalse(result.state.didAttemptAutomaticErrorRecovery)
    }

    @Test
    fun `progress nudge advances while below the ceiling`() {
        SplashPageViewService.setInitializationProgress(0f)

        reducer.reduce(
            SplashPageReducer.State(),
            SplashPageReducer.Action.BundleInitializationProgressOccurred,
        )

        assertEquals(0.0005f, SplashPageViewService.initializationProgress.value, EPSILON)
    }

    @Test
    fun `progress nudge stops at the ceiling`() {
        SplashPageViewService.setInitializationProgress(0.8f)

        reducer.reduce(
            SplashPageReducer.State(),
            SplashPageReducer.Action.BundleInitializationProgressOccurred,
        )

        assertEquals(0.8f, SplashPageViewService.initializationProgress.value, EPSILON)
    }

    @Test
    fun `first initialization failure flags automatic recovery`() {
        val exception = Exception("Initialization failed.", metadata = ExceptionMetadata(this))

        val result =
            reducer.reduce(
                SplashPageReducer.State(),
                SplashPageReducer.Action.InitializedBundle(exception),
            )

        assertTrue(result.state.didAttemptAutomaticErrorRecovery)
        assertEquals(exception, result.state.exception)
    }

    @Test
    fun `subsequent initialization failure stores the exception`() {
        val exception = Exception("Initialization failed again.", metadata = ExceptionMetadata(this))

        val result =
            reducer.reduce(
                SplashPageReducer.State(didAttemptAutomaticErrorRecovery = true),
                SplashPageReducer.Action.InitializedBundle(exception),
            )

        assertTrue(result.state.didAttemptAutomaticErrorRecovery)
        assertEquals(exception, result.state.exception)
    }

    @Test
    fun `successful initialization with no signed-in user navigates to onboarding`() {
        reducer.reduce(
            SplashPageReducer.State(),
            SplashPageReducer.Action.InitializedBundle(null),
        )

        assertEquals(
            RootNavigatorState.ModalPath.Onboarding,
            DependencyValues.current.navigation.state.value.modal,
        )
    }

    // MARK: - Companion

    private companion object {
        const val EPSILON = 1e-6f
    }
}
