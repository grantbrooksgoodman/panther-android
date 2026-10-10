//
//  PermissionPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.permissionpageview

import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.contactAccessDenied
import us.neotechnica.panther.modules.common.services.PermissionService
import us.neotechnica.panther.modules.content.onboarding.components.InstructionViewStrings
import us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService
import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer that drives the permission request page of the
 * onboarding flow.
 *
 * This page is the final step of sign-up. The user requests the contact
 * and notification permissions, agrees to the app's conduct policy,
 * and the reducer finalizes account creation with
 * [OnboardingService.createUser].
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings,
 *   remaining in the loading state until resolution completes. If
 *   resolution fails, the page falls back to its default strings and
 *   loads anyway.
 * - Each permission button begins a system permission request and
 *   records the result as a tri-state value: `null` until the request
 *   resolves, then `true` or `false`. When a permission is not granted,
 *   the reducer presents a call to action for it after a brief delay.
 * - When the contact permission is granted, the reducer synchronizes
 *   the contact pair archive. Notification delivery registers itself
 *   once the notification permission is granted.
 * - The finish button is enabled once the status of both permissions
 *   has been determined. Tapping it disables the page's buttons,
 *   presents a dimmed activity overlay, and presents the conduct policy
 *   sheet.
 * - If the user agrees to the conduct policy, the reducer creates the
 *   account. If creation succeeds, the reducer presents the splash
 *   page; otherwise, it removes the overlay, re-enables the buttons,
 *   and surfaces the error as a toast. Declining the policy restores
 *   the page without creating an account.
 */
class PermissionPageReducer : Reducer<PermissionPageReducer.State, PermissionPageReducer.Action> {
    // MARK: - Dependencies

    private val navigation: Navigation by Dependency { it.navigation }
    private val onboardingService: OnboardingService by Dependency { it.onboardingService }

    // MARK: - Actions

    /** The actions the permission page can process. */
    sealed interface Action {
        /** An action that indicates the view appeared. Begins display string resolution. */
        data object ViewAppeared : Action

        /** An action that indicates the user tapped the back button. Pops the current page. */
        data object BackButtonTapped : Action

        /** An action that indicates the user tapped the contact permission button. Begins a contact permission request. */
        data object ContactPermissionCapsuleButtonTapped : Action

        /** An action that indicates the user tapped the finish button. Presents the conduct policy sheet. */
        data object FinishButtonTapped : Action

        /** An action that indicates the user tapped the notification permission button. Begins a notification permission request. */
        data object NotificationPermissionCapsuleButtonTapped : Action

        /** An action that indicates account creation finished, carrying `null` if the operation succeeded; otherwise, the resulting [Exception]. */
        data class CreateUserReturned(
            val exception: Exception?,
        ) : Action

        /** An action that indicates the conduct policy sheet was dismissed, carrying whether the user declined the agreement. */
        data class EulaAlertDismissed(
            val cancelled: Boolean,
        ) : Action

        /** An action that indicates the contact permission request failed, carrying the resulting [Exception]. */
        data class RequestContactPermissionFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates the contact permission request resolved, carrying the resulting status. */
        data class RequestContactPermissionReturned(
            val status: PermissionService.PermissionStatus,
        ) : Action

        /** An action that indicates the notification permission request failed, carrying the resulting [Exception]. */
        data class RequestNotificationPermissionFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates the notification permission request resolved, carrying the resulting status. */
        data class RequestNotificationPermissionReturned(
            val status: PermissionService.PermissionStatus,
        ) : Action

        /** An action that indicates display string resolution failed, carrying the resulting [Exception]. */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates display string resolution succeeded, carrying the resolved strings. */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action
    }

    // MARK: - State

    /** The state of the permission page. */
    data class State(
        /** The strings the page's instruction header displays. Populated once display string resolution completes. */
        val instructionViewStrings: InstructionViewStrings = InstructionViewStrings.empty,
        /** A Boolean value that indicates whether the back button is enabled. Disabled while account creation is in progress. */
        val isBackButtonEnabled: Boolean = true,
        /** A Boolean value that indicates whether the contact permission was granted, or `null` if its request has not yet resolved. */
        val isContactPermissionGranted: Boolean? = null,
        /** A Boolean value that indicates whether the finish button is enabled. */
        val isFinishButtonEnabled: Boolean = false,
        /** A Boolean value that indicates whether the notification permission was granted, or `null` if its request has not yet resolved. */
        val isNotificationPermissionGranted: Boolean? = null,
        /** The page's translated display strings. Contains the default, untranslated strings until resolution completes. */
        val strings: List<TranslationOutputMap> = PermissionPageViewStrings.defaultOutputMap,
        /** The page's loading state. Remains loading until display string resolution completes. */
        val viewState: ViewState = ViewState.Loading,
    )

    // MARK: - Reduce

