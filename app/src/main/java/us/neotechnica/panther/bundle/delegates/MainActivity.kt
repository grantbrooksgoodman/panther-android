//
//  MainActivity.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.delegates

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import us.neotechnica.panther.bundle.developermode.ShakeDetector
import us.neotechnica.panther.bundle.notifications
import us.neotechnica.panther.bundle.openConversationIDKey
import us.neotechnica.panther.designsystem.modules.alertkit.views.AlertHost
import us.neotechnica.panther.designsystem.modules.developermode.services.DevModeService
import us.neotechnica.panther.designsystem.modules.foundation.components.CompatibilityFrame
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUDHost
import us.neotechnica.panther.designsystem.modules.foundation.overlay.OverlayHost
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheetHost
import us.neotechnica.panther.designsystem.modules.foundation.toast.ToastHost
import us.neotechnica.panther.designsystem.modules.foundation.views.root.RootOverlayView
import us.neotechnica.panther.designsystem.modules.foundation.views.root.RootWindow
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.designsystem.modules.theming.views.PantherTheme
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.AnalyticsService
import us.neotechnica.panther.modules.common.services.sources.NotificationService
import us.neotechnica.panther.modules.content.user.dependencies.uiCacheInvalidationService
import us.neotechnica.panther.modules.content.user.services.UICacheInvalidationService
import us.neotechnica.panther.modules.session.ClientSession
import us.neotechnica.panther.navigation.PendingChatNavigation
import us.neotechnica.panther.navigation.RootView
import us.neotechnica.panther.networking.modules.common.modifiers.IndicatesNetworkActivity
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ApplicationSuspensionDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * The app's single activity.
 *
 * The activity hosts [RootView] and the alert, toast, overlay, HUD,
 * sheet, and build-info hosts inside the app theme; records the
 * conversation a launch or notification intent targets through
 * [PendingChatNavigation]; sends the app to the background when Back
 * has nothing left to pop; and flushes the session store when it
 * finishes.
 */
class MainActivity : ComponentActivity() {
    // MARK: - Dependencies

    private val analyticsService: AnalyticsService by Dependency { it.commonServices.analytics }
    private val notificationService: NotificationService by Dependency { it.commonServices.notification }
    private val uiCacheInvalidationService: UICacheInvalidationService by Dependency { it.uiCacheInvalidationService }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash is blank and leaves on the first frame, so
        // the app's own splash page takes over at once.
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)

        // A non-null saved state on the first activity creation of a
        // process means the process was recreated after death (not a
        // config change, which reuses the process); the open
        // conversation is restored, unless a tapped notification
        // already targets one.
        val isProcessRestart = savedInstanceState != null && !hasCreatedInProcess
        hasCreatedInProcess = true

        PendingChatNavigation.set(pendingChat(intent))
        if (isProcessRestart && intent?.getStringExtra(PendingChatNavigation.CONVERSATION_ID_KEY_EXTRA) == null) {
            Persistent.string(PersistentStorageKey.openConversationIDKey)?.let {
                PendingChatNavigation.set(PendingChatNavigation.PendingChat(it))
            }
        }

        // Back with nothing left to go back to sends the app to the
        // background rather than closing it. Android does this itself only
        // for a task opened from the home screen; opened any
        // other way – from a notification, say – the activity would finish,
        // and reopening the app would rebuild the signed-in session's content.
        // Registered before the content, every in-app Back handler takes
        // precedence over it.
        onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }

        // A graceful exit returns to the Home screen before the process
        // terminates.
        AppSubsystem.delegates.registerApplicationSuspensionDelegate(
            object : ApplicationSuspensionDelegate {
                override fun suspendToHome() {
                    runOnUiThread { moveTaskToBack(true) }
                }
            },
        )

        enableEdgeToEdge()
        setContent {
            PantherTheme {
                CompatibilityFrame {
                    IndicatesNetworkActivity {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(LocalPantherColors.current.background),
                        ) {
                            RootWindow { RootView() }
                            RootSheetHost()
                            AlertHost()
                            OverlayHost()
                            HUDHost()
                            RootOverlayView()
                            ToastHost()
                            ShakeDetector { if (Build.isDeveloperModeEnabled) DevModeService.presentActionSheet() }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val pendingChat = pendingChat(intent) ?: return
        try {
            notificationService.respondToNotificationTap(
                pendingChat.conversationIDKey,
                focusedMessageID = pendingChat.focusedMessageID,
                recipientUserID = pendingChat.recipientUserID,
            )
        } catch (exception: Exception) {
            Logger.log(
                exception,
                domain = LoggerDomain.notifications,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            ClientSession.store.flushNow()
            uiCacheInvalidationService.refreshNotificationExtensionNameMap()
            analyticsService.logEvent(AnalyticsService.AnalyticsEvent.TERMINATE_APP)
        }
    }

    private fun pendingChat(intent: Intent?): PendingChatNavigation.PendingChat? {
        val conversationIDKey = intent?.getStringExtra(PendingChatNavigation.CONVERSATION_ID_KEY_EXTRA) ?: return null
        return PendingChatNavigation.PendingChat(
            conversationIDKey,
            focusedMessageID = intent.getStringExtra(PendingChatNavigation.REACTION_MESSAGE_ID_EXTRA),
            recipientUserID = intent.getStringExtra(PendingChatNavigation.RECIPIENT_USER_ID_EXTRA),
        )
    }

    private companion object {
        /**
         * Whether this process has already created the activity
         * once, distinguishing config changes from process
         * restarts.
         */
        @Volatile
        private var hasCreatedInProcess = false
    }
}
