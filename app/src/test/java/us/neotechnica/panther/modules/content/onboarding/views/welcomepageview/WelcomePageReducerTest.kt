//
//  WelcomePageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.welcomepageview

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.io.File
import java.util.Locale

/**
 * Exercises the welcome page reducer: device language restoration and
 * onboarding value flushing on appearance, welcome label cycling, and
 * the sign-in segue.
 */
class WelcomePageReducerTest {
    // MARK: - Setup

    private val reducer = WelcomePageReducer()

    @Before
    fun setUp() {
        LocalizedStringResolver.initializeForTesting { source ->
            val root = if (source == LocalizationSource.APP) "src/main/assets" else "../subsystem/src/main/assets"
            val json = Json.parseToJsonElement(File(root, source.assetName).readText()).jsonObject
            json.mapValues { (_, languages) ->
                languages.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            }
        }

        RuntimeStorage.languageCodeDictionary = mapOf("en" to "English", "es" to "Spanish")
        CoreUtilities.setLanguageCode("en")
        DependencyValues.current.navigation.navigate(Route.Onboarding(OnboardingRoute.Stack(emptyList())))
    }

    @After
    fun tearDown() {
        RuntimeStorage.remove(StoredItemKey.languageCode)
        RuntimeStorage.languageCodeDictionary = null
        LocalizedStringResolver.clearCache()
        DependencyValues.current.navigation.navigate(Route.Onboarding(OnboardingRoute.Stack(emptyList())))
    }

    // MARK: - View Appeared

    @Test
    fun `view appeared restores the device language and flushes onboarding values`() {
        CoreUtilities.setLanguageCode("fr")

        val onboardingService = DependencyValues.current.onboardingService
        onboardingService.setAuthID("auth")
        onboardingService.setLanguageCode("fr")

        val result = reducer.reduce(WelcomePageReducer.State(), WelcomePageReducer.Action.ViewAppeared)

        assertEquals(Locale.getDefault().language, RuntimeStorage.languageCode)
        assertNull(onboardingService.authID)
        assertNull(onboardingService.languageCode)
        assertEquals(
            LocalizedStringKey.WelcomeToHello.localized(languageCode = Locale.getDefault().language),
            result.state.welcomeLabelText,
        )
        assertNotNull(result.effect)
    }

    @Test
    fun `view first appeared keeps the page loading and schedules its effects`() {
        val result = reducer.reduce(WelcomePageReducer.State(), WelcomePageReducer.Action.ViewFirstAppeared)

        assertEquals(ViewState.Loading, result.state.viewState)
        assertNotNull(result.effect)
    }

    // MARK: - Welcome Label Cycling

    @Test
    fun `cycling records the chosen language and changes the label`() {
        val initialText = LocalizedStringKey.WelcomeToHello.localized(languageCode = "en")

        val result =
            reducer.reduce(
                WelcomePageReducer.State(welcomeLabelText = initialText, cycledLanguageCodes = mapOf("en" to initialText)),
                WelcomePageReducer.Action.CycleWelcomeLabelText,
            )

        // With English already cycled, only Spanish remains eligible;
        // either it is chosen, or the random draw repeats English and
        // the state is left untouched for the next attempt.
        val state = result.state
        if (state.cycledLanguageCodes.size == 2) {
            assertEquals(LocalizedStringKey.WelcomeToHello.localized(languageCode = "es"), state.welcomeLabelText)
        } else {
            assertEquals(initialText, state.welcomeLabelText)
        }
        assertNotNull(result.effect)
    }

    @Test
    fun `cycling resets once every supported language has been shown`() {
        val result =
            reducer.reduce(
                WelcomePageReducer.State(cycledLanguageCodes = mapOf("en" to "a", "es" to "b")),
                WelcomePageReducer.Action.CycleWelcomeLabelText,
            )

        assertTrue(result.state.cycledLanguageCodes.isEmpty())
    }

    @Test
    fun `tapping the label restores the current language text`() {
        val result =
            reducer.reduce(
                WelcomePageReducer.State(welcomeLabelText = "Bonjour"),
                WelcomePageReducer.Action.WelcomeLabelTapped,
            )

        assertEquals(LocalizedStringKey.WelcomeToHello.localized(), result.state.welcomeLabelText)
    }

    // MARK: - Navigation

    @Test
    fun `sign in pushes the sign in page`() {
        reducer.reduce(WelcomePageReducer.State(), WelcomePageReducer.Action.SignInButtonTapped)

        assertEquals(
            listOf(OnboardingNavigatorState.SeguePath.SignIn),
            DependencyValues.current.navigation.state.value.onboarding.stack,
        )
    }
}
