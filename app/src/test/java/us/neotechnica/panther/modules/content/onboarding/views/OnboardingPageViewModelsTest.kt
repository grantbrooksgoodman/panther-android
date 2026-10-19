//
//  OnboardingPageViewModelsTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.navigators.OnboardingNavigatorState

/**
 * Exercises the onboarding view-model holder: a page keeps its view
 * model beneath a push and across a pop, and its first appearance runs
 * once per flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingPageViewModelsTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `a page keeps its view model beneath a push and does not repeat its first appearance`() {
        val viewModels = OnboardingPageViewModels()
        val selectLanguage = OnboardingNavigatorState.SeguePaths.SelectLanguage
        var firstAppearances = 0

        val pushed = viewModels.selectLanguage
        viewModels.onFirstAppear(selectLanguage) { firstAppearances += 1 }

        // Verify Number pushes over Select Language, then pops back.
        viewModels.verifyNumber
        viewModels.onFirstAppear(selectLanguage) { firstAppearances += 1 }

        assertSame(pushed, viewModels.selectLanguage)
        assertEquals(1, firstAppearances)
        viewModels.closeAll()
    }

    @Test
    fun `a popped page keeps its view model for a re-push and appears no second time`() {
        val viewModels = OnboardingPageViewModels()
        val selectLanguage = OnboardingNavigatorState.SeguePaths.SelectLanguage
        var firstAppearances = 0

        val first = viewModels.selectLanguage
        viewModels.onFirstAppear(selectLanguage) { firstAppearances += 1 }

        val second = viewModels.selectLanguage
        viewModels.onFirstAppear(selectLanguage) { firstAppearances += 1 }

        assertSame(first, second)
        assertEquals(1, firstAppearances)
        viewModels.closeAll()
    }

    @Test
    fun `the welcome page persists across pushes and first-appears once`() {
        val viewModels = OnboardingPageViewModels()
        var firstAppearances = 0

        val welcome = viewModels.welcome
        viewModels.onFirstAppear(null) { firstAppearances += 1 }
        viewModels.selectLanguage
        viewModels.onFirstAppear(null) { firstAppearances += 1 }

        assertSame(welcome, viewModels.welcome)
        assertEquals(1, firstAppearances)
        viewModels.closeAll()
    }
}
