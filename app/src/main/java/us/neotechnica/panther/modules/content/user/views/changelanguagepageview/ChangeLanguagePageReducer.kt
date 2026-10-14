//
//  ChangeLanguagePageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.changelanguagepageview

import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.onboarding.components.instructionview.InstructionViewStrings
import us.neotechnica.panther.modules.content.user.dependencies.changeLanguagePageViewService
import us.neotechnica.panther.modules.content.user.dependencies.settingsPageViewService
import us.neotechnica.panther.modules.content.user.services.ChangeLanguagePageViewService
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
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
    // MARK: - Dependencies

    private val translator: HostedTranslationDelegate by Dependency { it.networking.hostedTranslation }

    // MARK: - Action

    /** The actions the change language page can process. */
    sealed interface Action {
        /**
         * An action that indicates the view appeared. Builds the
         * language list, selects the current language, and begins
         * resolving the display strings.
         */
        data object ViewAppeared : Action

        /**
         * An action that indicates the view disappeared. Marks the
         * settings page presented again and notifies observers of a
         * trait collection change.
         */
        data object ViewDisappeared : Action

        /** An action that indicates the user tapped the confirm button. Applies the selected language. */
        data object ConfirmButtonTapped : Action

        /** An action that indicates display string resolution succeeded, carrying the resolved strings. */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        /** An action that indicates display string resolution failed, carrying the resulting [Exception]. */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        /**
         * An action that indicates the user selected a language,
         * carrying its name. Enables the confirm button when the
         * selection differs from the current language.
         */
        data class SelectedLanguageNameChanged(
            val name: String,
        ) : Action
    }

    // MARK: - State

    /**
     * The state of the change language page.
     *
     * @property instructionViewStrings The instruction view's title
     *   and subtitle.
     * @property isConfirmButtonEnabled A Boolean value that indicates
     *   whether the confirm button is enabled.
     * @property languages The language names the picker offers, in
     *   alphabetical order.
     * @property selectedLanguageName The name of the selected
     *   language.
     * @property strings The page's translated display strings.
     *   Contains the default, untranslated strings until resolution
     *   completes.
     * @property viewState The page's loading state. Remains loading
     *   until display string resolution completes.
     */
    data class State(
        val instructionViewStrings: InstructionViewStrings = InstructionViewStrings.empty,
        val isConfirmButtonEnabled: Boolean = false,
        val languages: List<String> = listOf(),
        val selectedLanguageName: String = "",
        val strings: List<TranslationOutputMap> = ChangeLanguagePageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
    ) {
        internal val selectedLanguageCode: String
            get() {
                val localizedLanguageCodeDictionary =
                    CoreUtilities.localizedLanguageCodeDictionary(RuntimeStorage.languageCode) ?: return RuntimeStorage.languageCode
                return localizedLanguageCodeDictionary.entries.firstOrNull { it.value == selectedLanguageName }?.key
                    ?: RuntimeStorage.languageCode
            }
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                DependencyValues.current.settingsPageViewService.isMainPagePresented = false
                val languageCodeDictionary =
                    CoreUtilities.localizedLanguageCodeDictionary(RuntimeStorage.languageCode)
                        ?: return ReduceResult(state.copy(viewState = ViewState.Loading))
                val languages = languageCodeDictionary.values.sorted()
                val selectedLanguageName = languageCodeDictionary[RuntimeStorage.languageCode] ?: languages.firstOrNull() ?: ""
                ReduceResult(
                    state.copy(
                        isConfirmButtonEnabled = false,
                        languages = languages,
                        selectedLanguageName = selectedLanguageName,
                        viewState = ViewState.Loading,
                    ),
                    resolveEffect(),
                )
            }

            Action.ViewDisappeared -> {
                DependencyValues.current.settingsPageViewService.isMainPagePresented = true
                val sharedEvents = DependencyValues.current.sharedEvents
                sharedEvents.traitCollectionChanged.send(Unit)
                ReduceResult(state)
            }

            Action.ConfirmButtonTapped -> {
                DependencyValues.current.changeLanguagePageViewService.confirmButtonTapped(state.selectedLanguageCode)
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
                        translator.resolve(ChangeLanguagePageViewStrings),
                    ),
                )
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }
}
