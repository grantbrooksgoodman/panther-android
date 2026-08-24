//
//  SelectLanguagePageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview

import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.onboarding.models.InstructionViewStrings
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * The reducer for the native-language selection page.
 *
 * The user picks their language from the supported set; continuing
 * records the language code and advances to phone-number entry.
 */
class SelectLanguagePageReducer : Reducer<SelectLanguagePageReducer.State, SelectLanguagePageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object BackButtonTapped : Action

        data object ContinueButtonTapped : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        data class SelectedLanguageNameChanged(
            val name: String,
        ) : Action
    }

    // MARK: - State

    data class State(
        val instructionViewStrings: InstructionViewStrings = InstructionViewStrings.empty,
        val languages: List<String> = listOf(),
        val selectedLanguageName: String = "",
        val strings: List<TranslationOutputMap> = SelectLanguagePageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
    ) {
        val selectedLanguageCode: String
            get() =
                LocalizedStringResolver
                    .languageDisplayNames()
                    .entries
                    .firstOrNull { it.value == selectedLanguageName }
                    ?.key
                    ?: RuntimeStorage.languageCode
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                val displayNames = LocalizedStringResolver.languageDisplayNames()
                val languages = displayNames.values.sorted()
                val selected = displayNames[RuntimeStorage.languageCode] ?: languages.firstOrNull() ?: ""
                ReduceResult(
                    state.copy(
                        languages = languages,
                        selectedLanguageName = selected,
                        viewState = ViewState.Loading,
                    ),
                    resolveEffect(),
                )
            }

            Action.BackButtonTapped -> {
                navigate(OnboardingRoute.Pop)
                ReduceResult(state)
            }

            Action.ContinueButtonTapped -> {
                val languageCode = state.selectedLanguageCode
                RuntimeStorage.languageCode = languageCode
                OnboardingService.setLanguageCode(languageCode)
                navigate(OnboardingRoute.Push(OnboardingNavigatorState.SeguePath.VerifyNumber))
                ReduceResult(state)
            }

            is Action.SelectedLanguageNameChanged ->
                ReduceResult(state.copy(selectedLanguageName = action.name))

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings).withResolvedInstruction(action.strings))

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.withResolvedInstruction(state.strings))
            }
        }

    // MARK: - Auxiliary

    private fun State.withResolvedInstruction(strings: List<TranslationOutputMap>): State =
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

    private fun navigate(route: OnboardingRoute) {
        DependencyValues.current.navigation.navigate(Route.Onboarding(route))
    }
}

/** The translated label strings for the language-selection page. */
object SelectLanguagePageViewStrings : TranslatedLabelStrings {
    val backButtonText = TranslatedLabelStringCollection("selectLanguagePageView.backButtonText")
    val continueButtonText = TranslatedLabelStringCollection("selectLanguagePageView.continueButtonText")
    val instructionLabelText = TranslatedLabelStringCollection("selectLanguagePageView.instructionLabelText")
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("selectLanguagePageView.instructionViewSubtitleLabelText")
    val instructionViewTitleLabelText =
        TranslatedLabelStringCollection("selectLanguagePageView.instructionViewTitleLabelText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(backButtonText, TranslationInput("Back", alternate = "Go back")),
            TranslationInputMap(continueButtonText, TranslationInput("Continue")),
            TranslationInputMap(instructionLabelText, TranslationInput("I speak:")),
            TranslationInputMap(
                instructionViewSubtitleLabelText,
                TranslationInput(
                    "To begin, select your native language.\n\nThis will be the language you send and " +
                        "receive messages in, as well as that of system dialogues. Your selection can be " +
                        "changed later in Settings.",
                ),
            ),
            TranslationInputMap(instructionViewTitleLabelText, TranslationInput("Select Native Language")),
        )
}
