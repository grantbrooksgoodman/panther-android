//
//  Networking.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking

import android.content.Context
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import us.neotechnica.panther.networking.modules.auth.interfaces.AuthDelegate
import us.neotechnica.panther.networking.modules.auth.services.Auth
import us.neotechnica.panther.networking.modules.common.interfaces.DefaultNetworkActivityIndicatorDelegate
import us.neotechnica.panther.networking.modules.common.interfaces.NetworkActivityIndicatorDelegate
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.networking.modules.database.services.Database
import us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate
import us.neotechnica.panther.networking.modules.storage.services.Storage
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
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
    val DEFAULT_OPERATION_TIMEOUT = 10.seconds

    /** The shared configuration for the Networking framework. */
    val config = Config

    private val applicationContext = LockIsolated<Context?>(null)
    private val readWriteEnabled = LockIsolated(true)

    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether the app may read from
     * and write to the backend.
     *
     * **Note:** The remote read/write enablement service is
     * deferred; this value defaults to `true`.
     */
    val isReadWriteEnabled: Boolean
        get() = readWriteEnabled.wrappedValue

    // MARK: - Methods

    /**
     * Configures the framework and prepares it for use.
     *
     * Call this method once at app launch. It installs the App
     * Check provider factory – the debug provider for emulator and
     * debug builds, Play Integrity otherwise – and records the
     * default environment. Firebase itself is initialized
     * automatically by the `google-services` plugin.
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

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            if (useDebugAppCheckProvider) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            },
        )
    }

    /**
     * Sets whether the app may read from and write to the backend.
     *
     * @param isEnabled Whether read/write access is enabled.
     */
    fun setReadWriteEnabled(isEnabled: Boolean) {
        readWriteEnabled.wrappedValue = isEnabled
    }

    // MARK: - Auxiliary

    /**
     * Returns the time-to-live, in milliseconds, for a cache
     * sample whose backing fetch began at `startMillis`, with a
     * floor of 250 milliseconds. Mirrors the iOS heuristic of
     * caching a value for roughly as long as its fetch took.
     */
    internal fun cacheExpiryMillis(startMillis: Long): Long {
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

        private const val ENVIRONMENT_KEY = "networkEnvironment"
        private const val PREFERENCES_NAME = "networking"

        private val activityIndicator =
            LockIsolated<NetworkActivityIndicatorDelegate>(DefaultNetworkActivityIndicatorDelegate())
        private val auth = LockIsolated<AuthDelegate>(Auth())
        private val database = LockIsolated<DatabaseDelegate>(Database())
        private val environmentDefault = LockIsolated(NetworkEnvironment.PRODUCTION)
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

        /** The delegate that downloads and uploads stored files. */
        val storageDelegate: StorageDelegate
            get() = storage.wrappedValue

        // MARK: - Methods

        /** Registers a custom activity-indicator delegate. */
        fun registerActivityIndicatorDelegate(delegate: NetworkActivityIndicatorDelegate) {
            activityIndicator.wrappedValue = delegate
        }

        /** Registers a custom auth delegate. */
        fun registerAuthDelegate(delegate: AuthDelegate) {
            auth.wrappedValue = delegate
        }

        /** Registers a custom database delegate. */
        fun registerDatabaseDelegate(delegate: DatabaseDelegate) {
            database.wrappedValue = delegate
        }

        /** Registers a custom storage delegate. */
        fun registerStorageDelegate(delegate: StorageDelegate) {
            storage.wrappedValue = delegate
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
            preferences().edit().putString(ENVIRONMENT_KEY, environment.rawValue).apply()
        }

        internal fun setDefaultEnvironment(environment: NetworkEnvironment) {
            environmentDefault.wrappedValue = environment
        }

        // MARK: - Auxiliary

        private fun persistedEnvironment(): NetworkEnvironment? =
            preferences().getString(ENVIRONMENT_KEY, null)?.let { NetworkEnvironment.from(it) }

        private fun preferences() =
            requireContext().getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )
    }
}
