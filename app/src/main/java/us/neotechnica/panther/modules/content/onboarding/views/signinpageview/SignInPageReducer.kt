//
//  SignInPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.signinpageview

import us.neotechnica.panther.bundle.currentUserID
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.extensions.partiallyFormatted
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.auth.extensions.notReportableForAuthCodes
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancel
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.Translator
import kotlin.time.Duration.Companion.milliseconds

// This reducer exceeds the file-length and type-body-length limits.

/**
 * The reducer that drives the sign-in page of the onboarding flow.
 *
 * This page performs both steps of phone number verification for
 * returning users. It begins in the phone number configuration, where
 * the user enters their phone number; once a verification code has
 * been sent, it switches to the verification code configuration, where
 * the user enters the six-digit code sent to their phone number.
 * Authenticating successfully signs the user in.
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings,
 *   remaining in the loading state until resolution completes. If
 *   resolution fails, the page falls back to its default strings and
 *   loads anyway. The page restores any phone number and region
 *   previously recorded by [OnboardingService], defaulting to the
 *   device's region. When Developer Mode is enabled, the page instead
 *   prefills a test phone number and verification code.
 * - In the phone number configuration, the continue button is enabled
 *   only while the entered phone number is a valid length for the
 *   selected region's calling code. In the verification code
 *   configuration, it is enabled only while the entered code is exactly
 *   six characters long.
 * - Tapping continue dismisses the keyboard, then – after a brief
 *   delay – disables the page's buttons, presents a dimmed activity
 *   overlay, and begins the current configuration's operation: the
 *   account existence check in the phone number configuration, or
 *   authentication in the verification code configuration. Beginning
 *   either operation cancels the other if it is still in flight.
 * - If no account is registered with the phone number, the reducer
 *   offers to sign the user up instead. If the user accepts, the
 *   reducer records the phone number and region with
 *   [OnboardingService] and replaces the navigation stack with the
 *   language selection page.
 * - Otherwise, the reducer sends a verification code to the phone
 *   number and switches to the verification code configuration.
 *   Tapping back in that configuration returns to the phone number
 *   configuration rather than popping the page.
 * - If authentication succeeds, the reducer persists the authenticated
 *   user's identifier as the signed-in user's ID, logs a sign-in
 *   analytics event, and presents the splash page. If verification or
 *   authentication fails, the reducer removes the overlay, re-enables
 *   the buttons, and surfaces the error as a toast. Failures caused by
 *   the user – an invalid phone number, an incorrect code, an expired
 *   session, or a cancelled web context – are marked non-reportable
 *   before logging.
 */
@Suppress("LargeClass")
class SignInPageReducer : Reducer<SignInPageReducer.State, SignInPageReducer.Action> {
    // MARK: - Dependencies

    private val navigation: Navigation by Dependency { it.navigation }
    private val onboardingService: OnboardingService by Dependency { it.onboardingService }

    // MARK: - Actions

    /** The actions the sign-in page can process. */
    sealed interface Action {
        /** An action that indicates the view appeared. Restores any previously recorded phone number and region, then begins display string resolution. */
        data object ViewAppeared : Action

        /**
         * An action that indicates the user tapped the back button.
         * Pops the current page, or returns to the phone number
         * configuration when entering a verification code.
         */
        data object BackButtonTapped : Action

        /**
         * An action that indicates the user tapped the continue button.
         * Dismisses the keyboard, then triggers [RunContinueButtonEffect]
         * after a brief delay.
         */
        data object ContinueButtonTapped : Action

        /** An action that indicates the user swiped down on the page. Dismisses the keyboard. */
        data object DidSwipeDown : Action

        /**
         * An action that begins the current configuration's operation:
         * the account existence check in the phone number
         * configuration, or authentication in the verification code
         * configuration.
         */
        data object RunContinueButtonEffect : Action

        /** An action that indicates the account does not exist alert was dismissed, carrying whether the user selected the cancel option. */
        data class AccountDoesNotExistAlertDismissed(
            val cancelled: Boolean,
        ) : Action

        /** An action that indicates the account existence check resolved, carrying whether an account is registered with the entered phone number. */
        data class AccountExistsReturned(
            val accountExists: Boolean,
        ) : Action

