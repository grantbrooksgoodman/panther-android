//
//  Networking.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking

import android.content.Context
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.designsystem.modules.developermode.services.DevModeService
import us.neotechnica.panther.networking.modules.auth.interfaces.AuthDelegate
import us.neotechnica.panther.networking.modules.auth.services.Auth
import us.neotechnica.panther.networking.modules.common.extensions.NetworkingStorageKey
import us.neotechnica.panther.networking.modules.common.extensions.networking
import us.neotechnica.panther.networking.modules.common.extensions.networkingOptionsAction
import us.neotechnica.panther.networking.modules.common.interfaces.DefaultNetworkActivityIndicatorDelegate
import us.neotechnica.panther.networking.modules.common.interfaces.NetworkActivityIndicatorDelegate
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.networking.modules.common.services.ReadWriteEnablementStatusService
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.networking.modules.database.services.Database
import us.neotechnica.panther.networking.modules.health.interfaces.NetworkHealthDelegate
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthConfiguration
import us.neotechnica.panther.networking.modules.health.services.NetworkHealthService
import us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate
import us.neotechnica.panther.networking.modules.storage.services.Storage
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.services.HostedTranslationService
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

/**
 * The entry point to the Networking framework.
 *
 * Call [initialize] once at app launch, then access [config] to
 * register custom delegates, read or change the active
 * environment, and drive network operations through the database,
 * auth, and storage delegates.
 */
object Networking {
    // MARK: - Properties

    /**
     * The default timeout applied to database and storage
     * operations when no explicit value is provided.
     */
    val defaultOperationTimeout = 10.seconds

    /** The shared configuration for the Networking framework. */
    val config = Config

    private val applicationContext = LockIsolated<Context?>(null)
    private val readWriteEnabled = LockIsolated(true)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether the app may read from
     * and write to the backend.
     */
    var isReadWriteEnabled: Boolean
        get() = readWriteEnabled.wrappedValue
        internal set(newValue) {
            readWriteEnabled.wrappedValue = newValue
        }

    /** The delegate that estimates network health. */
    val health: NetworkHealthDelegate
        get() = config.healthDelegate

    // MARK: - Methods

