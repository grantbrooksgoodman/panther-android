//
//  BuildInfoOverlayView.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.BuildInfoOverlay
import androidx.compose.material3.Text as Material3Text

/**
 * A persistent diagnostic banner showing the build's code name,
 * version, build number, milestone, and revision, plus live memory
 * usage. Mirrors the iOS `BuildInfoOverlayView`.
 *
 * The overlay is shown for prerelease milestones only (never in a
 * general-release build). Tapping it reveals the full build details;
 * long-pressing hides it, and the hidden state persists.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun BuildInfoOverlayView(modifier: Modifier = Modifier) {
    if (!Build.isConfigured || Build.milestone == Milestone.GENERAL_RELEASE) return

    val isHidden by BuildInfoOverlay.isHidden.collectAsState()
    if (isHidden) return

    var showDetails by remember { mutableStateOf(false) }
    var statsText by remember { mutableStateOf(CALCULATING_TEXT) }

    LaunchedEffect(Unit) {
        while (true) {
            val runtime = Runtime.getRuntime()
            val usedMegabytes = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MEGABYTE
            statsText = "$usedMegabytes MB in use"
            delay(STATS_REFRESH_MILLIS)
        }
    }

    Column(
        horizontalAlignment = Alignment.End,
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showDetails = true },
                        onLongPress = { BuildInfoOverlay.hide() },
                    )
                }.padding(4.dp),
    ) {
        Components.Text(
            statsText,
            color = Color.White,
            font = Font.system(FontScale.Custom(STATS_FONT_SIZE)),
            modifier = Modifier.background(Color.Black).padding(horizontal = 4.dp, vertical = 1.dp),
        )
        Components.Text(
            Build.buildInfoString,
            color = Color.White,
            font = Font.systemBold(FontScale.Custom(BUILD_INFO_FONT_SIZE)),
            modifier = Modifier.background(Color.Black).padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }

    if (showDetails) {
        BuildInfoDetailsDialog(onDismiss = { showDetails = false })
    }
}

@Composable
private fun BuildInfoDetailsDialog(onDismiss: () -> Unit) {
    val details =
        listOf(
            "Milestone" to Build.milestone.rawValue.replaceFirstChar { it.uppercase() },
            "Bundle Version" to "${Build.bundleVersion} (${Build.buildNumber})",
            "Revision" to "${Build.bundleRevision} (${Build.revisionBuildNumber})",
            "SKU" to Build.buildSKU,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Material3Text("Project ${Build.codeName}", style = Font.systemBold(FontScale.Medium).textStyle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                details.forEach { (label, value) ->
                    Column {
                        Material3Text(label, style = Font.systemBold(FontScale.Small).textStyle)
                        Material3Text(value, style = Font.system(FontScale.Small).textStyle)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Material3Text("Dismiss") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    BuildInfoOverlay.hide()
                    onDismiss()
                },
            ) { Material3Text("Hide Overlay") }
        },
    )
}

private const val CALCULATING_TEXT = "Calculating…"
private const val BYTES_PER_MEGABYTE = 1_048_576L
private const val STATS_REFRESH_MILLIS = 1_000L
private const val STATS_FONT_SIZE = 10f
private const val BUILD_INFO_FONT_SIZE = 11f
