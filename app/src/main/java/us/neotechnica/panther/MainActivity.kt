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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import us.neotechnica.panther.modules.debug.views.databasedebugview.DatabaseDebugView
import us.neotechnica.panther.ui.theme.PantherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PantherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DatabaseDebugView(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
