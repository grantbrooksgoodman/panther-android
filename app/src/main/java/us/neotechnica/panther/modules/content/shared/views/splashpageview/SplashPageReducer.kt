//
//  SplashPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.views.splashpageview

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import us.neotechnica.panther.bundle.clientSession
import us.neotechnica.panther.bundle.failedToGenerateMediaFile
import us.neotechnica.panther.bundle.navigators.OnboardingRoute
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.bundle.timedOut
import us.neotechnica.panther.modules.content.shared.dependencies.splashPageViewService
import us.neotechnica.panther.modules.content.shared.services.SplashPageViewService
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.PendingChatNavigation
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * The reducer that drives the splash page.
 *
 * The splash page is presented at launch and after a completed sign-in
 * or sign-up. It initializes the app's data bundle through
 * [SplashPageViewService] and routes the user to the appropriate
 * destination when initialization settles.
 *
 * The page's behavior contract:
 *
 * - On appearance, the page begins bundle initialization, racing
 *   [SplashPageViewService.initializeBundle] against
 *   [SplashPageViewService.resolveCachedUserIfPoorNetwork]; whichever
 *   settles first determines how the app loads, and the other is
 *   cancelled.
 * - Each network activity event nudges the initialization progress
 *   forward until it approaches completion.
 * - If initialization succeeds, the page presents the user content
 *   when a signed-in user resolved; otherwise, it presents onboarding
 *   with an empty navigation stack.
 * - If initialization fails, the first failure attempts recovery
 *   automatically; subsequent failures present an error alert, and
 *   dismissing it retries. In both cases, failures a database repair
 *   cannot address – a timeout or a media file generation failure –
 *   retry initialization directly, while all other failures run
 *   [SplashPageViewService.performRetryHandler] before retrying.
 */
class SplashPageReducer : Reducer<SplashPageReducer.State, SplashPageReducer.Action> {
    // MARK: - Dependencies

    private val navigation: Navigation by Dependency { it.navigation }
    private val userSession: UserSessionService by Dependency { it.clientSession.entity.user }
    private val viewService: SplashPageViewService by Dependency { it.splashPageViewService }

    // MARK: - Actions

    /** The actions the splash page can process. */
    sealed interface Action {
        /** An action that indicates the view appeared. Begins bundle initialization. */
        data object ViewAppeared : Action

        /**
         * An action that indicates network activity occurred. Nudges
         * the initialization progress forward until it approaches
         * completion.
         */
        data object BundleInitializationProgressOccurred : Action

        /**
         * An action that indicates the error alert was dismissed.
         * Retries initialization, running the recovery handler first
         * when the failure warrants it.
         */
        data object ErrorAlertDismissed : Action

        /**
         * An action that indicates bundle initialization finished,
         * carrying `null` if the operation succeeded; otherwise, the
         * resulting [Exception].
         */
        data class InitializedBundle(
            val exception: Exception?,
        ) : Action

        /**
         * An action that indicates the recovery attempt finished,
         * carrying `null` if the operation succeeded; otherwise, the
         * resulting [Exception]. Retries initialization.
         */
        data class PerformRetryHandlerReturned(
            val exception: Exception?,
        ) : Action
    }

    // MARK: - State

    /**
     * The state of the splash page.
     *
     * @property didAttemptAutomaticErrorRecovery A Boolean value that
     *   indicates whether initialization has already been retried
     *   automatically after a failure.
     * @property exception The initialization failure, or `null` if
     *   none has occurred.
     */
    data class State(
        val didAttemptAutomaticErrorRecovery: Boolean = false,
        val exception: Exception? = null,
    )

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared ->
                ReduceResult(
                    state.copy(didAttemptAutomaticErrorRecovery = false),
                    initializeBundleTask(),
                )

            Action.BundleInitializationProgressOccurred -> {
                if (viewService.initializationProgress.value < PROGRESS_NUDGE_CEILING) {
                    viewService.setInitializationProgress(viewService.initializationProgress.value + PROGRESS_NUDGE_INCREMENT)
                }

                ReduceResult(state)
            }

