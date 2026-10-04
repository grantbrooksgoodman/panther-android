//
//  PermissionPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.permissionpageview

import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.services.PermissionService
import us.neotechnica.panther.modules.content.onboarding.components.InstructionViewStrings
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer for the final onboarding permissions page.
 *
 * The user grants notification and contact permissions (both
 * optional), then finishes: agreeing to the conduct policy creates the
 * account and enters the app.
 *
 * Tapping a permission capsule asks the reducer to request the
 * permission through [PermissionService]; a denial presents a
 * settings call-to-action, and granting contacts syncs the archive.
 */
class PermissionPageReducer : Reducer<PermissionPageReducer.State, PermissionPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object BackButtonTapped : Action

        data object ContactPermissionCapsuleButtonTapped : Action

        data object NotificationPermissionCapsuleButtonTapped : Action

        data object FinishButtonTapped : Action

        data class RequestContactPermissionReturned(
            val status: PermissionService.PermissionStatus,
        ) : Action

        data class RequestContactPermissionFailed(
            val exception: Exception,
        ) : Action

        data class RequestNotificationPermissionReturned(
            val status: PermissionService.PermissionStatus,
        ) : Action

        data class RequestNotificationPermissionFailed(
            val exception: Exception,
        ) : Action

        data class EulaAlertDismissed(
            val cancelled: Boolean,
        ) : Action

        data class CreateUserReturned(
            val exception: Exception?,
        ) : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action
    }

    // MARK: - State

    data class State(
        val instructionViewStrings: InstructionViewStrings = InstructionViewStrings.empty,
        val isBackButtonEnabled: Boolean = true,
        val isContactPermissionGranted: Boolean? = null,
        val isFinishButtonEnabled: Boolean = false,
        val isNotificationPermissionGranted: Boolean? = null,
        val strings: List<TranslationOutputMap> = PermissionPageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
    )

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared ->
                ReduceResult(state.copy(viewState = ViewState.Loading), resolveEffect())

            Action.BackButtonTapped -> {
                DependencyValues.current.navigation.navigate(Route.Onboarding(OnboardingRoute.Pop))
                ReduceResult(state)
            }

            Action.ContactPermissionCapsuleButtonTapped -> contactPermissionCapsuleButtonTapped(state)

            Action.NotificationPermissionCapsuleButtonTapped -> notificationPermissionCapsuleButtonTapped(state)

            is Action.RequestContactPermissionReturned -> requestContactPermissionReturned(state, action.status)

            is Action.RequestContactPermissionFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(isBackButtonEnabled = true, isFinishButtonEnabled = false))
            }

            is Action.RequestNotificationPermissionReturned -> requestNotificationPermissionReturned(state, action.status)

            is Action.RequestNotificationPermissionFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(isBackButtonEnabled = true, isFinishButtonEnabled = false))
            }

            Action.FinishButtonTapped -> {
                Overlay.show()
                ReduceResult(
                    state.copy(isBackButtonEnabled = false, isFinishButtonEnabled = false),
                    Effect.run { send ->
                        send(Action.EulaAlertDismissed(OnboardingService.presentEulaAlert()))
                    },
                )
            }

            is Action.EulaAlertDismissed ->
                if (action.cancelled) {
                    Overlay.hide()
                    ReduceResult(state.copy(isBackButtonEnabled = true, isFinishButtonEnabled = true))
                } else {
                    ReduceResult(
                        state,
                        Effect.run { send ->
                            try {
                                OnboardingService.createUser()
                                send(Action.CreateUserReturned(null))
                            } catch (exception: Exception) {
                                send(Action.CreateUserReturned(exception))
                            }
                        },
                    )
                }

            is Action.CreateUserReturned -> {
                Overlay.hide()
                val exception = action.exception
                if (exception != null) {
                    Logger.log(exception, with = AlertType.toast)
                    ReduceResult(state.copy(isBackButtonEnabled = true, isFinishButtonEnabled = true))
                } else {
                    // Clear the onboarding stack so a later sign-out returns to
                    // the welcome page rather than a stale onboarding page.
                    DependencyValues.current.navigation.navigate(
                        Route.Onboarding(OnboardingRoute.Stack(emptyList())),
                    )
                    DependencyValues.current.navigation.navigate(
                        Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash)),
                    )
                    ReduceResult(state)
                }
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings).withResolvedInstruction(action.strings))

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.withResolvedInstruction(state.strings))
            }
        }

    // MARK: - Auxiliary

    private fun contactPermissionCapsuleButtonTapped(state: State): ReduceResult<State, Action> =
        ReduceResult(
            state.copy(isFinishButtonEnabled = state.isNotificationPermissionGranted != null),
            Effect.run { send ->
                try {
                    send(
                        Action.RequestContactPermissionReturned(
                            PermissionService.requestPermission(PermissionService.PermissionType.CONTACTS),
                        ),
                    )
                } catch (exception: Exception) {
                    send(Action.RequestContactPermissionFailed(exception))
                }
            },
        )

    private fun notificationPermissionCapsuleButtonTapped(state: State): ReduceResult<State, Action> =
        ReduceResult(
            state.copy(isFinishButtonEnabled = state.isContactPermissionGranted != null),
            Effect.run { send ->
                try {
                    send(
                        Action.RequestNotificationPermissionReturned(
                            PermissionService.requestPermission(PermissionService.PermissionType.NOTIFICATIONS),
                        ),
                    )
                } catch (exception: Exception) {
                    send(Action.RequestNotificationPermissionFailed(exception))
                }
            },
        )

    private fun requestContactPermissionReturned(
        state: State,
        status: PermissionService.PermissionStatus,
    ): ReduceResult<State, Action> =
        if (status != PermissionService.PermissionStatus.GRANTED) {
            ReduceResult(
                state.copy(isContactPermissionGranted = false),
                Effect.task(delay = CTA_PRESENTATION_DELAY_MS.milliseconds) {
                    PermissionService.presentCTA(PermissionService.PermissionType.CONTACTS)
                    null
                },
            )
        } else {
            ReduceResult(
                state.copy(isContactPermissionGranted = true),
                Effect.fireAndForget {
                    try {
                        ContactService.syncContactPairArchive()
                    } catch (exception: Exception) {
                        Logger.log(exception)
                    }
                },
            )
        }

    private fun requestNotificationPermissionReturned(
        state: State,
        status: PermissionService.PermissionStatus,
    ): ReduceResult<State, Action> =
        if (status != PermissionService.PermissionStatus.GRANTED) {
            ReduceResult(
                state.copy(isNotificationPermissionGranted = false),
                Effect.task(delay = CTA_PRESENTATION_DELAY_MS.milliseconds) {
                    PermissionService.presentCTA(PermissionService.PermissionType.NOTIFICATIONS)
                    null
                },
            )
        } else {
            // iOS calls registerForRemoteNotifications() here; Android's FCM token is obtained
            // automatically, so no explicit registration call is needed.
            ReduceResult(state.copy(isNotificationPermissionGranted = true))
        }

    private fun State.withResolvedInstruction(strings: List<TranslationOutputMap>): State =
        copy(
            instructionViewStrings =
                InstructionViewStrings(
                    titleLabelText = strings.value(PermissionPageViewStrings.instructionViewTitleLabelText),
                    subtitleLabelText = strings.value(PermissionPageViewStrings.instructionViewSubtitleLabelText),
                ),
            viewState = ViewState.Loaded,
        )

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(
                    Action.ResolveReturned(
                        Networking.config.hostedTranslationDelegate.resolve(PermissionPageViewStrings),
                    ),
                )
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    // MARK: - Companion

    private companion object {
        const val CTA_PRESENTATION_DELAY_MS = 500L
    }
}
