//
//  StatusIndicatorButton.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import us.neotechnica.panther.modules.content.onboarding.constants.StatusIndicatorButtonColors
import us.neotechnica.panther.modules.content.onboarding.constants.StatusIndicatorButtonFloats
import us.neotechnica.panther.modules.content.onboarding.constants.StatusIndicatorButtonStrings

// MARK: - Constants Accessors

private typealias IndicatorColors = StatusIndicatorButtonColors
private typealias IndicatorFloats = StatusIndicatorButtonFloats
private typealias IndicatorStrings = StatusIndicatorButtonStrings

/**
 * A capsule-shaped button that displays the status of an onboarding
 * task alongside a text label.
 *
 * Use `StatusIndicatorButton` to represent a task the user must
 * complete during onboarding, such as granting a system permission. The
 * button pairs a status symbol with the given text and runs an action
 * when tapped.
 *
 * The button's appearance and interactivity derive from a tri-state
 * completion value:
 *
 * - `null` indicates the task's status is undetermined. The button is
 *   enabled, and tapping it runs the given action.
 * - `true` indicates the task completed successfully. The button is
 *   disabled and displays a confirmation symbol.
 * - `false` indicates the task failed or was declined. The button is
 *   disabled and displays a failure symbol.
 *
 * **Important:** Once the completion value becomes non-`null`, the
 * button no longer responds to taps. To let the user retry a failed
 * task, reset the value to `null`.
 *
 * @param text The text to display as the button's label.
 * @param isCompleted The completion state of the task the button
 *   represents. Pass `null` while the status is undetermined; pass
 *   `true` or `false` once the task has resolved. Any non-`null` value
 *   disables the button.
 * @param modifier The modifier for this button.
 * @param action The action to perform when the user taps the button.
 *   The button ignores taps while [isCompleted] is non-`null`.
 */
@Composable
fun StatusIndicatorButton(
    text: String,
    isCompleted: Boolean?,
    modifier: Modifier = Modifier,
    action: () -> Unit,
) {
    val isDetermined = isCompleted != null

    val backgroundColor =
        if (isDetermined) {
            IndicatorColors.foreground.copy(alpha = IndicatorFloats.DISABLED_ALPHA)
        } else {
            IndicatorColors.foreground
        }

    val labelForegroundColor =
        if (isDetermined) {
            IndicatorColors.determinedStatusLabelForeground
        } else {
            IndicatorColors.undeterminedStatusLabelForeground
        }

    val imageSecondaryForegroundColor =
        when (isCompleted) {
            null -> IndicatorColors.undeterminedStatusImageSecondaryForeground
            true -> IndicatorColors.grantedStatusImageSecondaryForeground
            false -> IndicatorColors.deniedStatusImageSecondaryForeground
        }

    val imageSystemName =
        when (isCompleted) {
            null -> IndicatorStrings.UNDETERMINED_STATUS_IMAGE_SYSTEM_NAME
            true -> IndicatorStrings.GRANTED_STATUS_IMAGE_SYSTEM_NAME
            false -> IndicatorStrings.DENIED_STATUS_IMAGE_SYSTEM_NAME
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(enabled = !isDetermined, onClick = action)
                .padding(horizontal = IndicatorFloats.horizontalPadding, vertical = IndicatorFloats.verticalPadding),
    ) {
        Components.Symbol(
            imageSystemName,
            foregroundColor = Color.White,
            secondaryForegroundColor = imageSecondaryForegroundColor,
            usesIntrinsicSize = false,
            modifier =
                Modifier
                    .width(IndicatorFloats.imageFrameWidth)
                    .height(IndicatorFloats.imageFrameHeight)
                    .padding(end = IndicatorFloats.imageTrailingPadding),
        )

        Components.Text(
            text,
            foregroundColor = labelForegroundColor,
            font = Font.systemBold(FontScale.Custom(IndicatorFloats.LABEL_FONT_SIZE)),
        )
    }
}
