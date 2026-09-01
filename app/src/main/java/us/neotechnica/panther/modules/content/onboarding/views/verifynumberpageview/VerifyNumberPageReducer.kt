//
//  VerifyNumberPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.verifynumberpageview

import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.extensions.partiallyFormatted
import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.onboarding.models.InstructionViewStrings
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.auth.extensions.notReportableForAuthCodes
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
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.models.TranslationInput
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer for the sign-up phone-number entry page.
 *
 * Continuing checks whether an account already exists for the number:
 * if so, it offers to sign in; otherwise it sends a verification code
 * and advances to the code-entry page.
 */
class VerifyNumberPageReducer : Reducer<VerifyNumberPageReducer.State, VerifyNumberPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object BackButtonTapped : Action

        data object ContinueButtonTapped : Action

        data object RunContinueButtonEffect : Action

        /** Debug-only: skips verification and advances to the code page. */
        data object DebugForceContinueTapped : Action

        data class AccountExistsAlertDismissed(
            val cancelled: Boolean,
        ) : Action

        data class AccountExistsReturned(
            val accountExists: Boolean,
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

        data class VerifyPhoneNumberFailed(
            val exception: Exception,
        ) : Action

        data class VerifyPhoneNumberReturned(
            val authID: String,
        ) : Action
    }

    // MARK: - State

    data class State(
        val hasError: Boolean = false,
        val instructionViewStrings: InstructionViewStrings = InstructionViewStrings.empty,
        val isBackButtonEnabled: Boolean = true,
        val isContinueButtonEnabled: Boolean = false,
        val phoneNumberString: String = "",
        val selectedRegionCode: String = "",
        val strings: List<TranslationOutputMap> = VerifyNumberPageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
    ) {
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

    @Suppress("CyclomaticComplexMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                val regionCode = OnboardingService.regionCode ?: RegionDetailService.deviceRegionCode
                val newState =
                    state.copy(
                        selectedRegionCode = regionCode,
                        phoneNumberString = OnboardingService.phoneNumber?.partiallyFormatted(regionCode) ?: "",
                        viewState = ViewState.Loading,
                    )
                ReduceResult(newState.copy(isContinueButtonEnabled = newState.numberIsValidLength), resolveEffect())
            }

            Action.BackButtonTapped -> {
                navigate(OnboardingRoute.Pop)
                ReduceResult(state)
            }

            Action.ContinueButtonTapped ->
                ReduceResult(
                    state.copy(hasError = false),
                    Effect.task(delay = CONTINUE_DELAY_MILLIS.milliseconds) { Action.RunContinueButtonEffect },
                )

            Action.DebugForceContinueTapped -> {
                OnboardingService.setPhoneNumber(state.phoneNumber)
                OnboardingService.setRegionCode(state.selectedRegionCode)
                navigate(OnboardingRoute.Push(OnboardingNavigatorState.SeguePath.AuthCode))
                ReduceResult(state.copy(hasError = false))
            }

            Action.RunContinueButtonEffect -> {
                Overlay.show()
                val phoneNumber = state.phoneNumber
                ReduceResult(
                    state.copy(isBackButtonEnabled = false, isContinueButtonEnabled = false),
                    Effect.run { send -> send(Action.AccountExistsReturned(UserService.accountExists(phoneNumber))) },
                )
            }

            is Action.AccountExistsReturned ->
                if (action.accountExists) {
                    Overlay.hide()
                    ReduceResult(
                        state,
                        Effect.run { send ->
                            send(Action.AccountExistsAlertDismissed(OnboardingService.presentAccountExistsAlert()))
                        },
                    )
                } else {
                    ReduceResult(state, verifyPhoneNumberEffect(state.phoneNumber))
                }

            is Action.AccountExistsAlertDismissed -> {
                val newState =
                    state.copy(
                        isBackButtonEnabled = true,
                        isContinueButtonEnabled = state.numberIsValidLength,
                    )
                if (!action.cancelled) {
                    OnboardingService.setPhoneNumber(state.phoneNumber)
                    OnboardingService.setRegionCode(state.selectedRegionCode)
                    navigate(OnboardingRoute.Stack(listOf(OnboardingNavigatorState.SeguePath.SignIn)))
                }
                ReduceResult(newState)
            }

            is Action.VerifyPhoneNumberReturned -> {
                Overlay.hide()
                OnboardingService.setAuthID(action.authID)
                OnboardingService.setPhoneNumber(state.phoneNumber)
                OnboardingService.setRegionCode(state.selectedRegionCode)
                navigate(OnboardingRoute.Push(OnboardingNavigatorState.SeguePath.AuthCode))
                ReduceResult(state.copy(isBackButtonEnabled = true, isContinueButtonEnabled = true))
            }

            is Action.VerifyPhoneNumberFailed -> {
                Overlay.hide()
                Logger.log(
                    action.exception.notReportableForAuthCodes(PHONE_USER_ERROR_CODES),
                    with = AlertType.toast,
                )
                ReduceResult(
                    state.copy(
                        hasError = true,
                        isBackButtonEnabled = true,
                        isContinueButtonEnabled = state.numberIsValidLength,
                    ),
                )
            }

            is Action.PhoneNumberStringChanged -> {
                val newState = state.copy(phoneNumberString = action.phoneNumberString)
                ReduceResult(newState.copy(isContinueButtonEnabled = newState.numberIsValidLength))
            }

            is Action.SelectedRegionCodeChanged ->
                ReduceResult(state.copy(selectedRegionCode = action.regionCode))

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings).withResolvedInstruction(action.strings))

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.withResolvedInstruction(state.strings))
            }
        }

    // MARK: - Auxiliary

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

    private fun State.withResolvedInstruction(strings: List<TranslationOutputMap>): State =
        copy(
            instructionViewStrings =
                InstructionViewStrings(
                    titleLabelText = strings.value(VerifyNumberPageViewStrings.instructionViewTitleLabelText),
                    subtitleLabelText = strings.value(VerifyNumberPageViewStrings.instructionViewSubtitleLabelText),
                ),
            viewState = ViewState.Loaded,
        )

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(
                    Action.ResolveReturned(
                        Networking.config.hostedTranslationDelegate.resolve(VerifyNumberPageViewStrings),
                    ),
                )
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    private fun navigate(route: OnboardingRoute) {
        DependencyValues.current.navigation.navigate(Route.Onboarding(route))
    }

    // MARK: - Companion

    private companion object {
        val PHONE_USER_ERROR_CODES =
            setOf(
                "ERROR_INVALID_PHONE_NUMBER",
                "ERROR_SESSION_EXPIRED",
                "ERROR_WEB_CONTEXT_CANCELLED",
            )

        const val CONTINUE_DELAY_MILLIS = 100L
    }
}

/** The translated label strings for the phone-number entry page. */
object VerifyNumberPageViewStrings : TranslatedLabelStrings {
    val backButtonText = TranslatedLabelStringCollection("verifyNumberPageView.backButtonText")
    val continueButtonText = TranslatedLabelStringCollection("verifyNumberPageView.continueButtonText")
    val instructionLabelText = TranslatedLabelStringCollection("verifyNumberPageView.instructionLabelText")
    val instructionViewTitleLabelText =
        TranslatedLabelStringCollection("verifyNumberPageView.instructionViewTitleLabelText")
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("verifyNumberPageView.instructionViewSubtitleLabelText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(backButtonText, TranslationInput("Back", alternate = "Go back")),
            TranslationInputMap(continueButtonText, TranslationInput("Continue")),
            TranslationInputMap(instructionLabelText, TranslationInput("Enter your phone number below:")),
            TranslationInputMap(instructionViewTitleLabelText, TranslationInput("Enter Phone Number")),
            TranslationInputMap(
                instructionViewSubtitleLabelText,
                TranslationInput(
                    "Next, enter your phone number.\n\nA verification code will be sent to your number. " +
                        "Standard messaging rates apply.",
                ),
            ),
        )
}