        /** An action that indicates authentication failed, carrying the resulting [Exception]. */
        data class AuthenticateUserFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates authentication succeeded, carrying the authenticated user's identifier. */
        data class AuthenticateUserReturned(
            val userID: String,
        ) : Action

        /** An action that indicates the entered phone number changed, carrying the new value. */
        data class PhoneNumberStringChanged(
            val phoneNumberString: String,
        ) : Action

        /** An action that indicates display string resolution failed, carrying the resulting [Exception]. */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates display string resolution succeeded, carrying the resolved strings. */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        /** An action that indicates the selected region changed, carrying the new region code. */
        data class SelectedRegionCodeChanged(
            val selectedRegionCode: String,
        ) : Action

        /** An action that indicates the entered verification code changed, carrying the new value. */
        data class VerificationCodeChanged(
            val verificationCode: String,
        ) : Action

        /** An action that indicates phone number verification failed, carrying the resulting [Exception]. */
        data class VerifyPhoneNumberFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates a verification code was sent to the entered phone number, carrying the identifier issued for the authentication attempt. */
        data class VerifyPhoneNumberReturned(
            val authID: String,
        ) : Action
    }

    // MARK: - State

    /** The state of the sign-in page. */
    data class State(
        /** The page's current input configuration. */
        val configuration: Configuration = Configuration.PHONE_NUMBER,
        /** A Boolean value that indicates whether the back button is enabled. Disabled while verification or authentication is in progress. */
        val isBackButtonEnabled: Boolean = true,
        /**
         * A Boolean value that indicates whether the continue button is
         * enabled. Enabled only while the current configuration's input is
         * valid and no operation is in progress.
         */
        val isContinueButtonEnabled: Boolean = false,
        /** The phone number the user has entered. */
        val phoneNumberString: String = "",
        /** The code of the region the entered phone number belongs to. */
        val selectedRegionCode: String = "",
        /** The page's translated display strings. Contains the default, untranslated strings until resolution completes. */
        val strings: List<TranslationOutputMap> = SignInPageViewStrings.defaultOutputMap,
        /** The verification code the user has entered. */
        val verificationCode: String = "",
        /** The page's loading state. Remains loading until display string resolution completes. */
        val viewState: ViewState = ViewState.Loading,
        internal val authID: String = "",
    ) {
        // MARK: - Types

        /** The input configurations the sign-in page can display. */
        enum class Configuration {
            /** The configuration in which the user enters their phone number. */
            PHONE_NUMBER,

            /** The configuration in which the user enters the verification code sent to their phone number. */
            VERIFICATION_CODE,
        }

        // MARK: - Computed Properties

        /** The continue button's title for the current configuration. */
        val continueButtonText: String
            get() =
                strings.value(
                    if (configuration == Configuration.PHONE_NUMBER) {
                        SignInPageViewStrings.phoneNumberContinueButtonText
                    } else {
                        SignInPageViewStrings.verificationCodeContinueButtonText
                    },
                )

        /** The instruction label's text for the current configuration. */
        val instructionLabelText: String
            get() =
                strings.value(
                    if (configuration == Configuration.PHONE_NUMBER) {
                        SignInPageViewStrings.phoneNumberInstructionLabelText
                    } else {
                        SignInPageViewStrings.verificationCodeInstructionLabelText
                    },
                )

        internal val isDeveloperModeEnabled: Boolean
            get() = Build.isDeveloperModeEnabled

        internal val numberIsValidLength: Boolean
            get() = PhoneNumberService.numberIsValidLength(phoneNumberString.digits.length, phoneNumber.callingCode)

        internal val phoneNumber: PhoneNumber
            get() =
                PhoneNumber(
                    callingCode = RegionDetailService.callingCode(selectedRegionCode) ?: PhoneNumberService.deviceCallingCode,
                    nationalNumberString = phoneNumberString.digits,
                    regionCode = selectedRegionCode,
                    label = null,
                    internalFormattedString = null,
                )
    }

    // MARK: - Reduce

