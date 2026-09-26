//
//  MainActivity.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import us.neotechnica.panther.designsystem.modules.alertkit.views.AlertHost
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUDHost
import us.neotechnica.panther.designsystem.modules.foundation.overlay.BuildInfoOverlayView
import us.neotechnica.panther.designsystem.modules.foundation.overlay.OverlayHost
import us.neotechnica.panther.designsystem.modules.foundation.toast.ToastHost
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.designsystem.modules.theming.views.PantherTheme
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.content.shared.views.ForcedUpdateView
import us.neotechnica.panther.navigation.PendingChatNavigation
import us.neotechnica.panther.navigation.RootView
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

class MainActivity : ComponentActivity() {
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

        enableEdgeToEdge()
        setContent {
            PantherTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(LocalPantherColors.current.background),
                ) {
                    RootView()
                    AlertHost()
                    ToastHost()
                    OverlayHost()
                    HUDHost()
                    BuildInfoOverlayView(
                        modifier =
                            Modifier
                                .zIndex(1f)
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 32.dp),
                    )
                    ForcedUpdateView()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        capturePendingChat(intent)
    }

    // Approximates applicationWillTerminate; the store flush lands with the
    // session store in a later phase.
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.TERMINATE_APP)
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
