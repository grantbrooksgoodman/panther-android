//
//  SelectLanguagePageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.services.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import java.util.Locale

/**
 * Exercises the select language page reducer: the localized language
 * dictionary that populates the wheel, the error state when none is
 * stored, and the cache clearing and language change on continue.
 */
class SelectLanguagePageReducerTest {
    // MARK: - Setup

    private val reducer = SelectLanguagePageReducer()

    private val deviceLanguageDictionary: Map<String, String>
        get() = checkNotNull(CoreUtilities.localizedLanguageCodeDictionary(Locale.getDefault().language))

    @Before
    fun setUp() {
        CommonPropertyLists.initializeForTesting(
            callingCodes = mapOf("FR" to "33", "US" to "1"),
            lookupTables = emptyMap(),
        )

        RuntimeStorage.languageCodeDictionary = mapOf("en" to "English", "es" to "Spanish")
        CoreUtilities.setLanguageCode("en")
        RegionDetailService.clearCache()
        DependencyValues.current.navigation.navigate(Route.Onboarding(OnboardingRoute.Stack(emptyList())))
    }

    @After
    fun tearDown() {
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
        RuntimeStorage.remove(StoredItemKey.languageCode)
        RuntimeStorage.languageCodeDictionary = null
        DependencyValues.current.navigation.navigate(Route.Onboarding(OnboardingRoute.Stack(emptyList())))
        DependencyValues.current.onboardingService.flushValues()
    }

    // MARK: - View Appeared

    @Test
    fun `view appeared lists the localized language names and preselects the current language`() {
        val dictionary = checkNotNull(CoreUtilities.localizedLanguageCodeDictionary("en"))

        val result = reducer.reduce(SelectLanguagePageReducer.State(), SelectLanguagePageReducer.Action.ViewAppeared)

        assertEquals(dictionary.values.sorted(), result.state.languages)
        assertEquals(dictionary.getValue("en"), result.state.selectedLanguageName)
        assertEquals(ViewState.Loading, result.state.viewState)
    }

    @Test
    fun `view appeared enters the error state without a language code dictionary`() {
        RuntimeStorage.languageCodeDictionary = null

        val result = reducer.reduce(SelectLanguagePageReducer.State(), SelectLanguagePageReducer.Action.ViewAppeared)

        val viewState = result.state.viewState
        assertTrue(viewState is ViewState.Error)
        assertEquals("No localized language code dictionary.", (viewState as ViewState.Error).exception.descriptor)
    }

    // MARK: - Selection

    @Test
    fun `the selected language code resolves against the device language dictionary`() {
        val dictionary = deviceLanguageDictionary

        val state = SelectLanguagePageReducer.State(selectedLanguageName = dictionary.getValue("es"))

        assertEquals("es", state.selectedLanguageCode)
    }

    @Test
    fun `an unknown selection falls back to the current language code`() {
        val state = SelectLanguagePageReducer.State(selectedLanguageName = "Klingon")

        assertEquals("en", state.selectedLanguageCode)
    }

    // MARK: - Continue

    @Test
    fun `continue applies the selected language and pushes the verify number page`() {
        val dictionary = deviceLanguageDictionary
        val state = SelectLanguagePageReducer.State(selectedLanguageName = dictionary.getValue("es"))

        reducer.reduce(state, SelectLanguagePageReducer.Action.ContinueButtonTapped)

        assertEquals("es", RuntimeStorage.languageCode)
        assertEquals("es", DependencyValues.current.onboardingService.languageCode)
        assertEquals(
            listOf(OnboardingNavigatorState.SeguePath.VerifyNumber),
            DependencyValues.current.navigation.state.value.onboarding.stack,
        )
    }

    @Test
    fun `continue clears the region detail cache so titles relocalize`() {
        val englishTitles = RegionDetailService.regionTitles(RegionDetailService.QueryStrategy.RegionCode("FR"))
        assertEquals(listOf("+33 (France)"), englishTitles)

        val dictionary = deviceLanguageDictionary
        reducer.reduce(
            SelectLanguagePageReducer.State(selectedLanguageName = dictionary.getValue("es")),
            SelectLanguagePageReducer.Action.ContinueButtonTapped,
        )

        assertEquals(listOf("+33 (Francia)"), RegionDetailService.regionTitles(RegionDetailService.QueryStrategy.RegionCode("FR")))
    }
}
