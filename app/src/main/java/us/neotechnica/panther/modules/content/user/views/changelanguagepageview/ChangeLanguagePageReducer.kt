//
//  ChangeLanguagePageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 22/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.changelanguagepageview

import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.onboarding.components.InstructionViewStrings
import us.neotechnica.panther.modules.content.user.services.ChangeLanguagePageViewService
import us.neotechnica.panther.modules.content.user.services.SettingsPageViewService
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

/**
 * The reducer that drives the change-language page.
 *
 * This page lets the user change the language the app's content is
 * translated into from Settings. The user selects a language, and the
 * reducer applies the change through [ChangeLanguagePageViewService].
 *
 * The page's behavior contract:
 *
 * - On appearance, the page builds its language list from the localized
 *   language-name dictionary, selecting the app's current language by
 *   default, and resolves its translated display strings, remaining in
 *   the loading state until resolution completes. If resolution fails,
 *   the page falls back to its default strings and loads anyway.
 * - The confirm button is enabled only while the selected language
 *   differs from the current language.
 * - Tapping confirm applies the selected language.
 */
class ChangeLanguagePageReducer : Reducer<ChangeLanguagePageReducer.State, ChangeLanguagePageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object ViewDisappeared : Action

        data object ConfirmButtonTapped : Action

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
        val isConfirmButtonEnabled: Boolean = false,
        val languages: List<String> = listOf(),
        val selectedLanguageName: String = "",
        val strings: List<TranslationOutputMap> = ChangeLanguagePageViewStrings.defaultOutputMap,
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
                SettingsPageViewService.isMainPagePresented = false
                val displayNames = LocalizedStringResolver.languageDisplayNames()
                val languages = displayNames.values.sorted()
                val selected = displayNames[RuntimeStorage.languageCode] ?: languages.firstOrNull() ?: ""
                ReduceResult(
                    state.copy(
                        isConfirmButtonEnabled = false,
                        languages = languages,
                        selectedLanguageName = selected,
                        viewState = ViewState.Loading,
                    ),
                    resolveEffect(),
                )
            }

            Action.ViewDisappeared -> {
                SettingsPageViewService.isMainPagePresented = true
                val sharedEvents = DependencyValues.current.sharedEvents
                sharedEvents.traitCollectionChanged.send(Unit)
                ReduceResult(state)
            }

            Action.ConfirmButtonTapped -> {
                ChangeLanguagePageViewService.confirmButtonTapped(state.selectedLanguageCode)
                ReduceResult(state)
            }

            is Action.SelectedLanguageNameChanged -> {
                val updated = state.copy(selectedLanguageName = action.name)
                ReduceResult(updated.copy(isConfirmButtonEnabled = updated.selectedLanguageCode != RuntimeStorage.languageCode))
            }

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
                    titleLabelText = strings.value(ChangeLanguagePageViewStrings.instructionViewTitleLabelText),
                    subtitleLabelText = strings.value(ChangeLanguagePageViewStrings.instructionViewSubtitleLabelText),
                ),
            viewState = ViewState.Loaded,
        )

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(
                    Action.ResolveReturned(
                        Networking.config.hostedTranslationDelegate.resolve(ChangeLanguagePageViewStrings),
                    ),
                )
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }
}
