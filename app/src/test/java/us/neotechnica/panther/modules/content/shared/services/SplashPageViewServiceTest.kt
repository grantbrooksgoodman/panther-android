//
//  SplashPageViewServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import java.io.File
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

@OptIn(ExperimentalCoroutinesApi::class)
class SplashPageViewServiceTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        val directory = File(System.getProperty("java.io.tmpdir"), "splash-service-test-${System.nanoTime()}")
        directory.mkdirs()
        FileStore.initializeForTesting(directory)
        Persistent.initializeForTesting()
        SplashPageViewService.setInitializationProgress(0f)
    }

    @After
    fun tearDown() {
        SplashPageViewService.setInitializationProgress(0f)
        Task.resetScope()
    }

    // MARK: - Tests

    @Test
    fun `initialization progress resets to zero after completion`() =
        runTest {
            Task.setScope(this)

            SplashPageViewService.setInitializationProgress(1f)
            assertEquals(1f, SplashPageViewService.initializationProgress.value, EPSILON)

            advanceUntilIdle()

            assertEquals(0f, SplashPageViewService.initializationProgress.value, EPSILON)
        }

    @Test
    fun `initialization progress does not reset when a new load begins`() =
        runTest {
            Task.setScope(this)

            SplashPageViewService.setInitializationProgress(1f)
            advanceTimeBy(HALF_RESET_DELAY_MILLISECONDS)

            // A new initialization begins before the reset delay elapses.
            SplashPageViewService.setInitializationProgress(0.5f)
            advanceUntilIdle()

            assertEquals(0.5f, SplashPageViewService.initializationProgress.value, EPSILON)
        }

    // MARK: - Companion

    private companion object {
        const val EPSILON = 1e-6f
        const val HALF_RESET_DELAY_MILLISECONDS = 1_000L
    }
}
