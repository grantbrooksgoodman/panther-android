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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.modules.common.services.CommonPropertyLists
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.networking.modules.common.services.ConnectionStatusService
import us.neotechnica.panther.networking.modules.session.services.MessageOutboxService
import us.neotechnica.panther.networking.modules.session.services.retryAllEligible
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.translator.Translator
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
        CommonPropertyLists.initialize(this)

        Networking.initialize(
            context = this,
            defaultEnvironment = NetworkEnvironment.from(BuildConfig.NETWORK_ENVIRONMENT),
            useDebugAppCheckProvider = BuildConfig.DEBUG,
        )

        registerTranslatorActivityProvider()
        setUpMessageOutboxRetry()
    }

    // MARK: - Message Outbox Retry

    private fun setUpMessageOutboxRetry() {
        ConnectionStatusService.initialize(this)

        // Retry eligible entries on launch.
        outboxScope.launch { MessageOutboxService.retryAllEligible() }

        // Retry eligible entries when connectivity is restored.
        ConnectionStatusService.addEffectUponConnectivityRestored(RETRY_OUTBOX_EFFECT_ID) {
            outboxScope.launch { MessageOutboxService.retryAllEligible() }
        }
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
        const val RETRY_OUTBOX_EFFECT_ID = "retryMessageOutbox"
    }
}
