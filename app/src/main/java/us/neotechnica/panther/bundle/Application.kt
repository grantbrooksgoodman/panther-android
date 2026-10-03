//
//  Application.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import android.content.Context
import us.neotechnica.panther.BuildConfig
import us.neotechnica.panther.designsystem.modules.alertkit.dependencies.alertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.HUDConfig
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.ApplicationStorageKey
import us.neotechnica.panther.modules.common.extensions.isEmulator
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.ErrorReportingService
import us.neotechnica.panther.modules.common.services.ExceptionMetadataService
import us.neotechnica.panther.modules.common.services.InviteService
import us.neotechnica.panther.modules.common.services.LoggerPresentationService
import us.neotechnica.panther.modules.common.services.NetworkActivityIndicatorService
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.content.user.services.AudioMessagePlaybackService
import us.neotechnica.panther.modules.content.user.services.MediaActionHandlerService
import us.neotechnica.panther.modules.content.user.services.SettingsPageViewService
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.modules.networking.translation.delegates.LocalTranslationArchiverDelegate
import us.neotechnica.panther.modules.networking.user.models.DeviceID
import us.neotechnica.panther.modules.session.entity.extensions.UserSessionServiceStorageKey
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.modules.session.sync.services.ConversationObserverService
import us.neotechnica.panther.navigation.ChatRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthConfiguration
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthProbeConfiguration
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.translator.Translator
import java.util.Date
import java.util.Properties
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The app's bootstrap configuration and shared application
 * namespace.
 *
 * Use [Application] to perform one-time launch setup through
 * [initialize], to reset the app to a signed-out, freshly installed
 * state, and to dismiss presented sheets.
 */
object Application {
    // MARK: - Types

    /** A follow-up action to perform after a reset completes. */
    enum class ResetCompletionProcedure {
        /**
         * Presents the splash page behind an activity indicator
         * overlay, then terminates the app after a one-second delay.
         */
        EXIT_GRACEFULLY,

        /** Clears the user content navigation stack and presents the splash page. */
        NAVIGATE_TO_SPLASH,
    }

    // MARK: - Properties

    /** The moment the current launch began loading. */
    var loadStartDate: Date = Date()

    private var appContext: Context? = null

    // MARK: - Initialization

