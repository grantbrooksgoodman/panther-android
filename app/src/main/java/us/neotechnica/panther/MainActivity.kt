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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.alertkit.views.AlertHost
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.designsystem.modules.theming.views.PantherTheme
import us.neotechnica.panther.modules.demo.views.databasedemoview.DatabaseDebugView
import us.neotechnica.panther.modules.demo.views.TranslationDemoView
import us.neotechnica.panther.modules.demo.views.GalleryView

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
                    RootContent(modifier = Modifier.systemBarsPadding())
                    AlertHost()
                }
            }
        }
    }
}

private enum class DemoScreen {
    GALLERY,
    TRANSLATION,
    DATABASE,
}

@Composable
private fun RootContent(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf(DemoScreen.GALLERY) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            TabButton("Design System", screen == DemoScreen.GALLERY) { screen = DemoScreen.GALLERY }
            TabButton("Translation", screen == DemoScreen.TRANSLATION) { screen = DemoScreen.TRANSLATION }
            TabButton("Database", screen == DemoScreen.DATABASE) { screen = DemoScreen.DATABASE }
        }

        when (screen) {
            DemoScreen.GALLERY -> GalleryView(modifier = Modifier.fillMaxWidth())
            DemoScreen.TRANSLATION -> TranslationDemoView(modifier = Modifier.fillMaxWidth())
            DemoScreen.DATABASE -> DatabaseDebugView(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Components.Button(
        text = label,
        color = if (isSelected) colors.accent else colors.subtitleText,
        onClick = onClick,
        font = Font.systemSemibold(FontScale.Small),
    )
}
