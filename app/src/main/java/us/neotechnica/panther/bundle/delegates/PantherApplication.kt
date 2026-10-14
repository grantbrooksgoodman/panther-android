//
//  PantherApplication.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.delegates

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.modules.common.extensions.isOnline
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.AnalyticsService
import us.neotechnica.panther.modules.common.services.sources.ConnectionStatusService
import us.neotechnica.panther.modules.common.services.sources.NotificationService
import us.neotechnica.panther.modules.common.services.sources.PushTokenService
import us.neotechnica.panther.modules.content.user.models.ConnectionStatusServiceEffectID
import us.neotechnica.panther.modules.content.user.services.UICacheInvalidationService
import us.neotechnica.panther.modules.networking.sources.common.dependencies.firebaseMessaging
import us.neotechnica.panther.modules.session.ClientSession
import us.neotechnica.panther.modules.session.state.services.retryAllEligible
import us.neotechnica.panther.notificationextension.PantherMessagingService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.translator.Translator
import java.util.concurrent.atomic.AtomicReference

/**
 * The application entry point.
 *
 * Performs one-time launch setup through
 * [us.neotechnica.panther.Application], gates Firebase
 * analytics collection, wires connection-status effects and push
 * notifications, gives the translator's web-view harness a way to
 * reach the current activity, and observes the process lifecycle.
 */
class PantherApplication : Application() {
    // MARK: - Dependencies

    private val analyticsService: AnalyticsService by Dependency { it.commonServices.analytics }
    private val connectionStatusService: ConnectionStatusService by Dependency { it.commonServices.connectionStatus }
    private val notificationService: NotificationService by Dependency { it.commonServices.notification }
    private val pushTokenService: PushTokenService by Dependency { it.commonServices.pushToken }

    // MARK: - Properties

    private val outboxScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // MARK: - Application

    override fun onCreate() {
        super.onCreate()

        us.neotechnica.panther.Application
            .initialize(this)

        setUpFirebaseAnalytics()
        setUpConnectionStatusEffects()
        setUpPushNotifications()
        registerTranslatorActivityProvider()
        observeProcessLifecycle()

        analyticsService.logEvent(AnalyticsService.AnalyticsEvent.OPEN_APP)
    }

    // MARK: - Process Lifecycle

    private fun observeProcessLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    val sharedEvents = DependencyValues.current.sharedEvents
                    sharedEvents.traitCollectionChanged.send(Unit)
                    outboxScope.launch { ClientSession.outbox.retryAllEligible() }
                }

                override fun onStop(owner: LifecycleOwner) {
                    ClientSession.store.flushNow()
                    UICacheInvalidationService.refreshNotificationExtensionNameMap()
                    outboxScope.launch {
                        val currentUser = ClientSession.entity.user.currentUser ?: return@launch
                        try {
                            notificationService.setBadgeNumber(currentUser.calculateBadgeNumber())
                        } catch (exception: Exception) {
                            Logger.log(exception)
                        }
                    }
                }
            },
        )
    }

    // MARK: - Firebase Analytics

    private fun setUpFirebaseAnalytics() {
        FirebaseAnalytics
            .getInstance(this)
            .setAnalyticsCollectionEnabled(analyticsService.shouldEnableDataCollection)
    }

    // MARK: - Push Notifications

    private fun setUpPushNotifications() {
        PantherMessagingService.createChannel(this)
        DependencyValues.current.firebaseMessaging.token.addOnSuccessListener { token ->
            pushTokenService.setCurrentToken(token)
        }
    }

    // MARK: - Connection Status Effects

    private fun setUpConnectionStatusEffects() {
        connectionStatusService.initialize(this)

        // Retry eligible entries when connectivity is restored.
        connectionStatusService.addEffectUponConnectionChanged(ConnectionStatusServiceEffectID.RETRY_MESSAGE_OUTBOX) {
            if (Build.isOnline) outboxScope.launch { ClientSession.outbox.retryAllEligible() }
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
}
