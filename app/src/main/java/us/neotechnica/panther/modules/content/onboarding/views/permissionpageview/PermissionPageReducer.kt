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
import us.neotechnica.panther.modules.content.onboarding.components.InstructionViewStrings
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * The reducer for the final onboarding permissions page.
 *
 * The user grants notification and contact permissions (both
 * optional), then finishes: agreeing to the conduct policy creates the
 * account and enters the app.
 *
 * **Note:** the runtime permission requests are launched from the view;
 * this reducer records their results. Contact-archive sync and
 * settings call-to-action prompts are deferred to a later phase.
 */
class PermissionPageReducer : Reducer<PermissionPageReducer.State, PermissionPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object BackButtonTapped : Action

        data object FinishButtonTapped : Action

        data class RequestContactPermissionReturned(
            val isGranted: Boolean,
        ) : Action

        data class RequestNotificationPermissionReturned(
            val isGranted: Boolean,
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

            is Action.RequestContactPermissionReturned ->
                ReduceResult(state.withPermission(contactGranted = action.isGranted))

            is Action.RequestNotificationPermissionReturned ->
                ReduceResult(state.withPermission(notificationGranted = action.isGranted))

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
                    Logger.log(exception)
                    ReduceResult(state.copy(isBackButtonEnabled = true, isFinishButtonEnabled = true))
                } else {
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

    private fun State.withPermission(
        contactGranted: Boolean? = isContactPermissionGranted,
        notificationGranted: Boolean? = isNotificationPermissionGranted,
    ): State {
        val updated =
            copy(
                isContactPermissionGranted = contactGranted,
                isNotificationPermissionGranted = notificationGranted,
            )
        return updated.copy(
            isFinishButtonEnabled =
                updated.isContactPermissionGranted != null && updated.isNotificationPermissionGranted != null,
        )
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
}

/** The translated label strings for the permissions page. */
object PermissionPageViewStrings : TranslatedLabelStrings {
    val backButtonText = TranslatedLabelStringCollection("permissionPageView.backButtonText")
    val finishButtonText = TranslatedLabelStringCollection("permissionPageView.finishButtonText")
    val contactPermissionCapsuleButtonText =
        TranslatedLabelStringCollection("permissionPageView.contactPermissionCapsuleButtonText")
    val notificationPermissionCapsuleButtonText =
        TranslatedLabelStringCollection("permissionPageView.notificationPermissionCapsuleButtonText")
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("permissionPageView.instructionViewSubtitleLabelText")
    val instructionViewTitleLabelText =
        TranslatedLabelStringCollection("permissionPageView.instructionViewTitleLabelText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(backButtonText, TranslationInput("Back", alternate = "Go back")),
            TranslationInputMap(finishButtonText, TranslationInput("Finish")),
            TranslationInputMap(
                contactPermissionCapsuleButtonText,
                TranslationInput("Tap to allow contact access"),
            ),
            TranslationInputMap(
                notificationPermissionCapsuleButtonText,
                TranslationInput("Tap to allow notifications"),
            ),
            TranslationInputMap(
                instructionViewSubtitleLabelText,
                TranslationInput(
                    "Finally, grant Hello the necessary permissions to work with your device.\n\n" +
                        "These options can be changed later in Settings.",
                ),
            ),
            TranslationInputMap(instructionViewTitleLabelText, TranslationInput("Grant Permissions")),
        )
}
