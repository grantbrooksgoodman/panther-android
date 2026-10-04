//
//  WelcomePageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.welcomepageview

import kotlinx.coroutines.delay
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.designsystem.modules.theming.models.Themes
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancel
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The reducer that drives the welcome page, the root of the onboarding
 * flow.
 *
 * This page is the user's entry point into the app. It offers two paths
 * forward: continuing to sign-up, which begins with language selection,
 * or signing in to an existing account.
 *
 * The page's behavior contract:
 *
 * - On first appearance, the page resolves its translated display
 *   strings, remaining in the loading state until resolution completes.
 *   If resolution fails, the page falls back to its default strings and
 *   loads anyway. The page also resets the app's theme to its default
 *   and begins cycling the welcome label.
 * - On every appearance, the page discards any values recorded by
 *   [OnboardingService] during a previous onboarding attempt, and signs
 *   the user in anonymously after a brief delay.
 * - The welcome label displays the welcome message in a randomly chosen
 *   supported language every few seconds, not repeating a language until
 *   all have been shown. Tapping the label resets it and restarts the
 *   cycling loop.
 * - Tapping continue pushes the language selection page; tapping sign in
 *   pushes the sign-in page.
 */
class WelcomePageReducer : Reducer<WelcomePageReducer.State, WelcomePageReducer.Action> {
    // MARK: - Types

    private enum class TaskID {
        CYCLE_WELCOME_LABEL_TEXT,
    }

    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object ViewFirstAppeared : Action

        data object ContinueButtonTapped : Action

        data object CycleWelcomeLabelText : Action

        data object SignInButtonTapped : Action

        data object WelcomeLabelTapped : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action
    }

    // MARK: - State

    data class State(
        val cycledLanguageCodes: Map<String, String> = emptyMap(),
        val strings: List<TranslationOutputMap> = WelcomePageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
        val welcomeLabelText: String = LocalizedStringKey.WelcomeToHello.localized(),
    ) {
        val supportedLanguageCodes: List<String>
            get() = LocalizedStringResolver.languageDisplayNames().keys.toList()
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                OnboardingService.flushValues()
                ReduceResult(
                    state.copy(
                        welcomeLabelText = LocalizedStringKey.WelcomeToHello.localized(languageCode = Locale.getDefault().language),
                    ),
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
                ReduceResult(
                    state.copy(viewState = ViewState.Loading),
                    Effect.merge(
                        resolveEffect(),
                        cycleWelcomeLabelTextEffect(delay = WELCOME_LABEL_CYCLE_INITIAL_DELAY_SECONDS.seconds),
                    ),
                )
            }

            Action.ContinueButtonTapped -> {
                navigate(OnboardingNavigatorState.SeguePath.SelectLanguage)
                ReduceResult(state)
            }

            Action.CycleWelcomeLabelText -> {
                val supportedLanguageCodes = state.supportedLanguageCodes
                val randomLanguageCode = supportedLanguageCodes.randomOrNull()
                when {
                    state.cycledLanguageCodes.size >= supportedLanguageCodes.size ->
                        ReduceResult(state.copy(cycledLanguageCodes = emptyMap()), cycleWelcomeLabelTextEffect())

                    randomLanguageCode == null ->
                        ReduceResult(state, cycleWelcomeLabelTextEffect())

                    else -> {
                        val localizedString = LocalizedStringKey.WelcomeToHello.localized(languageCode = randomLanguageCode)
                        if (state.cycledLanguageCodes.containsKey(randomLanguageCode) ||
                            state.cycledLanguageCodes.values.contains(localizedString) ||
                            state.welcomeLabelText == localizedString
                        ) {
                            ReduceResult(state, cycleWelcomeLabelTextEffect())
                        } else {
                            ReduceResult(
                                state.copy(
                                    cycledLanguageCodes = state.cycledLanguageCodes + (randomLanguageCode to localizedString),
                                    welcomeLabelText = localizedString,
                                ),
                                cycleWelcomeLabelTextEffect(delay = WELCOME_LABEL_CYCLE_DELAY_SECONDS.seconds),
                            )
                        }
                    }
                }
            }

            Action.SignInButtonTapped -> {
                navigate(OnboardingNavigatorState.SeguePath.SignIn)
                ReduceResult(state)
            }

            Action.WelcomeLabelTapped ->
                ReduceResult(
                    state.copy(welcomeLabelText = LocalizedStringKey.WelcomeToHello.localized()),
                    Effect.merge(
                        Effect.cancel(TaskID.CYCLE_WELCOME_LABEL_TEXT),
                        cycleWelcomeLabelTextEffect(delay = WELCOME_LABEL_CYCLE_INITIAL_DELAY_SECONDS.seconds),
                    ),
                )

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }
        }

    // MARK: - Auxiliary

    private fun cycleWelcomeLabelTextEffect(delay: Duration = Duration.ZERO): Effect<Action> =
        Effect
            .task<Action>(delay = delay) { Action.CycleWelcomeLabelText }
            .cancellable(TaskID.CYCLE_WELCOME_LABEL_TEXT)

    private fun navigate(path: OnboardingNavigatorState.SeguePath) {
        DependencyValues.current.navigation.navigate(Route.Onboarding(OnboardingRoute.Push(path)))
    }

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(WelcomePageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    // MARK: - Companion

    private companion object {
        const val ANONYMOUS_SIGN_IN_DELAY_MILLIS = 1_000L
        const val WELCOME_LABEL_CYCLE_DELAY_SECONDS = 3
        const val WELCOME_LABEL_CYCLE_INITIAL_DELAY_SECONDS = 5
    }
}
