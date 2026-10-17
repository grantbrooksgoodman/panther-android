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
import us.neotechnica.panther.bundle.developermode.ShakeDetector
import us.neotechnica.panther.bundle.openConversationIDKey
import us.neotechnica.panther.designsystem.modules.alertkit.views.AlertHost
import us.neotechnica.panther.designsystem.modules.developermode.services.DevModeService
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
import us.neotechnica.panther.modules.content.user.services.UICacheInvalidationService
import us.neotechnica.panther.modules.session.ClientSession
import us.neotechnica.panther.navigation.PendingChatNavigation
import us.neotechnica.panther.navigation.RootView
import us.neotechnica.panther.networking.modules.common.modifiers.IndicatesNetworkActivity
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ApplicationSuspensionDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A non-null saved state on the first activity creation of a process means the process was
        // recreated after death (not a config change, which reuses the process); restore the open
        // conversation, unless a tapped notification already targets one (R6.2).
        val isProcessRestart = savedInstanceState != null && !hasCreatedInProcess
        hasCreatedInProcess = true

        capturePendingChat(intent)
        if (isProcessRestart && intent?.getStringExtra(PendingChatNavigation.CONVERSATION_ID_KEY_EXTRA) == null) {
            PendingChatNavigation.set(Persistent.string(PersistentStorageKey.openConversationIDKey))
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
                        ToastHost()
                        OverlayHost()
                        HUDHost()
                        RootOverlayView()
                        ShakeDetector { if (Build.isDeveloperModeEnabled) DevModeService.presentActionSheet() }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        capturePendingChat(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            ClientSession.store.flushNow()
            UICacheInvalidationService.refreshNotificationExtensionNameMap()
            analyticsService.logEvent(AnalyticsService.AnalyticsEvent.TERMINATE_APP)
        }
    }

    private fun capturePendingChat(intent: Intent?) {
        PendingChatNavigation.set(intent?.getStringExtra(PendingChatNavigation.CONVERSATION_ID_KEY_EXTRA))
    }

    private companion object {
        /** Whether this process has already created the activity once, distinguishing config changes from process restarts. */
        @Volatile
        private var hasCreatedInProcess = false
    }
}
