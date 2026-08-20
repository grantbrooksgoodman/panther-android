//
//  MainActivity.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import us.neotechnica.panther.designsystem.modules.alertkit.views.AlertHost
import us.neotechnica.panther.designsystem.modules.foundation.overlay.OverlayHost
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.designsystem.modules.theming.views.PantherTheme
import us.neotechnica.panther.navigation.RootView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PantherTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(LocalPantherColors.current.background),
                ) {
                    RootView(modifier = Modifier.systemBarsPadding())
                    AlertHost()
                    OverlayHost()
                }
            }
        }
    }
}
