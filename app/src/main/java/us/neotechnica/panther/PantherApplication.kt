//
//  PantherApplication.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.common.models.ConnectionStatusServiceEffectID
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.ConnectionStatusService
import us.neotechnica.panther.modules.common.services.NotificationService
import us.neotechnica.panther.modules.common.services.PushTokenService
import us.neotechnica.panther.modules.common.services.UpdateService
import us.neotechnica.panther.modules.content.user.services.UICacheInvalidationService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.notifications.services.PantherMessagingService
import us.neotechnica.panther.modules.session.ClientSession
import us.neotechnica.panther.modules.session.entity.extensions.calculateBadgeNumber
import us.neotechnica.panther.modules.session.state.services.retryAllEligible
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.translator.Translator
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

/**
 * The application entry point.
 *
 * Performs one-time launch setup through
 * [us.neotechnica.panther.bundle.Application], gates Firebase
 * analytics collection, wires connection-status effects and push
 * notifications, gives the translator's web-view harness a way to
 * reach the current activity, and observes the process lifecycle.
 */
class PantherApplication : Application() {
    // MARK: - Properties

    private val outboxScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // MARK: - Application

    override fun onCreate() {
        super.onCreate()

        us.neotechnica.panther.bundle.Application.initialize(this)

        setUpFirebaseAnalytics()
        setUpConnectionStatusEffects()
        setUpPushNotifications()
        registerTranslatorActivityProvider()
        observeProcessLifecycle()

        AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.OPEN_APP)
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
                            NotificationService.setBadgeNumber(currentUser.calculateBadgeNumber())
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
            .setAnalyticsCollectionEnabled(AnalyticsService.shouldEnableDataCollection)
    }

    // MARK: - Push Notifications

    private fun setUpPushNotifications() {
        PantherMessagingService.createChannel(this)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            PushTokenService.setCurrentToken(token)
        }
    }

    // MARK: - Connection Status Effects

    private fun setUpConnectionStatusEffects() {
        ConnectionStatusService.initialize(this)

        // Retry eligible entries when connectivity is restored.
        ConnectionStatusService.addEffectUponConnectionChanged(ConnectionStatusServiceEffectID.RETRY_MESSAGE_OUTBOX) {
            if (ConnectionStatusService.isOnline) outboxScope.launch { ClientSession.outbox.retryAllEligible() }
        }

        // Show an offline toast when connectivity is lost.
        ConnectionStatusService.addEffectUponConnectionChanged(ConnectionStatusServiceEffectID.SHOW_OFFLINE_MODE_TOAST) {
            if (!ConnectionStatusService.isOnline) showOfflineModeToast()
        }

        // Re-check for available updates when connectivity is restored.
        ConnectionStatusService.addEffectUponConnectionChanged(ConnectionStatusServiceEffectID.CHECK_FOR_UPDATES) {
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
        const val OFFLINE_TOAST_SECONDS = 10L
    }
}
