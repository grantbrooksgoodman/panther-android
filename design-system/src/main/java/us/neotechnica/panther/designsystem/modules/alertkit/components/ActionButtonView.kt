//
//  ActionButtonView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.neotechnica.panther.designsystem.modules.alertkit.constants.ActionButtonViewFloats
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.AlertKitPalette
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font

// MARK: - Constants Accessors

private typealias ButtonFloats = ActionButtonViewFloats

// A button for an alert or action sheet action, with a centered,
// single-line label that shrinks to fit. Destructive labels are red
// and disabled labels are muted. The button highlights while pressed.
@Composable
internal fun ActionButtonView(
    action: Action,
    font: Font,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val palette = AlertKitPalette.current

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .background(if (isPressed) palette.highlight else Color.Transparent)
                .clickable(
                    enabled = isEnabled,
                    indication = null,
                    interactionSource = interactionSource,
                    onClick = onClick,
                ).padding(horizontal = ButtonFloats.LABEL_HORIZONTAL_PADDING.dp),
    ) {
        Text(
            action.title.sanitized,
            autoSize =
                TextAutoSize.StepBased(
                    maxFontSize = font.scale.points.sp,
                    minFontSize = ButtonFloats.LABEL_MINIMUM_FONT_SIZE.sp,
                ),
            color =
                when {
                    !isEnabled -> palette.actionLabelDisabled
                    action.style.isDestructive -> palette.actionLabelDestructive
                    else -> palette.actionLabel
                },
            maxLines = 1,
            style = font.textStyle,
        )
    }
}
