//
//  StatusIndicatorButton.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

/**
 * A capsule button that requests a permission and reflects its
 * granted/denied status, ported from the iOS `StatusIndicatorButton`.
 *
 * @param label The button label.
 * @param isGranted `true` if granted, `false` if denied, `null` if not
 *   yet requested.
 * @param onClick The action to perform when tapped.
 * @param modifier The modifier for this button.
 */
@Composable
fun StatusIndicatorButton(
    label: String,
    isGranted: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(RoundedCornerShape(CORNER_RADIUS))
                .background(colors.groupedContentBackground)
                .border(1.dp, colors.disabled, RoundedCornerShape(CORNER_RADIUS))
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Components.Text(label, color = colors.titleText, font = Font.systemMedium())

        when (isGranted) {
            true -> Components.Symbol("checkmark", color = colors.accent, modifier = Modifier.size(INDICATOR_SIZE))
            false -> Components.Symbol("xmark", color = colors.disabled, modifier = Modifier.size(INDICATOR_SIZE))
            null -> Unit
        }
    }
}

private val CORNER_RADIUS = 12.dp
private val INDICATOR_SIZE = 20.dp
