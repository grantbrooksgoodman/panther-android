//
//  SignInPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.signinpageview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.modules.common.services.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

/**
 * Exercises the sign in page reducer: the back button's two
 * configurations, verification code gating, and the segue after
 * authentication.
 */
class SignInPageReducerTest {
    // MARK: - Setup

    private val reducer = SignInPageReducer()

    private val onboardingStack: List<OnboardingNavigatorState.SeguePath>
        get() =
            DependencyValues.current.navigation.state.value.onboarding.stack

    @Before
    fun setUp() {
        CommonPropertyLists.initializeForTesting(
            callingCodes = mapOf("US" to "1"),
            lookupTables = mapOf("10" to listOf("1")),
        )

        CoreUtilities.setLanguageCode("en")
        RegionDetailService.clearCache()
        DependencyValues.current.navigation.navigate(Route.Onboarding(OnboardingRoute.Stack(emptyList())))
    }

    @After
    fun tearDown() {
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
        RuntimeStorage.remove(StoredItemKey.languageCode)
        Overlay.removeOverlay(animated = false)
        DependencyValues.current.navigation.navigate(Route.Onboarding(OnboardingRoute.Stack(emptyList())))
        DependencyValues.current.onboardingService.flushValues()
    }

    // MARK: - Back Button

    @Test
    fun `back during verification code entry returns to the phone number field`() {
        val state =
            SignInPageReducer.State(
                configuration = SignInPageReducer.State.Configuration.VERIFICATION_CODE,
                phoneNumberString = "(555) 888-5555",
                selectedRegionCode = "US",
                verificationCode = "123",
            )

        val result = reducer.reduce(state, SignInPageReducer.Action.BackButtonTapped)

        assertEquals(SignInPageReducer.State.Configuration.PHONE_NUMBER, result.state.configuration)
        assertTrue(result.state.isContinueButtonEnabled)
        assertTrue(onboardingStack.isEmpty())
    }

    @Test
    fun `back during phone number entry pops the page`() {
        DependencyValues.current.navigation.navigate(
            Route.Onboarding(OnboardingRoute.Stack(listOf(OnboardingNavigatorState.SeguePath.SignIn))),
        )

        val result =
            reducer.reduce(
                SignInPageReducer.State(configuration = SignInPageReducer.State.Configuration.PHONE_NUMBER),
                SignInPageReducer.Action.BackButtonTapped,
            )

        assertEquals(SignInPageReducer.State.Configuration.PHONE_NUMBER, result.state.configuration)
        assertTrue(onboardingStack.isEmpty())
    }

    // MARK: - Verification Code

    @Test
    fun `the continue button requires a six digit verification code`() {
        val state = SignInPageReducer.State(configuration = SignInPageReducer.State.Configuration.VERIFICATION_CODE)

        val short = reducer.reduce(state, SignInPageReducer.Action.VerificationCodeChanged("12345"))
        val complete = reducer.reduce(state, SignInPageReducer.Action.VerificationCodeChanged("123456"))

        assertFalse(short.state.isContinueButtonEnabled)
        assertTrue(complete.state.isContinueButtonEnabled)
    }

    @Test
    fun `a sent verification code switches to code entry and records the auth identifier`() {
        val result =
            reducer.reduce(
                SignInPageReducer.State(),
                SignInPageReducer.Action.VerifyPhoneNumberReturned("auth-id"),
            )

        assertEquals(SignInPageReducer.State.Configuration.VERIFICATION_CODE, result.state.configuration)
        assertEquals("auth-id", result.state.authID)
        assertFalse(Overlay.isVisible.value)
    }

    // MARK: - Authentication

    @Test
    fun `authentication presents the splash page without clearing the onboarding stack`() {
        DependencyValues.current.navigation.navigate(
            Route.Onboarding(OnboardingRoute.Stack(listOf(OnboardingNavigatorState.SeguePath.SignIn))),
        )
        DependencyValues.current.navigation.navigate(Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Onboarding)))

        reducer.reduce(SignInPageReducer.State(), SignInPageReducer.Action.AuthenticateUserReturned("user-id"))

        val navigatorState = DependencyValues.current.navigation.state.value
        assertEquals(RootNavigatorState.ModalPath.Splash, navigatorState.modal)
        assertEquals(listOf(OnboardingNavigatorState.SeguePath.SignIn), navigatorState.onboarding.stack)
        assertFalse(Overlay.isVisible.value)
    }
}
