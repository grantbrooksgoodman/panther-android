//
//  AuthCodePageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.authcodepageview

import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
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
import us.neotechnica.panther.networking.modules.auth.extensions.notReportableForAuthCodes
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer that drives the verification code entry page of the
 * onboarding flow.
 *
 * This page is the second step of phone number verification. The user
 * enters the six-digit code sent to their phone number, and the reducer
 * authenticates them using that code together with the authentication
 * identifier recorded by [OnboardingService] when the code was sent.
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings,
 *   remaining in the loading state until resolution completes. If
 *   resolution fails, the page falls back to its default strings and
 *   loads anyway.
 * - The continue button is enabled only while the entered code is
 *   exactly six characters long.
 * - Tapping continue dismisses the keyboard, then – after a brief
 *   delay – disables the page's buttons, presents a dimmed activity
 *   overlay, and begins authentication.
 * - If authentication succeeds, the reducer records the authenticated
 *   user's identifier with [OnboardingService.setUserID] and pushes
 *   the permission page.
 * - If authentication fails, the reducer removes the overlay,
 *   re-enables the buttons, and surfaces the error as a toast. Failures
 *   caused by the user – an incorrect code, an expired session, or a
 *   cancelled web context – are marked non-reportable before logging.
 */
class AuthCodePageReducer : Reducer<AuthCodePageReducer.State, AuthCodePageReducer.Action> {
    // MARK: - Dependencies

    private val navigation: Navigation by Dependency { it.navigation }
    private val onboardingService: OnboardingService by Dependency { it.onboardingService }

    // MARK: - Actions

    /** The actions the verification code entry page can process. */
    sealed interface Action {
        /** An action that indicates the view appeared. Begins display string resolution. */
        data object ViewAppeared : Action

        /** An action that indicates the user tapped the back button. Pops the current page. */
        data object BackButtonTapped : Action

        /**
         * An action that indicates the user tapped the continue button.
         * Dismisses the keyboard, then triggers [RunContinueButtonEffect]
         * after a brief delay.
         */
        data object ContinueButtonTapped : Action

        /** An action that indicates the user swiped down on the page. Dismisses the keyboard. */
        data object DidSwipeDown : Action

        /** An action that begins authenticating the user with the entered verification code. */
        data object RunContinueButtonEffect : Action

        /** An action that indicates authentication failed, carrying the resulting [Exception]. */
        data class AuthenticateUserFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates authentication succeeded, carrying the authenticated user's identifier. */
        data class AuthenticateUserReturned(
            val userID: String,
        ) : Action

        /** An action that indicates display string resolution failed, carrying the resulting [Exception]. */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates display string resolution succeeded, carrying the resolved strings. */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        /** An action that indicates the entered verification code changed, carrying the new value. */
        data class VerificationCodeChanged(
            val verificationCode: String,
        ) : Action
    }

    // MARK: - State

    /** The state of the verification code entry page. */
    data class State(
        /** The strings the page's instruction header displays. Populated once display string resolution completes. */
        val instructionViewStrings: InstructionViewStrings = InstructionViewStrings.empty,
        /** A Boolean value that indicates whether the back button is enabled. Disabled while authentication is in progress. */
        val isBackButtonEnabled: Boolean = true,
        /** A Boolean value that indicates whether the continue button is enabled. */
        val isContinueButtonEnabled: Boolean = false,
        /** The page's translated display strings. Contains the default, untranslated strings until resolution completes. */
        val strings: List<TranslationOutputMap> = AuthCodePageViewStrings.defaultOutputMap,
        /** The verification code the user has entered. */
        val verificationCode: String = "",
        /** The page's loading state. Remains loading until display string resolution completes. */
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

            is Action.AuthenticateUserFailed -> {
                Overlay.removeOverlay()

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

            is Action.AuthenticateUserReturned -> {
                Overlay.removeOverlay()

                onboardingService.setUserID(action.userID)
                navigation.navigate(Route.Onboarding(OnboardingRoute.Push(OnboardingNavigatorState.SeguePath.Permission)))
                ReduceResult(state.copy(isBackButtonEnabled = true, isContinueButtonEnabled = true))
            }

            Action.BackButtonTapped -> {
                navigation.navigate(Route.Onboarding(OnboardingRoute.Pop))
                ReduceResult(state)
            }

            Action.ContinueButtonTapped ->
                ReduceResult(
                    state,
                    Effect
                        .fireAndForget<Action> { KeyboardService.resignFirstResponders() }
                        .merge(Effect.task(delay = CONTINUE_DELAY) { Action.RunContinueButtonEffect }),
                )

            Action.DidSwipeDown ->
                ReduceResult(state, Effect.fireAndForget { KeyboardService.resignFirstResponders() })

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.withInstructionViewStrings(state.strings))
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings).withInstructionViewStrings(action.strings))

            Action.RunContinueButtonEffect -> {
                Overlay.addOverlay(alpha = OVERLAY_ALPHA, activityIndicator = Overlay.OverlayActivityIndicatorConfiguration.largeWhite)

                val verificationCode = state.verificationCode
                ReduceResult(
                    state.copy(isBackButtonEnabled = false, isContinueButtonEnabled = false),
                    Effect.task {
                        try {
                            Action.AuthenticateUserReturned(
                                Networking.config.authDelegate.authenticateUser(
                                    authID = onboardingService.authID ?: "",
                                    verificationCode = verificationCode,
                                ),
                            )
                        } catch (exception: Exception) {
                            Action.AuthenticateUserFailed(exception)
                        }
                    },
                )
            }

            is Action.VerificationCodeChanged ->
                ReduceResult(
                    state.copy(
                        verificationCode = action.verificationCode,
                        isContinueButtonEnabled = action.verificationCode.length == VERIFICATION_CODE_LENGTH,
                    ),
                )
        }

    // MARK: - Auxiliary

    private fun State.withInstructionViewStrings(strings: List<TranslationOutputMap>): State =
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

    // MARK: - Companion

    private companion object {
        val CONTINUE_DELAY = 100.milliseconds

        val VERIFICATION_USER_ERROR_CODES =
            setOf(
                "ERROR_INVALID_VERIFICATION_CODE",
                "ERROR_SESSION_EXPIRED",
                "ERROR_WEB_CONTEXT_CANCELLED",
            )

        const val OVERLAY_ALPHA = 0.5f
        const val VERIFICATION_CODE_LENGTH = 6
    }
}