    @Suppress("CyclomaticComplexMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared ->
                ReduceResult(state.copy(viewState = ViewState.Loading), resolveEffect())

            Action.BackButtonTapped -> {
                navigation.navigate(Route.Onboarding(OnboardingRoute.Pop))
                ReduceResult(state)
            }

            Action.ContactPermissionCapsuleButtonTapped ->
                ReduceResult(
                    state.copy(isFinishButtonEnabled = state.isNotificationPermissionGranted != null),
                    Effect.task {
                        try {
                            Action.RequestContactPermissionReturned(
                                PermissionService.requestPermission(PermissionService.PermissionType.CONTACTS),
                            )
                        } catch (exception: Exception) {
                            Action.RequestContactPermissionFailed(exception)
                        }
                    },
                )

            is Action.CreateUserReturned -> {
                Overlay.removeOverlay()

                val exception = action.exception
                if (exception != null) {
                    Logger.log(exception, with = AlertType.toast)
                    ReduceResult(state.copy(isBackButtonEnabled = true, isFinishButtonEnabled = true))
                } else {
                    navigation.navigate(Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash)))
                    ReduceResult(state)
                }
            }

            is Action.EulaAlertDismissed ->
                if (action.cancelled) {
                    Overlay.removeOverlay()
                    ReduceResult(state.copy(isBackButtonEnabled = true, isFinishButtonEnabled = true))
                } else {
                    ReduceResult(
                        state,
                        Effect.task {
                            try {
                                onboardingService.createUser()
                                Action.CreateUserReturned(null)
                            } catch (exception: Exception) {
                                Action.CreateUserReturned(exception)
                            }
                        },
                    )
                }

            Action.FinishButtonTapped -> {
                // Non-modal so the conduct policy sheet stays interactive.
                Overlay.addOverlay(
                    alpha = OVERLAY_ALPHA,
                    activityIndicator = Overlay.OverlayActivityIndicatorConfiguration.largeWhite,
                    isModal = false,
                )

                ReduceResult(
                    state.copy(isBackButtonEnabled = false, isFinishButtonEnabled = false),
                    Effect.task { Action.EulaAlertDismissed(cancelled = onboardingService.presentEULAAlert()) },
                )
            }

            Action.NotificationPermissionCapsuleButtonTapped ->
                ReduceResult(
                    state.copy(isFinishButtonEnabled = state.isContactPermissionGranted != null),
                    Effect.task {
                        try {
                            Action.RequestNotificationPermissionReturned(
                                PermissionService.requestPermission(PermissionService.PermissionType.NOTIFICATIONS),
                            )
                        } catch (exception: Exception) {
                            Action.RequestNotificationPermissionFailed(exception)
                        }
                    },
                )

            is Action.RequestContactPermissionFailed -> reduceRequestContactPermissionFailed(state, action.exception)

            is Action.RequestContactPermissionReturned -> reduceRequestContactPermissionReturned(state, action.status)

            is Action.RequestNotificationPermissionFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(isBackButtonEnabled = true, isFinishButtonEnabled = false))
            }

            is Action.RequestNotificationPermissionReturned -> {
                val isGranted = action.status == PermissionService.PermissionStatus.GRANTED
                if (isGranted) {
                    // Notification delivery registers automatically once granted.
                    ReduceResult(state.copy(isNotificationPermissionGranted = true))
                } else {
                    ReduceResult(
                        state.copy(isNotificationPermissionGranted = false),
                        presentCTAEffect(PermissionService.PermissionType.NOTIFICATIONS),
                    )
                }
            }

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.withInstructionViewStrings(state.strings))
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings).withInstructionViewStrings(action.strings))
        }

    // MARK: - Auxiliary

    private fun reduceRequestContactPermissionFailed(
        state: State,
        exception: Exception,
    ): ReduceResult<State, Action> {
        if (exception.isEqual(to = AppException.contactAccessDenied)) {
            return ReduceResult(
                state.copy(isContactPermissionGranted = false),
                presentCTAEffect(PermissionService.PermissionType.CONTACTS),
            )
        }

        Logger.log(exception, with = AlertType.toast)
        return ReduceResult(state.copy(isBackButtonEnabled = true, isFinishButtonEnabled = false))
    }

    private fun reduceRequestContactPermissionReturned(
        state: State,
        status: PermissionService.PermissionStatus,
    ): ReduceResult<State, Action> {
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
                    ContactService.syncContactPairArchive()
                } catch (exception: Exception) {
                    Logger.log(exception)
                }
            },
        )
    }

    private fun presentCTAEffect(permissionType: PermissionService.PermissionType): Effect<Action> =
        Effect.task(delay = CTA_PRESENTATION_DELAY) {
            PermissionService.presentCTA(permissionType)
            null
        }

    private fun State.withInstructionViewStrings(strings: List<TranslationOutputMap>): State =
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
        val CTA_PRESENTATION_DELAY = 500.milliseconds

        const val OVERLAY_ALPHA = 0.5f
    }
}
