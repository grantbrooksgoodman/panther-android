//
//  NetworkActivityIndicatorReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.components.networkactivityindicator

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.constants.NetworkActivityIndicatorFloats
import us.neotechnica.panther.networking.modules.common.extensions.NetworkingStorageKey
import us.neotechnica.panther.networking.modules.common.extensions.inspectNetworkHealthAction
import us.neotechnica.panther.networking.modules.common.extensions.isNetworkActivityOccurring
import us.neotechnica.panther.networking.modules.common.extensions.networking
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancel
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import kotlin.time.Duration.Companion.seconds

/**
 * The reducer that drives the network activity indicator.
 *
 * The indicator shows while network activity is occurring – gated
 * on a prerelease build, Developer Mode, and the persisted
 * enablement flag – and hides shortly after activity ends.
 */
class NetworkActivityIndicatorReducer : Reducer<NetworkActivityIndicatorReducer.State, NetworkActivityIndicatorReducer.Action> {
    // MARK: - Actions

    /** An event the view sends to the reducer. */
    sealed interface Action {
        data object HealthChanged : Action

        data object HideIfInactive : Action

        data object HideIndicator : Action

        data object IndicatorTapped : Action

        data class IsVisibleChanged(
            val isVisible: Boolean,
        ) : Action
    }

    // MARK: - State

    /** The state the reducer operates on. */
    data class State(
        /** The background color of the indicator. */
        val backgroundColor: Color? = null,
        /** Whether the indicator is currently visible. */
        val isVisible: Boolean = false,
        /** The indicator's vertical offset. */
        val yOffset: Float = NetworkActivityIndicatorFloats.HIDDEN_Y_OFFSET,
    ) {
        // MARK: - Types

        internal enum class TaskID {
            HIDE_IF_INACTIVE,
            HIDE_INDICATOR,
        }

        // MARK: - Computed Properties

        /** Whether the indicator currently responds to taps. */
        val allowsHitTesting: Boolean
            get() = isVisible && AlertPresenter.current.value == null

        /** The tint color of the indicator's progress view. */
        val progressViewTintColor: Color?
            get() = Networking.config.activityIndicatorDelegate.progressViewTintColor
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.HealthChanged ->
                ReduceResult(
                    state.copy(
                        backgroundColor =
                            Networking
                                .config
                                .activityIndicatorDelegate
                                .backgroundColor,
                    ),
                )

            Action.HideIfInactive -> ReduceResult(state, hideIndicatorEffect)

            Action.HideIndicator ->
                if (!state.isVisible) {
                    ReduceResult(state)
                } else {
                    ReduceResult(
                        state.copy(
                            isVisible = false,
                            yOffset = NetworkActivityIndicatorFloats.HIDDEN_Y_OFFSET,
                        ),
                    )
                }

            Action.IndicatorTapped ->
                if (!state.allowsHitTesting) {
                    ReduceResult(state)
                } else {
                    ReduceResult(
                        state,
                        Effect.fireAndForget {
                            val tapAction =
                                Networking
                                    .config
                                    .activityIndicatorDelegate
                                    .tapAction

                            if (tapAction == null) {
                                DevModeAction.inspectNetworkHealthAction.perform()
                            } else {
                                tapAction()
                            }
                        },
                    )
                }

            is Action.IsVisibleChanged -> reduceIsVisibleChanged(state, action)
        }

    // MARK: - Auxiliary

    private val hideIfInactiveEffect: Effect<Action>
        get() =
            Effect
                .task<Action>(delay = NetworkActivityIndicatorFloats.HIDE_IF_INACTIVE_TASK_DELAY_SECONDS.seconds) {
                    if (SharedState { it.isNetworkActivityOccurring }.wrappedValue) {
                        null
                    } else {
                        Action.HideIfInactive
                    }
                }.cancellable(
                    State.TaskID.HIDE_IF_INACTIVE,
                    cancelInFlight = true,
                )

    private val hideIndicatorEffect: Effect<Action>
        get() =
            Effect
                .cancel<Action>(State.TaskID.HIDE_INDICATOR)
                .merge(
                    Effect
                        .task<Action>(delay = NetworkActivityIndicatorFloats.HIDE_INDICATOR_TASK_DELAY_SECONDS.seconds) {
                            Action.HideIndicator
                        }.cancellable(State.TaskID.HIDE_INDICATOR),
                )

    private fun reduceIsVisibleChanged(
        state: State,
        action: Action.IsVisibleChanged,
    ): ReduceResult<State, Action> {
        val isNetworkActivityIndicatorEnabled =
            Persistent.booleanOrNull(
                PersistentStorageKey.networking(NetworkingStorageKey.IS_NETWORK_ACTIVITY_INDICATOR_ENABLED),
            )

        val canShowIndicator =
            Build.milestone != Build.Milestone.GENERAL_RELEASE &&
                Build.isDeveloperModeEnabled &&
                isNetworkActivityIndicatorEnabled == true

        fun activityChangeEffects(): Effect<Action> {
            val effects =
                mutableListOf(
                    hideIfInactiveEffect,
                    hideIndicatorEffect,
                )

            if (canShowIndicator) {
                effects.add(Effect.fireAndForget { performMediumImpactHaptic() })
            }

            return Effect.merge(effects)
        }

        if (!action.isVisible || state.isVisible == canShowIndicator) {
            return ReduceResult(state, activityChangeEffects())
        }

        return ReduceResult(
            state.copy(
                isVisible = canShowIndicator,
                yOffset = if (canShowIndicator) 0f else NetworkActivityIndicatorFloats.HIDDEN_Y_OFFSET,
            ),
            activityChangeEffects(),
        )
    }

    private fun performMediumImpactHaptic() {
        if (VERSION.SDK_INT < VERSION_CODES.Q) return

        runCatching {
            val context = Networking.requireContext()
            val vibrator =
                if (VERSION.SDK_INT >= VERSION_CODES.S) {
                    context
                        .getSystemService(VibratorManager::class.java)
                        .defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Vibrator::class.java)
                }

            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }
}
