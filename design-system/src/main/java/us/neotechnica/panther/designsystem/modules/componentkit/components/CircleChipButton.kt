//
//  CircleChipButton.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

/**
 * A circular, elevated icon button on a background-colored chip — the
 * navigation-bar affordance shared across the app's headers (back, done,
 * close, settings, new conversation).
 *
 * @param systemName The SF Symbol name of the glyph.
 * @param contentDescription The button's accessibility label.
 * @param onClick Invoked when the button is tapped.
 * @param modifier The modifier for this button.
 * @param tint The glyph tint; defaults to the accent color.
 * @param glyphSize The glyph's size.
 * @param enabled Whether the button responds to taps.
 */
@Composable
@Suppress("LongParameterList")
fun CircleChipButton(
    systemName: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalPantherColors.current.accent,
    glyphSize: Dp = 22.dp,
    enabled: Boolean = true,
) {
    val colors = LocalPantherColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(CHIP_SIZE)
                .shadow(CHIP_SHADOW_ELEVATION, CircleShape)
                .clip(CircleShape)
                .background(colors.background)
                .clickable(enabled = enabled, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
    ) {
        Components.Symbol(systemName, color = tint, modifier = Modifier.size(glyphSize))
    }
}

private val CHIP_SIZE = 40.dp
private val CHIP_SHADOW_ELEVATION = 2.dp
