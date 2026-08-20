//
//  SignInPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.signinpageview

import us.neotechnica.panther.BuildConfig
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.extensions.partiallyFormatted
import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.schema.common.models.PhoneNumber
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.networking.modules.user.services.UserService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancel
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.models.TranslationInput
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer for the sign-in page, which handles both phone-number
 * entry and verification-code entry in a single page.
 *
 * Continuing from phone entry verifies an existing account and sends a
 * code; continuing from code entry authenticates and enters the app.
 * When no account exists, it offers to sign up instead.
 */
class SignInPageReducer : Reducer<SignInPageReducer.State, SignInPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object BackButtonTapped : Action

        data object ContinueButtonTapped : Action

        data object RunContinueButtonEffect : Action

        data class AccountDoesNotExistAlertDismissed(
            val cancelled: Boolean,
        ) : Action

        data class AccountExistsReturned(
            val accountExists: Boolean,
        ) : Action

        data class AuthenticateUserFailed(
            val exception: Exception,
        ) : Action

        data class AuthenticateUserReturned(
            val userID: String,
        ) : Action

        data class PhoneNumberStringChanged(
            val phoneNumberString: String,
        ) : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        data class SelectedRegionCodeChanged(
            val regionCode: String,
        ) : Action

        data class VerificationCodeChanged(
            val code: String,
        ) : Action

        data class VerifyPhoneNumberFailed(
            val exception: Exception,
        ) : Action

        data class VerifyPhoneNumberReturned(
            val authID: String,
        ) : Action
    }

    // MARK: - State

    enum class Configuration {
        PHONE_NUMBER,
        VERIFICATION_CODE,
    }

    data class State(
        val authID: String = "",
        val configuration: Configuration = Configuration.PHONE_NUMBER,
        val isBackButtonEnabled: Boolean = true,
        val isContinueButtonEnabled: Boolean = false,
        val phoneNumberString: String = "",
        val selectedRegionCode: String = "",
        val strings: List<TranslationOutputMap> = SignInPageViewStrings.defaultOutputMap,
        val verificationCode: String = "",
        val viewState: ViewState = ViewState.Loading,
    ) {
        val continueButtonText: String
            get() =
                strings.value(
                    if (configuration == Configuration.PHONE_NUMBER) {
                        SignInPageViewStrings.phoneNumberContinueButtonText
                    } else {
                        SignInPageViewStrings.verificationCodeContinueButtonText
                    },
                )

        val instructionLabelText: String
            get() =
                strings.value(
                    if (configuration == Configuration.PHONE_NUMBER) {
                        SignInPageViewStrings.phoneNumberInstructionLabelText
                    } else {
                        SignInPageViewStrings.verificationCodeInstructionLabelText
                    },
                )

        val phoneNumber: PhoneNumber
            get() =
                PhoneNumber(
                    callingCode = RegionDetailService.callingCode(selectedRegionCode) ?: PhoneNumberService.deviceCallingCode,
                    nationalNumberString = phoneNumberString.digits,
                    regionCode = selectedRegionCode,
                    label = null,
                    internalFormattedString = null,
                )

        val numberIsValidLength: Boolean
            get() = PhoneNumberService.numberIsValidLength(phoneNumberString.digits.length, phoneNumber.callingCode)
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> reduceViewAppeared(state)

            Action.BackButtonTapped ->
                when (state.configuration) {
                    Configuration.PHONE_NUMBER -> {
                        navigate(OnboardingRoute.Pop)
                        ReduceResult(state)
                    }

                    Configuration.VERIFICATION_CODE ->
                        ReduceResult(
                            state.copy(
                                configuration = Configuration.PHONE_NUMBER,
                                isContinueButtonEnabled = state.numberIsValidLength,
                            ),
                        )
                }

            Action.ContinueButtonTapped ->
                ReduceResult(
                    state,
                    Effect.task(delay = CONTINUE_DELAY_MILLIS.milliseconds) { Action.RunContinueButtonEffect },
                )

            Action.RunContinueButtonEffect -> reduceRunContinueButtonEffect(state)

            is Action.AccountExistsReturned -> reduceAccountExistsReturned(state, action)

            is Action.AccountDoesNotExistAlertDismissed -> reduceAccountDoesNotExistAlertDismissed(state, action)

            is Action.VerifyPhoneNumberReturned -> {
                Overlay.hide()
                ReduceResult(
                    state.copy(
                        authID = action.authID,
                        configuration = Configuration.VERIFICATION_CODE,
                        isBackButtonEnabled = true,
                        isContinueButtonEnabled = isDeveloperModeEnabled,
                    ),
                )
            }

            is Action.VerifyPhoneNumberFailed -> {
                Overlay.hide()
                Logger.log(action.exception)
                ReduceResult(
                    state.copy(isBackButtonEnabled = true, isContinueButtonEnabled = state.numberIsValidLength),
                )
            }

            is Action.AuthenticateUserReturned -> {
                Overlay.hide()
                Persistent.setString(PersistentStorageKey.currentUserID, action.userID)
                navigate(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash))
                ReduceResult(state)
            }

            is Action.AuthenticateUserFailed -> {
                Overlay.hide()
                Logger.log(action.exception)
                ReduceResult(
                    state.copy(
                        isBackButtonEnabled = true,
                        isContinueButtonEnabled = state.verificationCode.length == VERIFICATION_CODE_LENGTH,
                    ),
                )
            }

            is Action.PhoneNumberStringChanged -> {
                val newState = state.copy(phoneNumberString = action.phoneNumberString)
                ReduceResult(newState.copy(isContinueButtonEnabled = newState.numberIsValidLength))
            }

            is Action.SelectedRegionCodeChanged ->
                ReduceResult(state.copy(selectedRegionCode = action.regionCode))

            is Action.VerificationCodeChanged ->
                ReduceResult(
                    state.copy(
                        verificationCode = action.code,
                        isContinueButtonEnabled = action.code.length == VERIFICATION_CODE_LENGTH,
                    ),
                )

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }
        }

    // MARK: - Reduce Helpers

    private fun reduceAccountExistsReturned(
        state: State,
        action: Action.AccountExistsReturned,
    ): ReduceResult<State, Action> =
        if (action.accountExists) {
            ReduceResult(
                state,
                Effect
                    .cancel<Action>(AuthenticateUserCancelID)
                    .merge(verifyPhoneNumberEffect(state.phoneNumber).cancellable(VerifyPhoneNumberCancelID)),
            )
        } else {
            Overlay.hide()
            ReduceResult(
                state,
                Effect.run { send ->
                    send(Action.AccountDoesNotExistAlertDismissed(OnboardingService.presentAccountDoesNotExistAlert()))
                },
            )
        }

    private fun reduceAccountDoesNotExistAlertDismissed(
        state: State,
        action: Action.AccountDoesNotExistAlertDismissed,
    ): ReduceResult<State, Action> {
        Overlay.hide()
        if (action.cancelled) {
            return ReduceResult(
                state.copy(isBackButtonEnabled = true, isContinueButtonEnabled = state.numberIsValidLength),
            )
        }
        OnboardingService.setPhoneNumber(state.phoneNumber)
        OnboardingService.setRegionCode(state.selectedRegionCode)
        navigate(OnboardingRoute.Stack(listOf(OnboardingNavigatorState.SeguePath.SelectLanguage)))
        return ReduceResult(state)
    }

    private fun reduceViewAppeared(state: State): ReduceResult<State, Action> {
        val regionCode = OnboardingService.regionCode ?: RegionDetailService.deviceRegionCode
        val newState =
            if (isDeveloperModeEnabled) {
                val developerNumber =
                    PhoneNumber(
                        callingCode = DEVELOPER_CALLING_CODE,
                        nationalNumberString = DEVELOPER_NATIONAL_NUMBER,
                        regionCode = regionCode,
                        label = null,
                        internalFormattedString = null,
                    )
                state.copy(
                    selectedRegionCode = regionCode,
                    isContinueButtonEnabled = true,
                    phoneNumberString = developerNumber.partiallyFormatted(regionCode),
                    verificationCode = DEVELOPER_CODE,
                    viewState = ViewState.Loading,
                )
            } else {
                state.copy(
                    selectedRegionCode = regionCode,
                    isContinueButtonEnabled = false,
                    phoneNumberString = OnboardingService.phoneNumber?.partiallyFormatted(regionCode) ?: "",
                    viewState = ViewState.Loading,
                )
            }
        return ReduceResult(newState, resolveEffect())
    }

    private fun reduceRunContinueButtonEffect(state: State): ReduceResult<State, Action> {
        Overlay.show()
        val disabled = state.copy(isBackButtonEnabled = false, isContinueButtonEnabled = false)
        return when (state.configuration) {
            Configuration.PHONE_NUMBER -> {
                val phoneNumber = state.phoneNumber
                ReduceResult(
                    disabled,
                    Effect.run { send -> send(Action.AccountExistsReturned(UserService.accountExists(phoneNumber))) },
                )
            }

            Configuration.VERIFICATION_CODE -> {
                val authID = state.authID
                val code = state.verificationCode
                val authenticateEffect =
                    Effect
                        .run<Action> { send ->
                            try {
                                send(
                                    Action.AuthenticateUserReturned(
                                        Networking.config.authDelegate.authenticateUser(authID, code),
                                    ),
                                )
                            } catch (exception: Exception) {
                                send(Action.AuthenticateUserFailed(exception))
                            }
                        }.cancellable(AuthenticateUserCancelID)
                ReduceResult(disabled, Effect.cancel<Action>(VerifyPhoneNumberCancelID).merge(authenticateEffect))
            }
        }
    }

    // MARK: - Auxiliary

    private val isDeveloperModeEnabled: Boolean get() = BuildConfig.DEBUG

    private fun verifyPhoneNumberEffect(phoneNumber: PhoneNumber): Effect<Action> =
        Effect.run { send ->
            val activity = Translator.config.currentActivityProvider?.invoke()
            if (activity == null) {
                send(
                    Action.VerifyPhoneNumberFailed(
                        Exception("No current activity for phone verification.", metadata = ExceptionMetadata(this)),
                    ),
                )
                return@run
            }
            try {
                send(
                    Action.VerifyPhoneNumberReturned(
                        Networking.config.authDelegate.verifyPhoneNumber(
                            activity = activity,
                            internationalNumber = phoneNumber.compiledNumberString,
                            languageCode = RuntimeStorage.languageCode,
                        ),
                    ),
                )
            } catch (exception: Exception) {
                send(Action.VerifyPhoneNumberFailed(exception))
            }
        }

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(SignInPageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    private fun navigate(route: OnboardingRoute) {
        DependencyValues.current.navigation.navigate(Route.Onboarding(route))
    }

    private fun navigate(route: RootRoute) {
        DependencyValues.current.navigation.navigate(Route.Root(route))
    }

    // MARK: - Companion

    private object AuthenticateUserCancelID

    private object VerifyPhoneNumberCancelID

    private companion object {
        const val CONTINUE_DELAY_MILLIS = 100L
        const val VERIFICATION_CODE_LENGTH = 6
        const val DEVELOPER_CALLING_CODE = "1"
        const val DEVELOPER_NATIONAL_NUMBER = "5558885555"
        const val DEVELOPER_CODE = "000000"
    }
}

/** The translated label strings for the sign-in page. */
object SignInPageViewStrings : TranslatedLabelStrings {
    val backButtonText = TranslatedLabelStringCollection("signInPageView.backButtonText")
    val phoneNumberContinueButtonText = TranslatedLabelStringCollection("signInPageView.phoneNumberContinueButtonText")
    val verificationCodeContinueButtonText =
        TranslatedLabelStringCollection("signInPageView.verificationCodeContinueButtonText")
    val phoneNumberInstructionLabelText =
        TranslatedLabelStringCollection("signInPageView.phoneNumberInstructionLabelText")
    val verificationCodeInstructionLabelText =
        TranslatedLabelStringCollection("signInPageView.verificationCodeInstructionLabelText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(backButtonText, TranslationInput("Back", alternate = "Go back")),
            TranslationInputMap(phoneNumberContinueButtonText, TranslationInput("Continue")),
            TranslationInputMap(verificationCodeContinueButtonText, TranslationInput("Finish")),
            TranslationInputMap(phoneNumberInstructionLabelText, TranslationInput("Enter your phone number below:")),
            TranslationInputMap(
                verificationCodeInstructionLabelText,
                TranslationInput("Enter the code sent to your device:"),
            ),
        )
}
