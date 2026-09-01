//
//  ForcedUpdateView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 01/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.views

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.extensions.isForcedUpdateRequired
import us.neotechnica.panther.networking.modules.common.services.MetadataService
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState

/**
 * Renders the blocking forced-update modal when a forced update is
 * required.
 *
 * Place a single [ForcedUpdateView] at the top of the composition
 * so it covers all content the moment the shared forced-update
 * flag becomes `true`; it consumes input so the app beneath cannot
 * be used until the user updates.
 */
@Composable
fun ForcedUpdateView() {
    val sharedState = remember { SharedState { it.isForcedUpdateRequired } }
    val isForcedUpdateRequired by sharedState.projectedValue.changes.collectAsState(initial = sharedState.wrappedValue)
    if (!isForcedUpdateRequired) return

    val colors = LocalPantherColors.current
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .clickable(indication = null, interactionSource = interactionSource) {}
                .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            "Update Required",
            color = colors.titleText,
            textAlign = TextAlign.Center,
        )

        Text(
            "A new version is required to continue. Please update to the latest version.",
            color = colors.subtitleText,
            textAlign = TextAlign.Center,
        )

        MetadataService.appShareLink?.let { appShareLink ->
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(appShareLink))) },
            ) {
                Text("Update")
            }
        }
    }
}
