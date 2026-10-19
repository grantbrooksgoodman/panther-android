//
//  SplashPageViewServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import java.io.File

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
        SplashPageViewService.setDependencies(null)
        Task.resetScope()
    }

    // MARK: - Parallel Resolution

    @Test
    fun `the signed-out path cancels the language-code job and resolves no cache status`() =
        runTest {
            var languageCodeResolutions = 0
            SplashPageViewService.setDependencies(
                SplashPageViewService.Dependencies(
                    resolveAndSetLanguageCode = {
                        languageCodeResolutions += 1
                        awaitCancellation()
                    },
                    resolveCacheStatus = { null },
                    resolveCurrentUser = {},
                ),
            )

            val resolution = with(SplashPageViewService) { startParallelResolution(currentUserID = null) }
            advanceUntilIdle()

            assertTrue(resolution.languageCode.isCancelled)
            assertNull(resolution.cacheStatus.await().getOrNull())
            assertNull(resolution.currentUser.await())
            assertTrue(languageCodeResolutions <= 1)
        }

    @Test
    fun `the signed-in path awaits the language code`() =
        runTest {
            var languageCodeResolved = false
            SplashPageViewService.setDependencies(
                SplashPageViewService.Dependencies(
                    resolveAndSetLanguageCode = { languageCodeResolved = true },
                    resolveCacheStatus = { null },
                    resolveCurrentUser = {},
                ),
            )

            val resolution = with(SplashPageViewService) { startParallelResolution(currentUserID = "me") }
            advanceUntilIdle()

            assertFalse(resolution.languageCode.isCancelled)
            assertNull(resolution.languageCode.await())
            assertTrue(languageCodeResolved)
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
