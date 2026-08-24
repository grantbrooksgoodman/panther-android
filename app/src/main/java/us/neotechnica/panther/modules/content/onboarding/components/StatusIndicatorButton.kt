//
//  StatusIndicatorButton.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.constants.StatusIndicatorButtonColors
import us.neotechnica.panther.modules.content.onboarding.constants.StatusIndicatorButtonFloats
import us.neotechnica.panther.modules.content.onboarding.constants.StatusIndicatorButtonStrings

/**
 * A capsule button that requests a permission and reflects its
 * granted/denied status, ported from the iOS `StatusIndicatorButton`.
 *
 * While undetermined ([isGranted] `null`) the button is a blue,
 * tappable capsule with a white label and an orange "?" status circle.
 * Once resolved it is disabled: a light-gray capsule with a gray label
 * and a green check (granted) or red cross (denied) status circle.
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
    val isDetermined = isGranted != null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(CircleShape)
                .background(if (isDetermined) colors.groupedContentBackground else StatusIndicatorButtonColors.foreground)
                .then(if (isDetermined) Modifier else Modifier.clickable(onClick = onClick))
                .padding(
                    horizontal = StatusIndicatorButtonFloats.horizontalPadding,
                    vertical = StatusIndicatorButtonFloats.verticalPadding,
                ),
    ) {
        StatusCircle(isGranted)
        Spacer(Modifier.width(StatusIndicatorButtonFloats.iconTrailingPadding))
        Components.Text(
            label,
            color = if (isDetermined) colors.subtitleText else Color.White,
            font = Font.systemBold(FontScale.Custom(StatusIndicatorButtonFloats.LABEL_FONT_SIZE)),
        )
    }
}

@Composable
private fun StatusCircle(isGranted: Boolean?) {
    val fillColor =
        when (isGranted) {
            true -> StatusIndicatorButtonColors.grantedStatusForeground
            false -> StatusIndicatorButtonColors.deniedStatusForeground
            null -> StatusIndicatorButtonColors.undeterminedStatusForeground
        }
    val glyphSize = StatusIndicatorButtonFloats.glyphSize
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(StatusIndicatorButtonFloats.circleSize).clip(CircleShape).background(fillColor),
    ) {
        when (isGranted) {
            true -> Components.Symbol("checkmark", color = Color.White, modifier = Modifier.size(glyphSize))
            false -> Components.Symbol("xmark", color = Color.White, modifier = Modifier.size(glyphSize))
            null ->
                Components.Text(
                    StatusIndicatorButtonStrings.UNDETERMINED_GLYPH,
                    color = Color.White,
                    font = Font.systemBold(),
                )
        }
    }
}