    @Suppress("CyclomaticComplexMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> reduceViewAppeared(state)

            is Action.AccountExistsReturned -> reduceAccountExistsReturned(state, action.accountExists)

            is Action.AccountDoesNotExistAlertDismissed -> reduceAccountDoesNotExistAlertDismissed(state, action.cancelled)

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

                Persistent.setString(PersistentStorageKey.currentUserID, action.userID)
                AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.LOG_IN)
                navigation.navigate(Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash)))
                ReduceResult(state)
            }

            Action.BackButtonTapped ->
                when (state.configuration) {
                    State.Configuration.PHONE_NUMBER -> {
                        navigation.navigate(Route.Onboarding(OnboardingRoute.Pop))
                        ReduceResult(state)
                    }

                    State.Configuration.VERIFICATION_CODE ->
                        ReduceResult(
                            state.copy(
                                configuration = State.Configuration.PHONE_NUMBER,
                                isContinueButtonEnabled = state.numberIsValidLength,
                            ),
                        )
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

            is Action.PhoneNumberStringChanged -> {
                val newState = state.copy(phoneNumberString = action.phoneNumberString)
                ReduceResult(newState.copy(isContinueButtonEnabled = newState.numberIsValidLength))
            }

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))

            Action.RunContinueButtonEffect -> reduceRunContinueButtonEffect(state)

            is Action.SelectedRegionCodeChanged ->
                ReduceResult(state.copy(selectedRegionCode = action.selectedRegionCode))

            is Action.VerificationCodeChanged ->
                ReduceResult(
                    state.copy(
                        verificationCode = action.verificationCode,
                        isContinueButtonEnabled = action.verificationCode.length == VERIFICATION_CODE_LENGTH,
                    ),
                )

            is Action.VerifyPhoneNumberFailed -> {
                Overlay.removeOverlay()

                Logger.log(
                    action.exception.notReportableForAuthCodes(PHONE_USER_ERROR_CODES),
                    with = AlertType.toast,
                )

                ReduceResult(
                    state.copy(isBackButtonEnabled = true, isContinueButtonEnabled = state.numberIsValidLength),
                )
            }

            is Action.VerifyPhoneNumberReturned -> {
                Overlay.removeOverlay()

                ReduceResult(
                    state.copy(
                        authID = action.authID,
                        configuration = State.Configuration.VERIFICATION_CODE,
                        isBackButtonEnabled = true,
                        isContinueButtonEnabled = state.isDeveloperModeEnabled,
                    ),
                )
            }
        }

    // MARK: - Auxiliary

    private fun reduceAccountDoesNotExistAlertDismissed(
        state: State,
        cancelled: Boolean,
    ): ReduceResult<State, Action> {
        Overlay.removeOverlay()

        if (cancelled) {
            return ReduceResult(
                state.copy(isBackButtonEnabled = true, isContinueButtonEnabled = state.numberIsValidLength),
            )
        }

        onboardingService.setPhoneNumber(state.phoneNumber)
        onboardingService.setRegionCode(state.selectedRegionCode)
        navigation.navigate(
            Route.Onboarding(OnboardingRoute.Stack(listOf(OnboardingNavigatorState.SeguePath.SelectLanguage))),
        )

        return ReduceResult(state)
    }

    private fun reduceAccountExistsReturned(
        state: State,
        accountExists: Boolean,
    ): ReduceResult<State, Action> {
        if (accountExists) {
            val verifyPhoneNumberTask = verifyPhoneNumberEffect(state.phoneNumber).cancellable(TaskID.VERIFY_PHONE_NUMBER)
            return ReduceResult(
                state,
                Effect.cancel<Action>(TaskID.AUTHENTICATE_USER).merge(verifyPhoneNumberTask),
            )
        }

        Overlay.removeOverlay()
        return ReduceResult(
            state,
            Effect.task {
                Action.AccountDoesNotExistAlertDismissed(cancelled = onboardingService.presentAccountDoesNotExistAlert())
            },
        )
    }

    private fun reduceRunContinueButtonEffect(state: State): ReduceResult<State, Action> {
        val newState = state.copy(isBackButtonEnabled = false, isContinueButtonEnabled = false)

        Overlay.addOverlay(alpha = OVERLAY_ALPHA, activityIndicator = Overlay.OverlayActivityIndicatorConfiguration.largeWhite)

        return when (state.configuration) {
            State.Configuration.PHONE_NUMBER -> {
                val phoneNumber = state.phoneNumber
                ReduceResult(
                    newState,
                    Effect.task { Action.AccountExistsReturned(UserService.accountExists(phoneNumber)) },
                )
            }

            State.Configuration.VERIFICATION_CODE -> {
                val authID = state.authID
                val verificationCode = state.verificationCode
                val authenticateUserTask =
                    Effect
                        .task<Action> {
                            try {
                                Action.AuthenticateUserReturned(
                                    Networking.config.authDelegate.authenticateUser(
                                        authID = authID,
                                        verificationCode = verificationCode,
                                    ),
                                )
                            } catch (exception: Exception) {
                                Action.AuthenticateUserFailed(exception)
                            }
                        }.cancellable(TaskID.AUTHENTICATE_USER)

                ReduceResult(
                    newState,
                    Effect.cancel<Action>(TaskID.VERIFY_PHONE_NUMBER).merge(authenticateUserTask),
                )
            }
        }
    }

    private fun reduceViewAppeared(state: State): ReduceResult<State, Action> {
        val selectedRegionCode = onboardingService.regionCode ?: RegionDetailService.deviceRegionCode

        val newState =
            if (state.isDeveloperModeEnabled) {
                state.copy(
                    isContinueButtonEnabled = true,
                    phoneNumberString = PhoneNumber(DEVELOPER_PHONE_NUMBER_STRING).partiallyFormatted(selectedRegionCode),
                    selectedRegionCode = selectedRegionCode,
                    verificationCode = DEVELOPER_VERIFICATION_CODE,
                    viewState = ViewState.Loading,
                )
            } else {
                state.copy(
                    isContinueButtonEnabled = false,
                    phoneNumberString = onboardingService.phoneNumber?.partiallyFormatted(selectedRegionCode) ?: "",
                    selectedRegionCode = selectedRegionCode,
                    viewState = ViewState.Loading,
                )
            }

        return ReduceResult(newState, resolveEffect())
    }

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(SignInPageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    private fun verifyPhoneNumberEffect(phoneNumber: PhoneNumber): Effect<Action> =
        Effect.task {
            // Phone verification needs the foreground activity to host
            // reCAPTCHA or app-check challenges.
            val activity =
                Translator.config.currentActivityProvider?.invoke()
                    ?: return@task Action.VerifyPhoneNumberFailed(
                        Exception(
                            "No current activity for phone verification.",
                            isReportable = false,
                            metadata = ExceptionMetadata(this),
                        ),
                    )

            try {
                Action.VerifyPhoneNumberReturned(
                    Networking.config.authDelegate.verifyPhoneNumber(
                        activity = activity,
                        internationalNumber = phoneNumber.compiledNumberString,
                        languageCode = RuntimeStorage.languageCode,
                    ),
                )
            } catch (exception: Exception) {
                Action.VerifyPhoneNumberFailed(exception)
            }
        }

    // MARK: - Companion

    private enum class TaskID {
        AUTHENTICATE_USER,
        VERIFY_PHONE_NUMBER,
    }

    private companion object {
        val CONTINUE_DELAY = 100.milliseconds

        val PHONE_USER_ERROR_CODES =
            setOf(
                "ERROR_INVALID_PHONE_NUMBER",
                "ERROR_SESSION_EXPIRED",
                "ERROR_WEB_CONTEXT_CANCELLED",
            )

        val VERIFICATION_USER_ERROR_CODES =
            setOf(
                "ERROR_INVALID_VERIFICATION_CODE",
                "ERROR_SESSION_EXPIRED",
                "ERROR_WEB_CONTEXT_CANCELLED",
            )

        const val DEVELOPER_PHONE_NUMBER_STRING = "15558885555"
        const val DEVELOPER_VERIFICATION_CODE = "000000"
        const val OVERLAY_ALPHA = 0.5f
        const val VERIFICATION_CODE_LENGTH = 6
    }
}
