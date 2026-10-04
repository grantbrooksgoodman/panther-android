//
//  InstructionView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.constants.InstructionViewFloats

/**
 * A leading-aligned title-and-subtitle header shown atop onboarding
 * pages, constrained to the leading half of the screen width to match
 * the iOS `InstructionView`.
 *
 * @param strings The resolved instruction strings.
 * @param modifier The modifier for this view.
 */
@Composable
fun InstructionView(
    strings: InstructionViewStrings,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current

    Row(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(InstructionViewFloats.titleSubtitleSpacing),
            modifier =
                Modifier
                    .fillMaxWidth(InstructionViewFloats.WIDTH_FRACTION)
                    .heightIn(max = InstructionViewFloats.frameMaxHeight)
                    .padding(start = InstructionViewFloats.leadingPadding, top = InstructionViewFloats.topPadding),
        ) {
            Components.Text(
                strings.titleLabelText,
                color = colors.titleText,
                font = Font.systemBold(FontScale.Large),
            )
            Components.Text(
                strings.subtitleLabelText,
                color = colors.subtitleText,
                font = Font.system(FontScale.Custom(InstructionViewFloats.SUBTITLE_LABEL_FONT_SIZE)),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