    /**
     * Configures the framework and prepares it for use.
     *
     * Call this method once at app launch. It installs the App
     * Check provider factory – the debug provider for emulator and
     * debug builds, Play Integrity otherwise – records the default
     * environment, registers the networking Developer Mode
     * actions, starts network health monitoring, and begins
     * observing read/write enablement status. Firebase itself is
     * initialized automatically by the `google-services` plugin.
     *
     * @param context A context used to resolve the application
     *   context for persistent storage.
     * @param defaultEnvironment The environment to use when no
     *   runtime override has been persisted – typically derived
     *   from the build flavor.
     * @param useDebugAppCheckProvider Whether to install the App
     *   Check debug provider (required on emulators, where Play
     *   Integrity cannot attest).
     */
    fun initialize(
        context: Context,
        defaultEnvironment: NetworkEnvironment,
        useDebugAppCheckProvider: Boolean,
    ) {
        applicationContext.wrappedValue = context.applicationContext
        config.setDefaultEnvironment(defaultEnvironment)

        DevModeService.insertAction(
            DevModeAction.networkingOptionsAction,
            at = 0,
        )

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            if (useDebugAppCheckProvider) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            },
        )

        config.healthDelegate.startMonitoring()

        scope.launch {
            ReadWriteEnablementStatusService.listenForReadWriteEnablementStatusChanges()
        }
    }

    /**
     * Enables or disables verbose Firebase diagnostic logging.
     *
     * When enabled, the Realtime Database SDK logs its connect,
     * authenticate, and listen activity.
     *
     * **Important:** Realtime Database logging can only be
     * configured before the first database operation, so call this
     * immediately after [initialize] and before any read, write,
     * or prewarm.
     *
     * @param enabled A Boolean value that determines whether
     *   verbose logging is enabled.
     */
    fun setVerboseFirebaseLoggingEnabled(enabled: Boolean) {
        FirebaseDatabase.getInstance().setLogLevel(
            if (enabled) com.google.firebase.database.Logger.Level.DEBUG else com.google.firebase.database.Logger.Level.INFO,
        )
    }

    // MARK: - Auxiliary

    /**
     * Returns the time-to-live, in milliseconds, for a cache
     * sample whose backing fetch began at `startMillis`, with a
     * floor of 250 milliseconds: a value is cached for
     * roughly as long as its fetch took.
     */
    internal fun cacheExpiryMilliseconds(startMillis: Long): Long {
        val elapsed = abs(System.currentTimeMillis() - startMillis)
        return if (elapsed < FLOOR_MILLIS) FLOOR_MILLIS + elapsed else elapsed
    }

    internal fun requireContext(): Context =
        checkNotNull(applicationContext.wrappedValue) {
            "Networking.initialize() must be called at app launch"
        }

    private const val FLOOR_MILLIS = 250L

    // MARK: - Config

    /**
     * The configuration object for the Networking framework.
     *
     * Delegates with sensible Firebase-backed defaults are
     * provided automatically; register a custom conformance with
     * the corresponding `register…` method.
     */
    object Config {
        // MARK: - Properties

        private val activityIndicator =
            LockIsolated<NetworkActivityIndicatorDelegate>(DefaultNetworkActivityIndicatorDelegate())
        private val auth = LockIsolated<AuthDelegate>(Auth())
        private val database = LockIsolated<DatabaseDelegate>(Database())
        private val environmentDefault = LockIsolated(NetworkEnvironment.PRODUCTION)
        private val health = LockIsolated<NetworkHealthDelegate>(NetworkHealthService)
        private val hostedTranslation =
            LockIsolated<HostedTranslationDelegate>(HostedTranslationService.shared)
        private val networkHealthConfig = LockIsolated(NetworkHealthConfiguration.default)
        private val storage = LockIsolated<StorageDelegate>(Storage())

        // MARK: - Computed Properties

        /** The delegate that reflects in-flight network activity. */
        val activityIndicatorDelegate: NetworkActivityIndicatorDelegate
            get() = activityIndicator.wrappedValue

        /** The delegate that manages authentication. */
        val authDelegate: AuthDelegate
            get() = auth.wrappedValue

        /** The delegate that reads, writes, and observes the database. */
        val databaseDelegate: DatabaseDelegate
            get() = database.wrappedValue

        /**
         * The active network environment.
         *
         * Resolves to the persisted runtime override if one exists,
         * otherwise the default supplied to [initialize].
         */
        val environment: NetworkEnvironment
            get() = persistedEnvironment() ?: environmentDefault.wrappedValue

        /** The delegate that estimates network health. */
        val healthDelegate: NetworkHealthDelegate
            get() = health.wrappedValue

        /** The delegate that translates against the hosted archive. */
        val hostedTranslationDelegate: HostedTranslationDelegate
            get() = hostedTranslation.wrappedValue

        /** The active network health configuration. */
        val networkHealthConfiguration: NetworkHealthConfiguration
            get() = networkHealthConfig.wrappedValue

        /** The delegate that downloads and uploads stored files. */
        val storageDelegate: StorageDelegate
            get() = storage.wrappedValue

        // MARK: - Methods

        /**
         * Registers one or more custom delegates in a single call.
         *
         * Each non-`null` argument replaces the corresponding
         * default delegate. Arguments left as `null` are
         * unchanged.
         *
         * @param activityIndicatorDelegate A custom network
         *   activity indicator delegate.
         * @param authDelegate A custom authentication delegate.
         * @param databaseDelegate A custom database delegate.
         * @param healthDelegate A custom network health delegate.
         * @param hostedTranslationDelegate A custom hosted
         *   translation delegate.
         * @param storageDelegate A custom storage delegate.
         */
        @Suppress("LongParameterList")
        fun register(
            activityIndicatorDelegate: NetworkActivityIndicatorDelegate? = null,
            authDelegate: AuthDelegate? = null,
            databaseDelegate: DatabaseDelegate? = null,
            healthDelegate: NetworkHealthDelegate? = null,
            hostedTranslationDelegate: HostedTranslationDelegate? = null,
            storageDelegate: StorageDelegate? = null,
        ) {
            activityIndicatorDelegate?.let { activityIndicator.wrappedValue = it }
            authDelegate?.let { auth.wrappedValue = it }
            databaseDelegate?.let { database.wrappedValue = it }
            healthDelegate?.let { health.wrappedValue = it }
            hostedTranslationDelegate?.let { hostedTranslation.wrappedValue = it }
            storageDelegate?.let { storage.wrappedValue = it }
        }

        /** Registers a custom activity-indicator delegate. */
        fun registerActivityIndicatorDelegate(activityIndicatorDelegate: NetworkActivityIndicatorDelegate) {
            register(activityIndicatorDelegate = activityIndicatorDelegate)
        }

        /** Registers a custom auth delegate. */
        fun registerAuthDelegate(authDelegate: AuthDelegate) {
            register(authDelegate = authDelegate)
        }

        /** Registers a custom database delegate. */
        fun registerDatabaseDelegate(databaseDelegate: DatabaseDelegate) {
            register(databaseDelegate = databaseDelegate)
        }

        /** Registers a custom network-health delegate. */
        fun registerHealthDelegate(healthDelegate: NetworkHealthDelegate) {
            register(healthDelegate = healthDelegate)
        }

        /** Registers a custom hosted-translation delegate. */
        fun registerHostedTranslationDelegate(hostedTranslationDelegate: HostedTranslationDelegate) {
            register(hostedTranslationDelegate = hostedTranslationDelegate)
        }

        /** Registers a custom storage delegate. */
        fun registerStorageDelegate(storageDelegate: StorageDelegate) {
            register(storageDelegate = storageDelegate)
        }

        /**
         * Sets the active network environment.
         *
         * The value is persisted and takes effect immediately for
         * subsequent environment-scoped operations.
         *
         * @param environment The environment to activate.
         */
        fun setEnvironment(environment: NetworkEnvironment) {
            Persistent.setString(
                PersistentStorageKey.networking(NetworkingStorageKey.NETWORK_ENVIRONMENT),
                environment.rawValue,
            )
        }

        /**
         * Sets the active network health configuration.
         *
         * @param configuration The configuration to activate.
         */
        fun setNetworkHealthConfiguration(configuration: NetworkHealthConfiguration) {
            networkHealthConfig.wrappedValue = configuration
        }

        internal fun setDefaultEnvironment(environment: NetworkEnvironment) {
            environmentDefault.wrappedValue = environment
        }

        // MARK: - Auxiliary

        private fun persistedEnvironment(): NetworkEnvironment? =
            Persistent
                .string(PersistentStorageKey.networking(NetworkingStorageKey.NETWORK_ENVIRONMENT))
                ?.let { NetworkEnvironment.from(it) }
    }
}
