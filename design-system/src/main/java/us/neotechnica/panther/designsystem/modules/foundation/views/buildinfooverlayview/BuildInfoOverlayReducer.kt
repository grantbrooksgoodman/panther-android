//
//  BuildInfoOverlayReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.buildinfooverlayview

import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.foundation.dependencies.buildInfoOverlayViewService
import us.neotechnica.panther.designsystem.modules.foundation.extensions.buildInfoOverlayDotIndicatorColor
import us.neotechnica.panther.designsystem.modules.foundation.extensions.presentedViewsCount
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import kotlin.time.Duration.Companion.seconds

internal class BuildInfoOverlayReducer : Reducer<BuildInfoOverlayReducer.State, BuildInfoOverlayReducer.Action> {
    // MARK: - Types

    private enum class CancelID {
        RESTORE_OPAQUE_APPEARANCE,
    }

    // MARK: - Dependencies

    private val viewService: BuildInfoOverlayViewService by Dependency { it.buildInfoOverlayViewService }

    // MARK: - Actions

    sealed interface Action {
        data object ViewAppeared : Action

        data object BuildInfoButtonTapped : Action

        data object SendFeedbackButtonTapped : Action

        data object RestoreIndicatorColor : Action

        data object RootViewTapped : Action

        data class ShouldUseTranslucentAppearanceChanged(
            val shouldUseTranslucentAppearance: Boolean,
        ) : Action

        data object UpdateStatsLabelText : Action
    }

    // MARK: - State

    data class State(
        val buildInfoButtonText: String = "",
        val developerModeIndicatorDotColor: Color = defaultDeveloperModeIndicatorDotColor,
        val shouldUseTranslucentAppearance: Boolean = false,
        val statsLabelText: String = "Calculating...",
        val yOffset: Float = 0f,
    ) {
        // MARK: - Computed Properties

        val backgroundColor: Color
            get() = Color.Black.copy(alpha = if (shouldUseTranslucentAppearance) TRANSLUCENT_ALPHA else 1f)

        val isDeveloperModeEnabled: Boolean
            get() = Build.isDeveloperModeEnabled

        val isUserInteractionDisabled: Boolean
            get() = AlertPresenter.current.value != null

        val sendFeedbackButtonText: String
            get() = Localized(SubsystemStringKey.SEND_FEEDBACK).wrappedValue
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared ->
                ReduceResult(
                    state.copy(
                        buildInfoButtonText =
                            "${Build.codeName} ${Build.bundleVersion} " +
                                "(${Build.buildNumber}${Build.milestone.shortString}/${Build.bundleRevision.lowercase()})",
                    ),
                    Effect.task { Action.UpdateStatsLabelText },
                )

            Action.BuildInfoButtonTapped ->
                ReduceResult(
                    state,
                    Effect.fireAndForget { viewService.buildInfoButtonTapped() },
                )

            Action.RestoreIndicatorColor ->
                ReduceResult(state.copy(developerModeIndicatorDotColor = defaultDeveloperModeIndicatorDotColor))

            Action.RootViewTapped ->
                ReduceResult(
                    state.copy(shouldUseTranslucentAppearance = true),
                    Effect
                        .task<Action>(delay = RESTORE_OPAQUE_APPEARANCE_DELAY_SECONDS.seconds) {
                            Action.ShouldUseTranslucentAppearanceChanged(false)
                        }.cancellable(
                            CancelID.RESTORE_OPAQUE_APPEARANCE,
                            cancelInFlight = true,
                        ),
                )

            Action.SendFeedbackButtonTapped ->
                ReduceResult(
                    state,
                    Effect.fireAndForget { viewService.sendFeedbackButtonTapped() },
                )

            is Action.ShouldUseTranslucentAppearanceChanged ->
                ReduceResult(state.copy(shouldUseTranslucentAppearance = action.shouldUseTranslucentAppearance))

            Action.UpdateStatsLabelText ->
                ReduceResult(
                    state.copy(statsLabelText = statsLabelText),
                    Effect.task(delay = UPDATE_STATS_LABEL_TEXT_DELAY_SECONDS.seconds) {
                        Action.UpdateStatsLabelText
                    },
                )
        }

    // MARK: - Auxiliary

    private val statsLabelText: String
        get() {
            val presentedViewsCount = SharedState { it.presentedViewsCount }.wrappedValue
            return "$presentedViewsCount views // ${CoreUtilities.appMemoryFootprint ?: 0}MB in use"
        }
}

private val defaultDeveloperModeIndicatorDotColor: Color
    get() =
        AppSubsystem
            .delegates
            .buildInfoOverlayDotIndicatorColor
            ?.developerModeIndicatorDotColor ?: Color(0xFFFF9500)

private const val RESTORE_OPAQUE_APPEARANCE_DELAY_SECONDS = 5L
private const val TRANSLUCENT_ALPHA = 0.35f
private const val UPDATE_STATS_LABEL_TEXT_DELAY_SECONDS = 1L
