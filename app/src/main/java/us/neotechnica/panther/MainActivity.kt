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
import us.neotechnica.panther.designsystem.modules.foundation.overlay.BuildInfoOverlayView
import us.neotechnica.panther.designsystem.modules.foundation.overlay.OverlayHost
import us.neotechnica.panther.designsystem.modules.foundation.toast.ToastHost
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.designsystem.modules.theming.views.PantherTheme
import us.neotechnica.panther.modules.content.shared.views.ForcedUpdateView
import us.neotechnica.panther.navigation.PendingChatNavigation
import us.neotechnica.panther.navigation.RootView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        capturePendingChat(intent)
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

    private fun capturePendingChat(intent: Intent?) {
        PendingChatNavigation.set(intent?.getStringExtra(PendingChatNavigation.CONVERSATION_ID_KEY_EXTRA))
    }
}
