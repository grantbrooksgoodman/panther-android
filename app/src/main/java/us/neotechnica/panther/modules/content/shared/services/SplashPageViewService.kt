//
//  SplashPageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 27/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import us.neotechnica.panther.designsystem.modules.alertkit.dependencies.alertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.ErrorAlert
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.isEmulator
import us.neotechnica.panther.modules.common.extensions.isOnline
import us.neotechnica.panther.modules.common.extensions.currentUserIDNotSet
import us.neotechnica.panther.modules.common.models.RemoteCacheStatus
import us.neotechnica.panther.modules.common.services.AlertKitTranslationService
import us.neotechnica.panther.modules.common.services.CommonServices
import us.neotechnica.panther.modules.common.services.ErrorReportingService
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.extensions.syncIfNeeded
import us.neotechnica.panther.modules.content.user.extensions.updateDeviceIDIfNeeded
import us.neotechnica.panther.modules.content.user.services.UICacheInvalidationService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.NetworkServices
import us.neotechnica.panther.modules.networking.common.populateTemporaryCaches
import us.neotechnica.panther.modules.networking.networking
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.ClientSession
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.extensions.calculateBadgeNumber
import us.neotechnica.panther.modules.session.entity.extensions.conversations
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.extensions.messages
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.networking.modules.common.extensions.noValueExists
import us.neotechnica.panther.networking.modules.health.extensions.networkHealth
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthTier
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import us.neotechnica.panther.bundle.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// The iOS `SplashPageViewService` disables `cyclomatic_complexity`,
// `file_length`, `function_body_length`, and `type_body_length`.
/**
 * The service that initializes the app's data bundle behind the splash
 * page.
 *
 * Use [SplashPageViewService] to bring the app from launch – or from a
 * completed sign-in or sign-up – to a presentable state. The splash
 * page's reducer calls [initializeBundle] when the page appears,
 * racing it against [resolveCachedUserIfPoorNetwork]; whichever
 * settles first determines how the app loads, and the other is
 * cancelled. The service reports progress through
 * [initializationProgress] and publishes the loading indicator the
 * splash page displays.
 *
 * If initialization fails, the splash page's reducer drives a recovery
 * sequence through this service: the first failure retries
 * automatically – attempting recovery through [performRetryHandler]
 * when the failure warrants one – and subsequent failures present
 * [presentErrorAlert] so the user can retry manually.
 *
 * **Important:** Because initialization races
 * [resolveCachedUserIfPoorNetwork], both methods check for
 * cancellation between steps and return early once cancelled.
 */
@Suppress("LargeClass", "TooManyFunctions")
object SplashPageViewService {
    // MARK: - Types

    /** The loading indicator shown behind the splash page while the bundle initializes. */
    enum class LoadingIndicatorStyle {
        /** A determinate progress bar, shown for heavy loads: a poor network or a cold cache. */
        BAR,

        /** No indicator, shown for the first second of loading. */
        HIDDEN,

        /** An indeterminate spinner, shown for light loads. */
        SPINNER,
    }

    // MARK: - Dependencies

    private val clientSession: ClientSession by Dependency { it.clientSession }
    private val networking: NetworkServices by Dependency { it.networking }
    private val services: CommonServices by Dependency { it.commonServices }

    // MARK: - Properties

    private val initializationProgressState = MutableStateFlow(0f)
    private val loadingIndicatorStyleState = MutableStateFlow(LoadingIndicatorStyle.HIDDEN)

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loadingIndicatorStyleJob = LockIsolated<Job?>(null)
    private val networkHealth = SharedState { it.networkHealth }
    private val quickLoadTimeoutJob = LockIsolated<Job?>(null)

    private var didAttemptDatabaseRepair = false

    @Suppress("unused")
    private var didSurpassQuickLoadTimeoutDuration = false

    // MARK: - Computed Properties

    /**
     * The fraction of bundle initialization that has completed, from
     * `0` to `1`.
     *
     * When the value reaches `1`, it resets itself to `0` after a brief
     * delay unless a new initialization has since begun.
     */
    val initializationProgress: StateFlow<Float> = initializationProgressState.asStateFlow()

