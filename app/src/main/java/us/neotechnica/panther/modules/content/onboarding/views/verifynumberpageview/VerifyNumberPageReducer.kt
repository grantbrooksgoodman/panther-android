//
//  VerifyNumberPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.verifynumberpageview

import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.extensions.partiallyFormatted
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.onboarding.components.InstructionViewStrings
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.auth.extensions.notReportableForAuthCodes
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.Translator
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer that drives the phone number entry page of the
 * onboarding flow.
 *
 * This page is the first step of phone number verification. The user
 * enters their phone number, and the reducer checks whether an account
 * is already registered with it before sending a verification code.
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings,
 *   remaining in the loading state until resolution completes. If
 *   resolution fails, the page falls back to its default strings and
 *   loads anyway. The page restores any phone number and region
 *   previously recorded by [OnboardingService], defaulting to the
 *   device's region.
 * - The continue button is enabled only while the entered phone number
 *   is a valid length for the selected region's calling code.
 * - Tapping continue dismisses the keyboard, then – after a brief
 *   delay – disables the page's buttons, presents a dimmed activity
 *   overlay, and begins the account existence check.
 * - If an account is already registered with the phone number, the
 *   reducer offers to sign the user in instead. If the user accepts,
 *   the reducer records the phone number and region with
 *   [OnboardingService] and replaces the navigation stack with the
 *   sign-in page.
 * - Otherwise, the reducer sends a verification code to the phone
 *   number. When the code is sent, the reducer records the issued
 *   identifier, phone number, and region with [OnboardingService] and
 *   pushes the verification code entry page.
 * - If verification fails, the reducer removes the overlay, re-enables
 *   the buttons, and surfaces the error as a toast. Failures caused by
 *   the user – an invalid phone number, an expired session, or a
 *   cancelled web context – are marked non-reportable before logging.
 */
class VerifyNumberPageReducer : Reducer<VerifyNumberPageReducer.State, VerifyNumberPageReducer.Action> {
    // MARK: - Dependencies

    private val navigation: Navigation by Dependency { it.navigation }
    private val onboardingService: OnboardingService by Dependency { it.onboardingService }

    // MARK: - Actions

    /** The actions the phone number entry page can process. */
    sealed interface Action {
        /** An action that indicates the view appeared. Restores any previously recorded phone number and region, then begins display string resolution. */
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

        /** An action that begins checking whether an account exists for the entered phone number. */
        data object RunContinueButtonEffect : Action

        /** An action that indicates the account exists alert was dismissed, carrying whether the user selected the cancel option. */
        data class AccountExistsAlertDismissed(
            val cancelled: Boolean,
        ) : Action

        /** An action that indicates the account existence check resolved, carrying whether an account is registered with the entered phone number. */
        data class AccountExistsReturned(
            val accountExists: Boolean,
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

    /** The state of the phone number entry page. */
    data class State(
        /** The strings the page's instruction header displays. Populated once display string resolution completes. */
        val instructionViewStrings: InstructionViewStrings = InstructionViewStrings.empty,
        /** A Boolean value that indicates whether the back button is enabled. Disabled while phone number verification is in progress. */
        val isBackButtonEnabled: Boolean = true,
        /** A Boolean value that indicates whether the continue button is enabled. */
        val isContinueButtonEnabled: Boolean = false,
        /** The phone number the user has entered. */
        val phoneNumberString: String = "",
        /** The code of the region the entered phone number belongs to. */
        val selectedRegionCode: String = "",
        /** The page's translated display strings. Contains the default, untranslated strings until resolution completes. */
        val strings: List<TranslationOutputMap> = VerifyNumberPageViewStrings.defaultOutputMap,
        /** The page's loading state. Remains loading until display string resolution completes. */
        val viewState: ViewState = ViewState.Loading,
    ) {
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
            Action.ViewAppeared -> {
                val selectedRegionCode = onboardingService.regionCode ?: RegionDetailService.deviceRegionCode
                val newState =
                    state.copy(
                        phoneNumberString = onboardingService.phoneNumber?.partiallyFormatted(selectedRegionCode) ?: "",
                        selectedRegionCode = selectedRegionCode,
                        viewState = ViewState.Loading,
                    )

                ReduceResult(newState.copy(isContinueButtonEnabled = newState.numberIsValidLength), resolveEffect())
            }

            is Action.AccountExistsAlertDismissed -> {
                if (!action.cancelled) {
                    onboardingService.setPhoneNumber(state.phoneNumber)
                    onboardingService.setRegionCode(state.selectedRegionCode)
                    navigation.navigate(
                        Route.Onboarding(OnboardingRoute.Stack(listOf(OnboardingNavigatorState.SeguePath.SignIn))),
                    )
                }

                ReduceResult(state.copy(isBackButtonEnabled = true, isContinueButtonEnabled = state.numberIsValidLength))
            }

            is Action.AccountExistsReturned ->
                if (action.accountExists) {
                    Overlay.removeOverlay()
                    ReduceResult(
                        state,
                        Effect.task {
                            Action.AccountExistsAlertDismissed(cancelled = onboardingService.presentAccountExistsAlert())
                        },
                    )
                } else {
                    ReduceResult(state, verifyPhoneNumberEffect(state.phoneNumber))
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

            is Action.PhoneNumberStringChanged -> {
                val newState = state.copy(phoneNumberString = action.phoneNumberString)
                ReduceResult(newState.copy(isContinueButtonEnabled = newState.numberIsValidLength))
            }

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.withInstructionViewStrings(state.strings))
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings).withInstructionViewStrings(action.strings))

            Action.RunContinueButtonEffect -> {
                Overlay.addOverlay(alpha = OVERLAY_ALPHA, activityIndicator = Overlay.OverlayActivityIndicatorConfiguration.largeWhite)

                val phoneNumber = state.phoneNumber
                ReduceResult(
                    state.copy(isBackButtonEnabled = false, isContinueButtonEnabled = false),
                    Effect.task { Action.AccountExistsReturned(UserService.accountExists(phoneNumber)) },
                )
            }

            is Action.SelectedRegionCodeChanged ->
                ReduceResult(state.copy(selectedRegionCode = action.selectedRegionCode))

            is Action.VerifyPhoneNumberFailed -> {
                Overlay.removeOverlay()

                Logger.log(
                    action.exception.notReportableForAuthCodes(PHONE_USER_ERROR_CODES),
                    with = AlertType.toast,
                )

                ReduceResult(state.copy(isBackButtonEnabled = true, isContinueButtonEnabled = state.numberIsValidLength))
            }

            is Action.VerifyPhoneNumberReturned -> {
                Overlay.removeOverlay()

                onboardingService.setAuthID(action.authID)
                onboardingService.setPhoneNumber(state.phoneNumber)
                onboardingService.setRegionCode(state.selectedRegionCode)

                navigation.navigate(Route.Onboarding(OnboardingRoute.Push(OnboardingNavigatorState.SeguePath.AuthCode)))
                ReduceResult(state.copy(isBackButtonEnabled = true, isContinueButtonEnabled = true))
            }
        }

    // MARK: - Auxiliary

    private fun State.withInstructionViewStrings(strings: List<TranslationOutputMap>): State =
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

    private companion object {
        val CONTINUE_DELAY = 100.milliseconds

        val PHONE_USER_ERROR_CODES =
            setOf(
                "ERROR_INVALID_PHONE_NUMBER",
                "ERROR_SESSION_EXPIRED",
                "ERROR_WEB_CONTEXT_CANCELLED",
            )

        const val OVERLAY_ALPHA = 0.5f
    }
}
