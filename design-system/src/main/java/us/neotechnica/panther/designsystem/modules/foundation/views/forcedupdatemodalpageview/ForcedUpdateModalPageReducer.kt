//
//  ForcedUpdateModalPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.forcedupdatemodalpageview

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.designsystem.modules.foundation.views.root.RootWindowStatus
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.services.TranslationService
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class ForcedUpdateModalPageReducer : Reducer<ForcedUpdateModalPageReducer.State, ForcedUpdateModalPageReducer.Action> {
    // MARK: - Actions

    sealed interface Action {
        data object ViewAppeared : Action

        data object InstallButtonTapped : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        data class ResolveReturned(
            val strings: Map<ForcedUpdateModalPageViewStringKey, String>,
        ) : Action
    }

    // MARK: - State

    data class State(
        val shouldShowInstallButton: Boolean = false,
        val strings: Map<ForcedUpdateModalPageViewStringKey, String> = ForcedUpdateModalPageViewStrings.defaultOutputMap,
        val versionLabelText: String = "",
        val viewState: ViewState = ViewState.Loading,
    ) {
        // MARK: - Computed Properties

        val installButtonRedirectURL: String?
            get() = AppSubsystem.delegates.forcedUpdateModal?.installButtonRedirectURL
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                val installButtonRedirectURL = state.installButtonRedirectURL
                ReduceResult(
                    state.copy(
                        shouldShowInstallButton =
                            installButtonRedirectURL != null &&
                                canOpenURL(installButtonRedirectURL),
                        versionLabelText = versionLabelText,
                    ),
                    hideInteractiveContentTask
                        .merge(resolveTask),
                )
            }

            Action.InstallButtonTapped -> {
                val installButtonRedirectURL = state.installButtonRedirectURL
                if (installButtonRedirectURL == null) {
                    ReduceResult(state)
                } else {
                    ReduceResult(
                        state,
                        Effect.fireAndForget { open(installButtonRedirectURL) },
                    )
                }
            }

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }

            is Action.ResolveReturned ->
                ReduceResult(
                    state.copy(
                        strings = action.strings,
                        viewState = ViewState.Loaded,
                    ),
                )
        }

    // MARK: - Auxiliary

    private val hideInteractiveContentTask: Effect<Action>
        get() =
            Effect.fireAndForget {
                while (true) {
                    hideInteractiveContent()
                    if (Build.isDeveloperModeEnabled) break
                    delay(HIDE_INTERACTIVE_CONTENT_INTERVAL_MILLISECONDS.milliseconds)
                }
            }

    private val resolveTask: Effect<Action>
        get() =
            Effect.task {
                try {
                    Action.ResolveReturned(resolve())
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    Action.ResolveFailed(
                        throwable as? Exception ?: Exception.from(throwable, ExceptionMetadata(this)),
                    )
                }
            }

    private val versionLabelText: String
        get() =
            "v${Build.bundleVersion} " +
                "(${Build.buildNumber}${Build.milestone.shortString}/${Build.bundleRevision.lowercase()})"

    private fun canOpenURL(url: String): Boolean {
        val activity = Translator.config.currentActivityProvider?.invoke() ?: return false
        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).resolveActivity(activity.packageManager) != null
    }

    private fun hideInteractiveContent() {
        Toast.hide()
        RootWindowStatus.setRootOverlayWindowAlpha(0f)
        AlertPresenter.dismiss()
        RootSheets.dismiss()
        KeyboardService.resignFirstResponders()
    }

    private fun open(url: String) {
        val activity = Translator.config.currentActivityProvider?.invoke() ?: return
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private suspend fun resolve(): Map<ForcedUpdateModalPageViewStringKey, String> {
        val keyPairs = ForcedUpdateModalPageViewStrings.keyPairs
        val inputs = keyPairs.map { it.second }
        val languagePair =
            LanguagePair(
                from = "en",
                to = RuntimeStorage.languageCode,
            )

        val translations =
            withTimeoutOrNull(TRANSLATION_TIMEOUT_SECONDS.seconds) {
                TranslationService.getTranslations(inputs, languagePair)
            } ?: inputs.map {
                Translation(
                    input = it,
                    output = it.value,
                    languagePair = languagePair,
                )
            }

        return keyPairs.associate { (key, input) ->
            key to (translations.firstOrNull { it.input.value == input.value }?.output ?: key.rawValue)
        }
    }
}

private const val HIDE_INTERACTIVE_CONTENT_INTERVAL_MILLISECONDS = 100L
private const val TRANSLATION_TIMEOUT_SECONDS = 10L