    /**
     * The loading indicator shown behind the splash page, or
     * [LoadingIndicatorStyle.HIDDEN] for the first second of a fresh
     * load. Resolved once, one second in, from network and cache state.
     */
    val loadingIndicatorStyle: StateFlow<LoadingIndicatorStyle> = loadingIndicatorStyleState.asStateFlow()

    // MARK: - Set Initialization Progress

    /**
     * Sets the fraction of bundle initialization that has completed.
     *
     * When the value reaches `1`, it resets itself to `0` after a brief
     * delay unless a new initialization has since begun.
     *
     * @param value The new progress value, from `0` to `1`.
     */
    fun setInitializationProgress(value: Float) {
        initializationProgressState.value = value
        if (value != 1f) return
        Task.delayed(by = 2.seconds) {
            // Skip if a new initialization has since begun.
            if (initializationProgressState.value == 1f) setInitializationProgress(0f)
        }
    }

    // MARK: - Initialize Bundle

    /**
     * Initializes the app's data bundle, preparing every service the
     * app requires.
     *
     * The splash page's reducer calls this method when the page
     * appears, and again – passing `true` – when retrying after a
     * failure. Progress is reported through [initializationProgress],
     * reaching `1` when initialization completes.
     *
     * @param fromRetry Whether this call retries a previous attempt.
     *   Passing `true` preserves the existing progress and timeout
     *   state.
     *
     * @throws Exception If a required startup step fails. Failing to
     *   resolve the current user because no user is signed in is not an
     *   error; in that case, progress completes so the user can proceed
     *   to onboarding.
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    suspend fun initializeBundle(fromRetry: Boolean) {
        /* Service Setup */

        us.neotechnica.panther.designsystem.modules.foundation.toast.Toast.hide()

        if (!fromRetry) {
            didSurpassQuickLoadTimeoutDuration = false
            setInitializationProgress(0f)
            loadingIndicatorStyleState.value = LoadingIndicatorStyle.HIDDEN

            quickLoadTimeoutJob.withValue {
                it.value?.cancel()
                it.value =
                    Task.delayed(by = 2500.milliseconds) {
                        if (initializationProgress.value <= QUICK_LOAD_TIMEOUT_PROGRESS_THRESHOLD) didSurpassQuickLoadTimeoutDuration = true
                    }
            }

            // Show no indicator for the first second, then pick the bar
            // or spinner from the network and cache state at that point.
            loadingIndicatorStyleJob.withValue {
                it.value?.cancel()
                it.value =
                    Task.delayed(by = 1.seconds) {
                        if (initializationProgress.value < 1f) {
                            loadingIndicatorStyleState.value = resolveLoadingIndicatorStyle()
                        }
                    }
            }
        }

        /* AlertKit Delegate Setup */

        DependencyValues.current.alertKitConfig.registerReportDelegate(ErrorReportingService)
        DependencyValues.current.alertKitConfig.registerTranslationDelegate(AlertKitTranslationService)

        /* Breadcrumbs Capture Setup: absent – BreadcrumbsCaptureService is deferred. */

        /* Store Observation Setup */

        UICacheInvalidationService.startObserving()

        /* Offline User Setup */

        if (!Build.isOnline) {
            val currentUser =
                clientSession.entity.user.currentUser ?: return Logger.log(
                    Exception(
                        "No persisted user exists.",
                        isReportable = false,
                        metadata = ExceptionMetadata(this),
                    ),
                )

            setInitializationProgress(1f)
            RuntimeStorage.languageCode = currentUser.languageCode
            return
        }

        /* Pre-flight Configuration: enhanced-translation configuration is cut. */

        Logger.setReportsErrorsAutomatically(
            !Build.isEmulator && Build.milestone == Milestone.GENERAL_RELEASE,
        )

        services.review.incrementAppOpenCount()

        /* Anonymous Sign-In */

        currentCoroutineContext().ensureActive()
        runCatching { networking.auth.signInAnonymously() }

        /* Parallel Initialization */

        currentCoroutineContext().ensureActive()