    /**
     * Performs one-time application setup at launch.
     *
     * Registers the subsystem delegates, configures build metadata,
     * starts the networking framework, prewarms the database and
     * storage connections, and selects the first-run environment.
     * Call this method exactly once, before any other subsystem API
     * is used.
     *
     * @param context A context used to resolve the application
     *   context.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext

        initializeServices(context)
        registerDelegates()
        configureBuild(context)

        Logger.log("Application launched.")

        setUpNetworking(context)
        prewarmConnections()
        selectFirstRunEnvironment()
    }

    // MARK: - Methods

    /** Dismisses every presented sheet in the app. */
    fun dismissSheets() {
        // Only the chat flow presents a sheet on Android; the other
        // navigators have no sheet route to dismiss.
        DependencyValues.current.navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))
    }

    /**
     * Resets the app to a signed-out, freshly installed state.
     *
     * Tears down the session – clearing the outbox, the session
     * store, and conversation observation – then clears all caches,
     * erases the app's on-disk directories, resets persisted
     * defaults, and signs the current user out. Permanent persistent
     * storage keys survive the reset.
     *
     * @param preserveCurrentUserID Whether the current user's
     *   identifier should survive the reset.
     * @param onCompletion The follow-up action to perform once the
     *   reset completes, or `null` to leave the interface untouched.
     */
    fun reset(
        preserveCurrentUserID: Boolean = false,
        onCompletion: ResetCompletionProcedure? = null,
    ) {
        MessageOutboxService.removeAll()
        SessionStore.advanceEpoch()
        ConversationObserverService.stopObserving()

        if (!preserveCurrentUserID) {
            UserSessionService.stopObservingCurrentUserChanges()
        }

        CoreUtilities.clearCaches()
        eraseDirectories()

        Persistent.reset(
            preserving =
                Persistent.permanentAndSubsystemKeys(
                    plus =
                        if (preserveCurrentUserID) {
                            listOf(
                                PersistentStorageKey.userSessionService(UserSessionServiceStorageKey.CURRENT_USER_ID),
                            )
                        } else {
                            null
                        },
                ),
        )
        RuntimeStorage.remove(StoredItemKey.populatedTemporaryCaches)

        if (!preserveCurrentUserID) {
            runCatching { Networking.config.authDelegate.signOut() }
                .onFailure { Logger.log("Failed to sign out during reset. ${it.message}") }
        }

        val procedure = onCompletion ?: return
        dismissSheets()

        when (procedure) {
            ResetCompletionProcedure.EXIT_GRACEFULLY -> beginGracefulExit()

            ResetCompletionProcedure.NAVIGATE_TO_SPLASH -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
                DependencyValues.current.navigation.navigate(
                    Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash)),
                )
            }
        }
    }

    /**
     * Presents the splash page behind an activity indicator overlay,
     * then terminates the app after a one-second delay.
     *
     * Use [beginGracefulExit] to complete a flow that requires the app
     * to restart – such as clearing caches – after the user has been
     * informed. The reset completion procedure and the settings page's
     * exit flows share this sequence.
     */
    fun beginGracefulExit() {
        Overlay.show()
        DependencyValues.current.navigation.navigate(
            Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash)),
        )
        Task.delayed(by = 1.seconds) { exitGracefully() }
    }

    // MARK: - Auxiliary

    private fun initializeServices(context: Context) {
        AnalyticsService.initialize(context)
        LocalizedStringResolver.initialize(context)
        Persistent.initialize(context)
        FileStore.initialize(context)
        CommonPropertyLists.initialize(context)
        ContactService.initialize(context)
        DeviceID.initialize(context)
        InviteService.initialize(context)
        SettingsPageViewService.initialize(context)
        TextToSpeechService.initialize(context)
        AudioMessagePlaybackService.initialize(context)
        MediaActionHandlerService.initialize(context)
    }

    private fun registerDelegates() {
        Logger.setPresentationDelegate(LoggerPresentationService)
        AppSubsystem.delegates.registerCacheDomainListDelegate(CacheDomainList)
        AppSubsystem.delegates.registerExceptionMetadataDelegate(ExceptionMetadataService)
        AppSubsystem.delegates.registerLoggerDomainSubscriptionDelegate(LoggerDomainSubscription)
        AppSubsystem.delegates.registerPermanentPersistentStorageKeyDelegate(PermanentKeyDelegate)
        AppSubsystem.delegates.registerErrorReportDelegate(ErrorReportingService)
        LocalTranslationArchiverDelegate.registerWithDependencies()

        DependencyValues.current.alertKitConfig.overrideTranslationHUDConfig(
            HUDConfig(appearsAfter = 500.milliseconds, isModal = true),
        )
    }

    private fun configureBuild(context: Context) {
        val (buildNumber, buildDate, firstCompileDate) = readBuildInfo(context)
        Build.initialize(
            appStoreBuildNumber = APP_STORE_BUILD_NUMBER,
            buildNumber = buildNumber,
            codeName = CODE_NAME,
            finalName = FINAL_NAME,
            bundleVersion = BuildConfig.VERSION_NAME,
            environment = BuildConfig.NETWORK_ENVIRONMENT,
            milestone = resolveBuildMilestone(),
            buildDate = Date(buildDate * MILLIS_PER_SECOND),
            firstCompileDate = Date(firstCompileDate * MILLIS_PER_SECOND),
        )
    }

    private fun resolveBuildMilestone(): Milestone {
        val key = PersistentStorageKey.application(ApplicationStorageKey.BUILD_MILESTONE_STRING)
        val persisted = Persistent.string(key)
        val milestone =
            persisted?.let { Milestone.from(it) }
                ?: if (Build.isEmulator) Milestone.BETA else Milestone.GENERAL_RELEASE
        Persistent.setString(key, milestone.rawValue)
        return milestone
    }

    private fun readBuildInfo(context: Context): Triple<Int, Long, Long> =
        runCatching {
            context.assets.open(BUILD_INFO_ASSET).use { stream ->
                val properties = Properties().apply { load(stream) }
                Triple(
                    properties.getProperty("buildNumber", "0").toInt(),
                    properties.getProperty("buildDate", "0").toLong(),
                    properties.getProperty("firstCompileDate", "0").toLong(),
                )
            }
        }.getOrDefault(Triple(0, 0L, 0L))

    private fun setUpNetworking(context: Context) {
        Networking.initialize(
            context = context,
            defaultEnvironment = NetworkEnvironment.from(BuildConfig.NETWORK_ENVIRONMENT),
            useDebugAppCheckProvider = BuildConfig.DEBUG,
        )
        Networking.config.registerActivityIndicatorDelegate(NetworkActivityIndicatorService)
        Networking.config.setNetworkHealthConfiguration(
            NetworkHealthConfiguration.default.copy(
                probeConfiguration = NetworkHealthProbeConfiguration(url = "https://www.apple.com"),
            ),
        )
    }

    private fun prewarmConnections() {
        Networking.config.databaseDelegate.prewarm()
        Networking.config.storageDelegate.prewarm()
    }

    private fun selectFirstRunEnvironment() {
        val hasRunOnceKey = PersistentStorageKey.application(ApplicationStorageKey.HAS_RUN_ONCE)
        if (Build.isEmulator && Persistent.booleanOrNull(hasRunOnceKey) == null) {
            Networking.config.setEnvironment(NetworkEnvironment.DEVELOPMENT)
            Persistent.setBoolean(hasRunOnceKey, true)
        } else if (Build.milestone == Milestone.GENERAL_RELEASE) {
            Networking.config.setEnvironment(NetworkEnvironment.PRODUCTION)
        }
    }

    private fun eraseDirectories() {
        val context = appContext ?: return
        context.filesDir?.listFiles()?.forEach { it.deleteRecursively() }
        context.noBackupFilesDir?.listFiles()?.forEach { it.deleteRecursively() }
        context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun exitGracefully() {
        Translator.config.currentActivityProvider?.invoke()?.finishAffinity()
        exitProcess(0)
    }

    // MARK: - Constants

    private const val BUILD_INFO_ASSET = "build_info.properties"
    private const val CODE_NAME = "Panther"
    private const val FINAL_NAME = "Hello"
    private const val APP_STORE_BUILD_NUMBER = 0
    private const val MILLIS_PER_SECOND = 1_000L
}
