//
//  PantherApplication.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.common.services.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.ExceptionMetadataService
import us.neotechnica.panther.modules.common.services.LoggerPresentationService
import us.neotechnica.panther.modules.common.services.UpdateService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.modules.notifications.services.PantherMessagingService
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.networking.modules.common.services.AnalyticsService
import us.neotechnica.panther.networking.modules.common.services.ConnectionStatusService
import us.neotechnica.panther.networking.modules.session.services.MessageOutboxService
import us.neotechnica.panther.networking.modules.session.services.UserMutationService
import us.neotechnica.panther.networking.modules.session.services.retryAllEligible
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.translator.Translator
import java.util.Date
import java.util.Properties
import java.util.concurrent.atomic.AtomicReference

/**
 * The application entry point.
 *
 * Initializes the Networking framework with the environment
 * baked into the active build flavor and the App Check provider
 * appropriate to the build type, prepares localization, and gives the
 * translator's web-view harness a way to reach the current activity.
 */
class PantherApplication : Application() {
    // MARK: - Properties

    private val outboxScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // MARK: - Application

    override fun onCreate() {
        super.onCreate()

        LocalizedStringResolver.initialize(this)
        Persistent.initialize(this)
        FileStore.initialize(this)
        CommonPropertyLists.initialize(this)
        ContactService.initialize(this)
        TextToSpeechService.initialize(this)
        configureBuild()

        Logger.setPresentationDelegate(LoggerPresentationService)
        AppSubsystem.delegates.registerExceptionMetadataDelegate(ExceptionMetadataService)

        Networking.initialize(
            context = this,
            defaultEnvironment = NetworkEnvironment.from(BuildConfig.NETWORK_ENVIRONMENT),
            useDebugAppCheckProvider = BuildConfig.DEBUG,
        )

        registerTranslatorActivityProvider()
        setUpConnectionStatusEffects()
        setUpPushNotifications()

        AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.OPEN_APP)
    }

    // MARK: - Build Configuration

    /**
     * Populates [Build] from the per-compile `build_info.properties`
     * asset (stamped by the app module's Gradle script, the analog of
     * the iOS Run Script build-number bump).
     */
    private fun configureBuild() {
        val (buildNumber, buildDate, firstCompileDate) = readBuildInfo()
        Build.initialize(
            appStoreBuildNumber = APP_STORE_BUILD_NUMBER,
            buildNumber = buildNumber,
            codeName = CODE_NAME,
            finalName = FINAL_NAME,
            bundleVersion = BuildConfig.VERSION_NAME,
            environment = BuildConfig.NETWORK_ENVIRONMENT,
            milestone = if (BuildConfig.DEBUG) Milestone.ALPHA else Milestone.GENERAL_RELEASE,
            buildDate = Date(buildDate * MILLIS_PER_SECOND),
            firstCompileDate = Date(firstCompileDate * MILLIS_PER_SECOND),
        )
    }

    private fun readBuildInfo(): Triple<Int, Long, Long> =
        runCatching {
            assets.open(BUILD_INFO_ASSET).use { stream ->
                val properties = Properties().apply { load(stream) }
                Triple(
                    properties.getProperty("buildNumber", "0").toInt(),
                    properties.getProperty("buildDate", "0").toLong(),
                    properties.getProperty("firstCompileDate", "0").toLong(),
                )
            }
        }.getOrDefault(Triple(0, 0L, 0L))

    // MARK: - Push Notifications

    private fun setUpPushNotifications() {
        PantherMessagingService.createChannel(this)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            UserMutationService.setCurrentToken(token)
        }
    }

    // MARK: - Connection Status Effects

    private fun setUpConnectionStatusEffects() {
        ConnectionStatusService.initialize(this)

        // Retry eligible entries on launch.
        outboxScope.launch { MessageOutboxService.retryAllEligible() }

        // Retry eligible entries when connectivity is restored.
        ConnectionStatusService.addEffectUponConnectionChanged(RETRY_OUTBOX_EFFECT_ID) {
            if (ConnectionStatusService.isOnline) outboxScope.launch { MessageOutboxService.retryAllEligible() }
        }

        // Show an offline toast when connectivity is lost.
        ConnectionStatusService.addEffectUponConnectionChanged(SHOW_OFFLINE_MODE_TOAST_EFFECT_ID) {
            if (!ConnectionStatusService.isOnline) showOfflineModeToast()
        }

        // Re-check for available updates when connectivity is restored.
        ConnectionStatusService.addEffectUponConnectionChanged(CHECK_FOR_UPDATES_EFFECT_ID) {
            if (ConnectionStatusService.isOnline) outboxScope.launch { runCatching { UpdateService.promptToUpdateIfNeeded() } }
        }

        if (!ConnectionStatusService.isOnline) showOfflineModeToast()
    }

    private fun showOfflineModeToast() {
        Toast.show(
            Toast(
                Toast.Type.Capsule(ToastStyle.WARNING),
                message = LocalizedStringKey.OfflineMode.localized(),
                perpetuation = Toast.Perpetuation.Ephemeral(OFFLINE_TOAST_SECONDS.seconds),
            ),
        )
    }

    // MARK: - Translator Wiring

    private fun registerTranslatorActivityProvider() {
        val currentActivity = AtomicReference<Activity?>(null)

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    currentActivity.set(activity)
                }

                override fun onActivityPaused(activity: Activity) {
                    if (currentActivity.get() === activity) currentActivity.set(null)
                }

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )

        Translator.config.registerCurrentActivityProvider { currentActivity.get() }
    }

    // MARK: - Companion

    private companion object {
        const val CHECK_FOR_UPDATES_EFFECT_ID = "checkForUpdates"
        const val RETRY_OUTBOX_EFFECT_ID = "retryMessageOutbox"
        const val SHOW_OFFLINE_MODE_TOAST_EFFECT_ID = "showOfflineModeToast"

        const val OFFLINE_TOAST_SECONDS = 10L

        const val BUILD_INFO_ASSET = "build_info.properties"
        const val CODE_NAME = "Panther"
        const val FINAL_NAME = "Hello"
        const val APP_STORE_BUILD_NUMBER = 0
        const val MILLIS_PER_SECOND = 1_000L
    }
}
