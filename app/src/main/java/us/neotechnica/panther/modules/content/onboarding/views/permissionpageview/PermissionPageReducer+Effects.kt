//
//  PermissionPageReducer+Effects.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.permissionpageview

import us.neotechnica.panther.bundle.contactAccessDenied
import us.neotechnica.panther.bundle.navigators.OnboardingRoute
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.PermissionService
import us.neotechnica.panther.modules.common.services.sources.contacts.ContactService
import us.neotechnica.panther.modules.content.onboarding.components.instructionview.InstructionViewStrings
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Auxiliary

internal fun PermissionPageReducer.reduceRequestContactPermissionFailed(
    state: PermissionPageReducer.State,
    exception: Exception,
): ReduceResult<PermissionPageReducer.State, PermissionPageReducer.Action> {
    if (exception.isEqual(to = AppException.contactAccessDenied)) {
        return ReduceResult(
            state.copy(isContactPermissionGranted = false),
            presentCTAEffect(PermissionService.PermissionType.CONTACTS),
        )
    }

    Logger.log(exception, with = AlertType.toast)
    return ReduceResult(state.copy(isBackButtonEnabled = true, isFinishButtonEnabled = false))
}

internal fun PermissionPageReducer.reduceRequestContactPermissionReturned(
    state: PermissionPageReducer.State,
    status: PermissionService.PermissionStatus,
): ReduceResult<PermissionPageReducer.State, PermissionPageReducer.Action> {
    val isGranted = status == PermissionService.PermissionStatus.GRANTED
    if (!isGranted) {
        return ReduceResult(
            state.copy(isContactPermissionGranted = false),
            presentCTAEffect(PermissionService.PermissionType.CONTACTS),
        )
    }

    return ReduceResult(
        state.copy(isContactPermissionGranted = true),
        Effect.fireAndForget {
            try {
                DependencyValues.current.commonServices.contact
                    .syncContactPairArchive()
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        },
    )
}

internal fun PermissionPageReducer.presentCTAEffect(permissionType: PermissionService.PermissionType): Effect<PermissionPageReducer.Action> =
    Effect.task(delay = PermissionPageReducer.CTA_PRESENTATION_DELAY) {
        DependencyValues.current.commonServices.permission
            .presentCTA(permissionType)
        null
    }

internal fun PermissionPageReducer.State.withInstructionViewStrings(strings: List<TranslationOutputMap>): PermissionPageReducer.State =
    copy(
        instructionViewStrings =
            InstructionViewStrings(
                titleLabelText = strings.value(PermissionPageViewStrings.instructionViewTitleLabelText, PermissionPageViewStrings),
                subtitleLabelText = strings.value(PermissionPageViewStrings.instructionViewSubtitleLabelText, PermissionPageViewStrings),
            ),
        viewState = ViewState.Loaded,
    )

internal fun PermissionPageReducer.resolveEffect(): Effect<PermissionPageReducer.Action> =
    Effect.run { send ->
        try {
            send(
                PermissionPageReducer.Action.ResolveReturned(
                    DependencyValues.current.networking.hostedTranslation
                        .resolve(PermissionPageViewStrings),
                ),
            )
        } catch (exception: Exception) {
            send(PermissionPageReducer.Action.ResolveFailed(exception))
        }
    }
