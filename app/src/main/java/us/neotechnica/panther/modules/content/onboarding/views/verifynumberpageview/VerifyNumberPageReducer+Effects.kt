//
//  VerifyNumberPageReducer+Effects.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.verifynumberpageview

import us.neotechnica.panther.bundle.navigators.OnboardingNavigatorState
import us.neotechnica.panther.bundle.navigators.OnboardingRoute
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.PhoneNumberService
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.modules.content.onboarding.components.instructionview.InstructionViewStrings
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.modules.content.onboarding.extensions.verifyPhoneNumber
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.modules.networking.sources.user.services.UserService
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.auth.extensions.notReportableForAuthCodes
import us.neotechnica.panther.networking.modules.auth.interfaces.AuthDelegate
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Auxiliary

internal fun VerifyNumberPageReducer.State.withInstructionViewStrings(strings: List<TranslationOutputMap>): VerifyNumberPageReducer.State =
    copy(
        instructionViewStrings =
            InstructionViewStrings(
                titleLabelText = strings.value(VerifyNumberPageViewStrings.instructionViewTitleLabelText, VerifyNumberPageViewStrings),
                subtitleLabelText = strings.value(VerifyNumberPageViewStrings.instructionViewSubtitleLabelText, VerifyNumberPageViewStrings),
            ),
        viewState = ViewState.Loaded,
    )

internal fun VerifyNumberPageReducer.resolveEffect(): Effect<VerifyNumberPageReducer.Action> =
    Effect.run { send ->
        try {
            send(
                VerifyNumberPageReducer.Action.ResolveReturned(
                    DependencyValues.current.networking.hostedTranslation
                        .resolve(VerifyNumberPageViewStrings),
                ),
            )
        } catch (exception: Exception) {
            send(VerifyNumberPageReducer.Action.ResolveFailed(exception))
        }
    }

internal fun VerifyNumberPageReducer.verifyPhoneNumberEffect(phoneNumber: PhoneNumber): Effect<VerifyNumberPageReducer.Action> =
    Effect.verifyPhoneNumber(
        phoneNumber,
        sender = this,
        returned = VerifyNumberPageReducer.Action::VerifyPhoneNumberReturned,
        failed = VerifyNumberPageReducer.Action::VerifyPhoneNumberFailed,
    )
