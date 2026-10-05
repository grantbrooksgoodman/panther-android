//
//  AuthCodePageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.authcodepageview

import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.services.KeyboardService
import us.neotechnica.panther.modules.content.onboarding.components.InstructionViewStrings
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.auth.extensions.notReportableForAuthCodes
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
 * The reducer for the verification-code entry page in the sign-up
 * flow. A valid code authenticates the user and advances to the
 * permissions page.
 */
class AuthCodePageReducer : Reducer<AuthCodePageReducer.State, AuthCodePageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object BackButtonTapped : Action

        data object ContinueButtonTapped : Action

        data object DidSwipeDown : Action

        data object RunContinueButtonEffect : Action

        data class AuthenticateUserFailed(
            val exception: Exception,
        ) : Action

        data class AuthenticateUserReturned(
            val userID: String,
        ) : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        data class VerificationCodeChanged(
            val code: String,
        ) : Action
    }

    // MARK: - State

    data class State(
        val instructionViewStrings: InstructionViewStrings = InstructionViewStrings.empty,
        val isBackButtonEnabled: Boolean = true,
        val isContinueButtonEnabled: Boolean = false,
        val strings: List<TranslationOutputMap> = AuthCodePageViewStrings.defaultOutputMap,
        val verificationCode: String = "",
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
                navigate(OnboardingRoute.Pop)
                ReduceResult(state)
            }

            Action.ContinueButtonTapped -> {
                KeyboardService.resignFirstResponders()
                ReduceResult(
                    state,
                    Effect.task(delay = CONTINUE_DELAY_MILLIS.milliseconds) { Action.RunContinueButtonEffect },
                )
            }

            Action.DidSwipeDown -> {
                KeyboardService.resignFirstResponders()
                ReduceResult(state)
            }

            Action.RunContinueButtonEffect -> {
                Overlay.show()
                val code = state.verificationCode
                ReduceResult(
                    state.copy(isBackButtonEnabled = false, isContinueButtonEnabled = false),
                    Effect.run { send ->
                        try {
                            send(
                                Action.AuthenticateUserReturned(
                                    Networking.config.authDelegate.authenticateUser(
                                        authID = OnboardingService.authID ?: "",
                                        verificationCode = code,
                                    ),
                                ),
                            )
                        } catch (exception: Exception) {
                            send(Action.AuthenticateUserFailed(exception))
                        }
                    },
                )
            }

            is Action.AuthenticateUserReturned -> {
                Overlay.hide()
                OnboardingService.setUserID(action.userID)
                navigate(OnboardingRoute.Push(OnboardingNavigatorState.SeguePath.Permission))
                ReduceResult(state.copy(isBackButtonEnabled = true, isContinueButtonEnabled = true))
            }

            is Action.AuthenticateUserFailed -> {
                Overlay.hide()
                Logger.log(
                    action.exception.notReportableForAuthCodes(VERIFICATION_USER_ERROR_CODES),
                    with = AlertType.toast,
                )
                ReduceResult(
                    state.copy(
                        isBackButtonEnabled = true,
                        isContinueButtonEnabled = state.verificationCode.length == VERIFICATION_CODE_LENGTH,
                    ),
                )
            }

            is Action.VerificationCodeChanged ->
                ReduceResult(
                    state.copy(
                        verificationCode = action.code,
                        isContinueButtonEnabled = action.code.length == VERIFICATION_CODE_LENGTH,
                    ),
                )

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
                    titleLabelText = strings.value(AuthCodePageViewStrings.instructionViewTitleLabelText),
                    subtitleLabelText = strings.value(AuthCodePageViewStrings.instructionViewSubtitleLabelText),
                ),
            viewState = ViewState.Loaded,
        )

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(AuthCodePageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    private fun navigate(route: OnboardingRoute) {
        DependencyValues.current.navigation.navigate(Route.Onboarding(route))
    }

    // MARK: - Companion

    private companion object {
        val VERIFICATION_USER_ERROR_CODES =
            setOf(
                "ERROR_INVALID_VERIFICATION_CODE",
                "ERROR_SESSION_EXPIRED",
                "ERROR_WEB_CONTEXT_CANCELLED",
            )

        const val CONTINUE_DELAY_MILLIS = 100L
        const val VERIFICATION_CODE_LENGTH = 6
    }
}