        // Launch the heaviest independent network calls concurrently.
        val currentUserID = User.currentUserID
        val needsRestart =
            coroutineScope {
                val resolveCurrentUserResult = async { runCatchingException { clientSession.entity.user.resolveCurrentUser() } }
                val resolveLanguageCodeResult = async { runCatchingException { clientSession.resolveAndSetLanguageCode() } }
                val cacheStatusResult = async { resolveCacheStatusResult(currentUserID) }

                // Revalidate hosted metadata in the background: consumers
                // read the persisted snapshot until it lands.
                appScope.launch { runCatching { services.metadata.resolveValues() } }

                currentCoroutineContext().ensureActive()
                if (currentUserID != null) resolveLanguageCodeResult.await()?.let { throw it }

                setInitializationProgress(initializationProgress.value + SMALL_PROGRESS_INCREMENT)

                /* UpdateService Setup */

                currentCoroutineContext().ensureActive()
                services.update.incrementRelaunchCountIfNeeded()
                services.update.promptToUpdateIfNeeded()
                services.update.startObservingForcedUpdateChanges()

                setInitializationProgress(initializationProgress.value + UPDATE_PROGRESS_INCREMENT)

                /* Cache Setup */

                if (handleCacheStatus(cacheStatusResult, currentUserID)) {
                    resolveCurrentUserResult.cancel()
                    return@coroutineScope true
                }

                /* UserSessionService Setup */

                resolveCurrentUserAndFinish(resolveCurrentUserResult)
                false
            }

