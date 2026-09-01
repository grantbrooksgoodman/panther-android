//
//  WelcomePageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.welcomepageview

import kotlinx.coroutines.delay
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.designsystem.modules.theming.models.Themes
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * The reducer for the onboarding welcome page.
 *
 * On first appearance it resets the theme and resolves its label
 * strings; on each appearance it restores the device language, clears
 * any prior onboarding values, and signs in anonymously so the
 * translation archive can be read before the user authenticates.
 *
 * **Note:** the iOS welcome page cycles its greeting through many
 * languages; that decorative animation is omitted here.
 */
class WelcomePageReducer : Reducer<WelcomePageReducer.State, WelcomePageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object ViewFirstAppeared : Action

        data object ContinueButtonTapped : Action

        data object SignInButtonTapped : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action
    }

    // MARK: - State

    data class State(
        val strings: List<TranslationOutputMap> = WelcomePageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
        val welcomeLabelText: String = LocalizedStringKey.WelcomeToHello.localized(),
    )

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                OnboardingService.flushValues()
                ReduceResult(
                    state,
                    Effect.run {
                        delay(ANONYMOUS_SIGN_IN_DELAY_MILLIS)
                        try {
                            Networking.config.authDelegate.signInAnonymously()
                        } catch (exception: Exception) {
                            Logger.log(exception, with = AlertType.toastInPrerelease)
                        }
                    },
                )
            }

            Action.ViewFirstAppeared -> {
                ThemeService.setTheme(Themes.appDefault)
                ThemeService.setStyleOverride(null)
                ReduceResult(state.copy(viewState = ViewState.Loading), resolveEffect())
            }

            Action.ContinueButtonTapped -> {
                navigate(OnboardingNavigatorState.SeguePath.SelectLanguage)
                ReduceResult(state)
            }

            Action.SignInButtonTapped -> {
                navigate(OnboardingNavigatorState.SeguePath.SignIn)
                ReduceResult(state)
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }
        }

    // MARK: - Auxiliary

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(WelcomePageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    private fun navigate(path: OnboardingNavigatorState.SeguePath) {
        DependencyValues.current.navigation.navigate(Route.Onboarding(OnboardingRoute.Push(path)))
    }

    // MARK: - Companion

    private companion object {
        const val ANONYMOUS_SIGN_IN_DELAY_MILLIS = 1_000L
    }
}

/** The translated label strings for the welcome page. */
object WelcomePageViewStrings : TranslatedLabelStrings {
    val continueButtonText = TranslatedLabelStringCollection("welcomePageView.continueButtonText")
    val signInButtonText = TranslatedLabelStringCollection("welcomePageView.signInButtonText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(continueButtonText, TranslationInput("Get Started", alternate = "Create an Account")),
            TranslationInputMap(signInButtonText, TranslationInput("Sign In")),
        )
}