            Action.ErrorAlertDismissed -> {
                val exception = state.exception
                if (exception == null ||
                    exception.isEqual(toAny = listOf(AppException.failedToGenerateMediaFile, AppException.timedOut))
                ) {
                    ReduceResult(state, initializeBundleTask(fromRetry = true))
                } else {
                    ReduceResult(state, performRetryHandlerTask())
                }
            }

            is Action.InitializedBundle -> reduceInitializedBundle(state, action.exception)

            is Action.PerformRetryHandlerReturned -> {
                action.exception?.let { Logger.log(it) }
                ReduceResult(state, initializeBundleTask(fromRetry = true))
            }
        }

    // MARK: - Auxiliary

    private fun reduceInitializedBundle(
        state: State,
        exception: Exception?,
    ): ReduceResult<State, Action> {
        val newState = state.copy(exception = exception)

        if (exception == null) {
            navigateAfterInitialization()
            return ReduceResult(newState)
        }

        if (!newState.didAttemptAutomaticErrorRecovery) {
            Logger.log("Attempting automatic error recovery.", domain = LoggerDomain.general)

            val effect =
                if (exception.isEqual(toAny = listOf(AppException.failedToGenerateMediaFile, AppException.timedOut))) {
                    initializeBundleTask(fromRetry = true)
                } else {
                    performRetryHandlerTask()
                }

            Logger.log(exception)
            return ReduceResult(newState.copy(didAttemptAutomaticErrorRecovery = true), effect)
        }

        Logger.log(exception)
        return ReduceResult(
            newState,
            Effect.run { send ->
                viewService.presentErrorAlert(exception)
                send(Action.ErrorAlertDismissed)
            },
        )
    }

    private fun navigateAfterInitialization() {
        if (User.currentUserID != null && userSession.currentUser != null) {
            navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.UserContent)))

            // Open a conversation deep-linked from a tapped push notification.
            PendingChatNavigation.consume()?.let { conversationIDKey ->
                navigation.navigate(
                    Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePaths.Chat(conversationIDKey))),
                )
            }
        } else {
            navigation.navigate(Route.Onboarding(OnboardingRoute.Stack(emptyList())))
            navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Onboarding)))
        }
    }

    private fun initializeBundleTask(fromRetry: Boolean = false): Effect<Action> =
        Effect.run { send ->
            val viewService = DependencyValues.current.splashPageViewService
            coroutineScope {
                val initDeferred =
                    async {
                        try {
                            viewService.initializeBundle(fromRetry)
                            Action.InitializedBundle(null)
                        } catch (exception: Exception) {
                            Action.InitializedBundle(exception)
                        }
                    }

                val cachedDeferred =
                    async<Action> {
                        // Yields to initializeBundle when it settles first.
                        if (viewService.resolveCachedUserIfPoorNetwork()) {
                            Logger.log(
                                "Loading from cached user; network is poor or initialization stalled.",
                                domain = LoggerDomain.clientSession,
                            )
                            Action.InitializedBundle(null)
                        } else {
                            awaitCancellation()
                        }
                    }

                // First task to produce an action wins.
                val action =
                    select<Action> {
                        initDeferred.onAwait { it }
                        cachedDeferred.onAwait { it }
                    }

                initDeferred.cancel()
                cachedDeferred.cancel()
                send(action)
            }
        }

    private fun performRetryHandlerTask(): Effect<Action> =
        Effect.task {
            val viewService = DependencyValues.current.splashPageViewService
            try {
                viewService.performRetryHandler()
                Action.PerformRetryHandlerReturned(null)
            } catch (exception: Exception) {
                Action.PerformRetryHandlerReturned(exception)
            }
        }
}

private const val PROGRESS_NUDGE_CEILING = 0.8f
private const val PROGRESS_NUDGE_INCREMENT = 0.0005f