        if (needsRestart) return initializeBundle(fromRetry = true)
    }

    // MARK: - Perform Retry Handler

    /**
     * Attempts to recover from a failed initialization.
     *
     * The first call attempts a database repair; a subsequent call
     * after another failure resets the application entirely.
     *
     * **Note:** Per the parity plan, `IntegrityService.repairDatabase()`
     * – and the iOS `updateRequired` → `isForcedUpdateRequired` branch
     * that follows it – are deferred to a separate plan, so the first
     * call performs no repair.
     */
    @Suppress("RedundantSuspendModifier")
    suspend fun performRetryHandler() {
        if (!didAttemptDatabaseRepair) {
            didAttemptDatabaseRepair = true
        } else {
            Application.reset()
            didAttemptDatabaseRepair = false
        }
    }

    // MARK: - Present Error Alert

    /**
     * Presents an error alert for the given exception, offering to try
     * again.
     *
     * The alert's error description is translated when the exception
     * carries a specific user-facing descriptor; generic and timed-out
     * descriptors are presented without translation. When the exception
     * is reportable, the alert includes an option to send an error
     * report.
     *
     * @param exception The exception to present.
     */
    suspend fun presentErrorAlert(exception: Exception) {
        val mockGenericException = Exception(metadata = ExceptionMetadata(this))
        val mockTimedOutException = Exception("The operation timed out.", metadata = ExceptionMetadata(this))

        val notGenericDescriptor = exception.userFacingDescriptor != mockGenericException.userFacingDescriptor
        val notTimedOutDescriptor = exception.userFacingDescriptor != mockTimedOutException.userFacingDescriptor
        val hasUserFacingDescriptor = exception.descriptor != exception.userFacingDescriptor

        val shouldTranslate = hasUserFacingDescriptor && notGenericDescriptor && notTimedOutDescriptor

        val translationOptionKeys = mutableListOf<ErrorAlert.TranslationOptionKey>()
        if (shouldTranslate) translationOptionKeys.add(ErrorAlert.TranslationOptionKey.ErrorDescription)
        if (exception.isReportable) translationOptionKeys.add(ErrorAlert.TranslationOptionKey.SendErrorReportButtonTitle)

        ErrorAlert(
            exception = exception,
            dismissButtonTitle = LocalizedStringKey.TryAgain.localized(),
        ).present(translating = translationOptionKeys)
    }

    // MARK: - Resolve Cached User If Poor Network

    /**
     * Falls back to the cached user when the network is too poor for
     * full initialization.
     *
     * Call this method concurrently with [initializeBundle]. If a
     * complete cached user – one whose conversations all have their
     * messages and users present – is available, the method waits for
     * the network health to degrade to poor, or for a fallback deadline
     * to elapse on a network that has produced no health evidence. It
     * then reports progress as nearly complete, applies the cached
     * user's language, and schedules a deferred resolution of the
     * user's data for when the network health recovers.
     *
     * @return `true` if the app loaded from the cached user; otherwise,
     *   `false`.
     */
    suspend fun resolveCachedUserIfPoorNetwork(): Boolean {
        val currentUser = clientSession.entity.user.currentUser
        val conversations = currentUser?.conversations
        if (currentUser == null ||
            conversations == null ||
            !conversations.all { it.messages != null } ||
            !conversations.all { it.users != null }
        ) {
            Logger.log(
                Exception(
                    "Insufficient data to load from cached user.",
                    isReportable = false,
                    metadata = ExceptionMetadata(this),
                ),
            )
            return false
        }

        // The values stream doesn't replay the current value, so check
        // it first; also resolves instantly on retry, when the health
        // is already known to be poor.
        if (networking.health.health.tier != NetworkHealthTier.POOR) {
            if (!raceHealthPoorAgainstDeadline()) return false
        }

        setInitializationProgress(CACHED_USER_PROGRESS)
        RuntimeStorage.languageCode = currentUser.languageCode

        appScope.launch { resolveCurrentUserDataWhenNetworkRecovers() }
        return true
    }

    // MARK: - Auxiliary

    private suspend fun handleCacheStatus(
        cacheStatusResult: kotlinx.coroutines.Deferred<Result<RemoteCacheStatus?>>,
        currentUserID: String?,
    ): Boolean {
        try {
            currentCoroutineContext().ensureActive()
            val cacheStatus = cacheStatusResult.await().getOrThrow()
            if (cacheStatus != null && currentUserID != null) {
                setInitializationProgress(initializationProgress.value + SMALL_PROGRESS_INCREMENT)

                if (cacheStatus == RemoteCacheStatus.INVALID) {
                    currentCoroutineContext().ensureActive()
                    services.remoteCache.setCacheStatus(RemoteCacheStatus.VALID, currentUserID)
                    Application.reset(preserveCurrentUserID = true)
                    return true
                }
            }
        } catch (error: Exception) {
            if (!error.isEqual(to = AppException.noValueExists)) Logger.log(error)
        }

        return false
    }

    private suspend fun resolveCurrentUserAndFinish(
        resolveCurrentUserResult: kotlinx.coroutines.Deferred<Exception?>,
    ) {
        // User resolution likely completed during the metadata + update
        // + cache gates above.
        try {
            currentCoroutineContext().ensureActive()
            resolveCurrentUserResult.await()?.let { throw it }
            setInitializationProgress(initializationProgress.value + USER_RESOLUTION_PROGRESS_INCREMENT)

            val currentUser =
                clientSession.entity.user.currentUser ?: throw Exception(
                    "Failed to resolve current user.",
                    metadata = ExceptionMetadata(this),
                )

            /* UI Setup: enhanced-translation configuration and prevarication mode are cut. */

            /* Device ID Update */

            // Must complete before the database observer starts
            // (post-splash), otherwise the observer sees the change and
            // triggers sign-out.
            currentCoroutineContext().ensureActive()
            currentUser.updateDeviceIDIfNeeded()

            /* Contact Pair Archive + Temporary Cache Population */

            appScope.launch(Dispatchers.Default) {
                try {
                    ContactService.syncIfNeeded()
                } catch (exception: Exception) {
                    Logger.log(exception)
                }
            }

            if ((currentUser.conversationIDs ?: emptyList()).size > CONVERSATION_COUNT_TEMPORARY_CACHE_THRESHOLD &&
                clientSession.store.conversations.isEmpty()
            ) {
                appScope.launch(Dispatchers.Default) {
                    try {
                        networking.database.populateTemporaryCaches()
                    } catch (exception: Exception) {
                        Logger.log(exception)
                    }
                }
            }

            /* Conversation Resolution */

            currentCoroutineContext().ensureActive()
            clientSession.entity.conversation.setCurrentConversation(null)
            clientSession.entity.user.resolveCurrentUser(UserSessionService.DataType.entries.toSet())

            setInitializationProgress(1f)

            /* Post-launch Maintenance */

            schedulePostLaunchMaintenance(currentUser)
        } catch (error: Exception) {
            if (error.isEqual(to = AppException.currentUserIDNotSet)) {
                setInitializationProgress(1f)
                return
            }

            throw error
        }
    }

    private fun schedulePostLaunchMaintenance(currentUser: User) {
        appScope.launch {
            if (Build.environment != STAGING_ENVIRONMENT) {
                try {
                    services.pushToken.prunePushTokensForCurrentUser()
                } catch (exception: Exception) {
                    Logger.log(exception, with = AlertType.toastInPrerelease)
                }
            }

            // Typing indicator reset is cut.

            try {
                services.notification.setBadgeNumber(currentUser.calculateBadgeNumber())
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toastInPrerelease)
            }

            // PenPals sharing-data update is cut.
        }
    }

    private suspend fun raceHealthPoorAgainstDeadline(): Boolean {
        coroutineScope {
            val healthPoor =
                launch { networkHealth.projectedValue.changes.first { it.tier == NetworkHealthTier.POOR } }

            // Fallback deadline; on a dead network, the health estimator
            // has no evidence until the first censored timeout sample lands.
            val deadline = launch { delay(5.seconds) }

            select<Unit> {
                healthPoor.onJoin {}
                deadline.onJoin {}
            }

            healthPoor.cancel()
            deadline.cancel()
        }

        return true
    }

    private suspend fun resolveCacheStatusResult(userID: String?): Result<RemoteCacheStatus?> =
        try {
            Result.success(resolveCacheStatus(userID))
        } catch (exception: Exception) {
            Result.failure(exception)
        }

    private suspend fun resolveCacheStatus(userID: String?): RemoteCacheStatus? {
        val resolvedUserID = userID ?: return null
        return services.remoteCache.cacheStatus(resolvedUserID)
    }

    private fun resolveLoadingIndicatorStyle(): LoadingIndicatorStyle {
        if (User.currentUserID == null) return LoadingIndicatorStyle.SPINNER

        if (networking.health.health.tier != NetworkHealthTier.POOR &&
            clientSession.store.conversations.isNotEmpty()
        ) {
            return LoadingIndicatorStyle.HIDDEN
        }

        return LoadingIndicatorStyle.BAR
    }

    private suspend fun resolveCurrentUserDataWhenNetworkRecovers() {
        for (attempt in 1..MAXIMUM_DEFERRED_RESOLUTION_ATTEMPTS) {
            currentCoroutineContext().ensureActive()

            try {
                clientSession.entity.user.resolveCurrentUser(UserSessionService.DataType.entries.toSet())
                return Logger.log(
                    "Deferred resolution of current user data was successful.",
                    domain = LoggerDomain.clientSession,
                )
            } catch (error: Exception) {
                Logger.log(error)
            }

            if (attempt >= MAXIMUM_DEFERRED_RESOLUTION_ATTEMPTS) {
                return Logger.log(
                    Exception(
                        "Exhausted deferred resolution attempts; retaining cached data.",
                        isReportable = false,
                        metadata = ExceptionMetadata(this),
                    ),
                )
            }

            delay(DEFERRED_RESOLUTION_RETRY_INTERVAL)
        }
    }

    private suspend fun runCatchingException(operation: suspend () -> Unit): Exception? =
        try {
            operation()
            null
        } catch (exception: Exception) {
            exception
        }

    // MARK: - Companion

    private const val CACHED_USER_PROGRESS = 0.9f
    private const val CONVERSATION_COUNT_TEMPORARY_CACHE_THRESHOLD = 20
    private const val MAXIMUM_DEFERRED_RESOLUTION_ATTEMPTS = 15
    private const val QUICK_LOAD_TIMEOUT_PROGRESS_THRESHOLD = 0.6f
    private const val SMALL_PROGRESS_INCREMENT = 0.02f
    private const val STAGING_ENVIRONMENT = "staging"
    private const val UPDATE_PROGRESS_INCREMENT = 0.01f
    private const val USER_RESOLUTION_PROGRESS_INCREMENT = 0.2f

    private val DEFERRED_RESOLUTION_RETRY_INTERVAL = 3.seconds
}
