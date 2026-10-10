//
//  SelectLanguagePageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview

import us.neotechnica.panther.bundle.activityDescription
import us.neotechnica.panther.bundle.conversationCellViewData
import us.neotechnica.panther.bundle.regionDetailService
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.onboarding.components.InstructionViewStrings
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.util.Locale

/**
 * The reducer that drives the language selection page of the
 * onboarding flow.
 *
 * This page is the first step of sign-up. The user chooses the language
 * the app's content is translated into, and the reducer commits the
 * choice before phone number verification begins.
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings,
 *   remaining in the loading state until resolution completes. If
 *   resolution fails, the page falls back to its default strings and
 *   loads anyway.
 * - The page builds its language list from the localized language code
 *   dictionary, selecting the app's current language by default. If the
 *   dictionary is unavailable, the page enters the error state instead.
 * - Tapping continue clears the language-dependent caches, sets the
 *   app's language to the selection, records the choice with
 *   [OnboardingService.setLanguageCode], and pushes the phone number
 *   entry page.
 */
class SelectLanguagePageReducer : Reducer<SelectLanguagePageReducer.State, SelectLanguagePageReducer.Action> {
    // MARK: - Dependencies

    private val navigation: Navigation by Dependency { it.navigation }
    private val onboardingService: OnboardingService by Dependency { it.onboardingService }

    // MARK: - Actions

    /** The actions the language selection page can process. */
    sealed interface Action {
        /** An action that indicates the view appeared. Builds the language list and begins display string resolution. */
        data object ViewAppeared : Action

        /** An action that indicates the user tapped the back button. Pops the current page. */
        data object BackButtonTapped : Action

        /**
         * An action that indicates the user tapped the continue button.
         * Commits the selected language and pushes the phone number
         * entry page.
         */
        data object ContinueButtonTapped : Action

        /** An action that indicates display string resolution failed, carrying the resulting [Exception]. */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates display string resolution succeeded, carrying the resolved strings. */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        /** An action that indicates the selected language changed, carrying the new language's display name. */
        data class SelectedLanguageNameChanged(
            val selectedLanguageName: String,
        ) : Action
    }

    // MARK: - State

    /** The state of the language selection page. */
    data class State(
        /** The strings the page's instruction header displays. Populated once display string resolution completes. */
        val instructionViewStrings: InstructionViewStrings = InstructionViewStrings.empty,
        /** The display names of the selectable languages, sorted alphabetically. */
        val languages: List<String> = emptyList(),
        /** The display name of the selected language. */
        val selectedLanguageName: String = "",
        /** The page's translated display strings. Contains the default, untranslated strings until resolution completes. */
        val strings: List<TranslationOutputMap> = SelectLanguagePageViewStrings.defaultOutputMap,
        /** The page's loading state. Remains loading until display string resolution completes. */
        val viewState: ViewState = ViewState.Loading,
    ) {
        internal val selectedLanguageCode: String
            get() =
                CoreUtilities
                    .localizedLanguageCodeDictionary(Locale.getDefault().language)
                    ?.entries
                    ?.firstOrNull { it.value == selectedLanguageName }
                    ?.key
                    ?: RuntimeStorage.languageCode
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> reduceViewAppeared(state)

            Action.BackButtonTapped -> {
                navigation.navigate(Route.Onboarding(OnboardingRoute.Pop))
                ReduceResult(state)
            }

            Action.ContinueButtonTapped -> {
                CoreUtilities.clearCaches(
                    listOf(
                        CacheDomain.activityDescription,
                        CacheDomain.conversationCellViewData,
                        CacheDomain.localization,
                        CacheDomain.regionDetailService,
                    ),
                )

                val selectedLanguageCode = state.selectedLanguageCode
                CoreUtilities.setLanguageCode(selectedLanguageCode)

                navigation.navigate(Route.Onboarding(OnboardingRoute.Push(OnboardingNavigatorState.SeguePath.VerifyNumber)))
                onboardingService.setLanguageCode(selectedLanguageCode)
                ReduceResult(state)
            }

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.withInstructionViewStrings(state.strings))
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings).withInstructionViewStrings(action.strings))

            is Action.SelectedLanguageNameChanged ->
                ReduceResult(state.copy(selectedLanguageName = action.selectedLanguageName))
        }

    // MARK: - Auxiliary

    private fun reduceViewAppeared(state: State): ReduceResult<State, Action> {
        val localizedLanguageCodeDictionary = CoreUtilities.localizedLanguageCodeDictionary(RuntimeStorage.languageCode)

        if (localizedLanguageCodeDictionary == null) {
            val exception =
                Exception(
                    "No localized language code dictionary.",
                    metadata = ExceptionMetadata(this),
                )

            Logger.log(exception)
            return ReduceResult(state.copy(viewState = ViewState.Error(exception)))
        }

        return ReduceResult(
            state.copy(
                languages = localizedLanguageCodeDictionary.values.sorted(),
                selectedLanguageName =
                    localizedLanguageCodeDictionary[RuntimeStorage.languageCode]
                        ?: localizedLanguageCodeDictionary.values.firstOrNull()
                        ?: "",
                viewState = ViewState.Loading,
            ),
            resolveEffect(),
        )
    }

    private fun State.withInstructionViewStrings(strings: List<TranslationOutputMap>): State =
        copy(
            instructionViewStrings =
                InstructionViewStrings(
                    titleLabelText = strings.value(SelectLanguagePageViewStrings.instructionViewTitleLabelText),
                    subtitleLabelText = strings.value(SelectLanguagePageViewStrings.instructionViewSubtitleLabelText),
                ),
            viewState = ViewState.Loaded,
        )

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(
                    Action.ResolveReturned(
                        Networking.config.hostedTranslationDelegate.resolve(SelectLanguagePageViewStrings),
                    ),
                )
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }
}
