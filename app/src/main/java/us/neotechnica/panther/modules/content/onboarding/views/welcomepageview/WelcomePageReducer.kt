//
//  WelcomePageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.welcomepageview

import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.designsystem.modules.theming.models.Themes
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.modules.common.services.NotificationService
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancel
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.localized
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
 *   loads anyway. The page also resets the app's theme to its default,
 *   clears the application badge, and begins cycling the welcome label.
 * - On every appearance, the page restores the device's language code,
 *   discards any values recorded by [OnboardingService] during a
 *   previous onboarding attempt, and signs the user in anonymously
 *   after a brief delay.
 * - The welcome label displays the welcome message in a randomly chosen
 *   supported language every few seconds, not repeating a language until
 *   all have been shown. Tapping the label resets it and restarts the
 *   cycling loop.
 * - Tapping continue pushes the language selection page; tapping sign in
 *   pushes the sign-in page.
 */
class WelcomePageReducer : Reducer<WelcomePageReducer.State, WelcomePageReducer.Action> {
    // MARK: - Dependencies

    private val navigation: Navigation by Dependency { it.navigation }
    private val onboardingService: OnboardingService by Dependency { it.onboardingService }

    // MARK: - Actions

    /** The actions the welcome page can process. */
    sealed interface Action {
        /**
         * An action that indicates the view appeared. Resets the welcome
         * label, restores the device's language code, discards any
         * values recorded by [OnboardingService] during a previous
         * onboarding attempt, and signs the user in anonymously after a
         * brief delay.
         */
        data object ViewAppeared : Action

        /**
         * An action that indicates the view appeared for the first time.
         * Begins display string resolution, resets the app's theme,
         * clears the application badge, and begins cycling the welcome
         * label.
         */
        data object ViewFirstAppeared : Action

        /** An action that indicates the user tapped the continue button. Pushes the language selection page. */
        data object ContinueButtonTapped : Action

        /** An action that indicates the user tapped the sign in button. Pushes the sign-in page. */
        data object SignInButtonTapped : Action

        /** An action that indicates the user tapped the welcome label. Resets the label and restarts the cycling loop. */
        data object WelcomeLabelTapped : Action

        /**
         * An action that displays the welcome message in a randomly
         * chosen supported language, then schedules the next cycle.
         * Languages do not repeat until all have been shown.
         */
        data object CycleWelcomeLabelText : Action

        /** An action that indicates display string resolution failed, carrying the resulting [Exception]. */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates display string resolution succeeded, carrying the resolved strings. */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action
    }

    // MARK: - State

    /** The state of the welcome page. */
    data class State(
        /** The page's translated display strings. Contains the default, untranslated strings until resolution completes. */
        val strings: List<TranslationOutputMap> = WelcomePageViewStrings.defaultOutputMap,
        /** The page's loading state. Remains loading until display string resolution completes. */
        val viewState: ViewState = ViewState.Loading,
        /** The text the welcome label displays. Cycles through the supported languages while the page is visible. */
        val welcomeLabelText: String = LocalizedStringKey.WelcomeToHello.localized(),
        internal val cycledLanguageCodes: Map<String, String> = emptyMap(),
    ) {
        internal val supportedLanguageCodes: List<String>
            get() = RuntimeStorage.languageCodeDictionary?.keys?.toList() ?: emptyList()
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                val newState =
                    state.copy(
                        welcomeLabelText =
                            LocalizedStringKey.WelcomeToHello.localized(languageCode = Locale.getDefault().language),
                    )

                CoreUtilities.restoreDeviceLanguageCode()
                onboardingService.flushValues()

                ReduceResult(
                    newState,
                    Effect.task(delay = ANONYMOUS_SIGN_IN_DELAY) {
                        try {
                            Networking.config.authDelegate.signInAnonymously()
                        } catch (exception: Exception) {
                            Logger.log(exception, with = AlertType.toastInPrerelease)
                        }

                        null
                    },
                )
            }

            Action.ViewFirstAppeared -> {
                ThemeService.setStyleOverride(null)
                ThemeService.setTheme(Themes.appDefault)

                val resetBadgeNumberEffect =
                    Effect.fireAndForget<Action> {
                        try {
                            NotificationService.setBadgeNumber(0, updateHostedValue = false)
                        } catch (exception: Exception) {
                            Logger.log(exception)
                        }
                    }

                ReduceResult(
                    state.copy(viewState = ViewState.Loading),
                    resolveEffect()
                        .merge(resetBadgeNumberEffect)
                        .merge(cycleWelcomeLabelTextEffect(delay = INITIAL_CYCLE_DELAY)),
                )
            }

            Action.ContinueButtonTapped -> {
                navigation.navigate(Route.Onboarding(OnboardingRoute.Push(OnboardingNavigatorState.SeguePath.SelectLanguage)))
                ReduceResult(state)
            }

            Action.CycleWelcomeLabelText -> reduceCycleWelcomeLabelText(state)

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))

            Action.SignInButtonTapped -> {
                navigation.navigate(Route.Onboarding(OnboardingRoute.Push(OnboardingNavigatorState.SeguePath.SignIn)))
                ReduceResult(state)
            }

            Action.WelcomeLabelTapped ->
                ReduceResult(
                    state.copy(welcomeLabelText = LocalizedStringKey.WelcomeToHello.localized()),
                    Effect
                        .cancel<Action>(TaskID.CYCLE_WELCOME_LABEL_TEXT)
                        .merge(cycleWelcomeLabelTextEffect(delay = INITIAL_CYCLE_DELAY)),
                )
        }

    // MARK: - Auxiliary

    private fun reduceCycleWelcomeLabelText(state: State): ReduceResult<State, Action> {
        if (state.cycledLanguageCodes.size >= state.supportedLanguageCodes.size) {
            return ReduceResult(state.copy(cycledLanguageCodes = emptyMap()), cycleWelcomeLabelTextEffect())
        }

        val randomLanguageCode =
            state.supportedLanguageCodes.randomOrNull()
                ?: return ReduceResult(state, cycleWelcomeLabelTextEffect())

        val localizedString = LocalizedStringKey.WelcomeToHello.localized(languageCode = randomLanguageCode)

        if (state.cycledLanguageCodes[randomLanguageCode] != null ||
            state.cycledLanguageCodes.values.contains(localizedString) ||
            state.welcomeLabelText == localizedString
        ) {
            return ReduceResult(state, cycleWelcomeLabelTextEffect())
        }

        return ReduceResult(
            state.copy(
                cycledLanguageCodes = state.cycledLanguageCodes + (randomLanguageCode to localizedString),
                welcomeLabelText = localizedString,
            ),
            cycleWelcomeLabelTextEffect(delay = CYCLE_DELAY),
        )
    }

    private fun cycleWelcomeLabelTextEffect(delay: Duration = Duration.ZERO): Effect<Action> =
        Effect
            .task<Action>(delay = delay) { Action.CycleWelcomeLabelText }
            .cancellable(TaskID.CYCLE_WELCOME_LABEL_TEXT)

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(WelcomePageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    // MARK: - Companion

    private enum class TaskID {
        CYCLE_WELCOME_LABEL_TEXT,
    }

    private companion object {
        val ANONYMOUS_SIGN_IN_DELAY = 1.seconds
        val CYCLE_DELAY = 3.seconds
        val INITIAL_CYCLE_DELAY = 5.seconds
    }
}
