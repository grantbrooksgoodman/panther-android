//
//  InstructionView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.TextFit
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.designsystem.modules.theming.views.ThemedView
import us.neotechnica.panther.modules.content.onboarding.constants.InstructionViewColors
import us.neotechnica.panther.modules.content.onboarding.constants.InstructionViewFloats

// MARK: - Constants Accessors

private typealias Colors = InstructionViewColors
private typealias Floats = InstructionViewFloats

/**
 * A header that displays a title and subtitle introducing an
 * onboarding page.
 *
 * Use `InstructionView` at the top of an onboarding page to tell the
 * user what the page is for and what they should do next. The view
 * renders the title and subtitle from the given
 * [InstructionViewStrings] value, leading-aligned and constrained to
 * the leading half of the screen's width.
 *
 * **Note:** The view displays the given strings verbatim. Perform any
 * localization before creating the [InstructionViewStrings] value.
 *
 * @param strings The title and subtitle text to display.
 * @param modifier The modifier for this view.
 */
@Composable
fun InstructionView(
    strings: InstructionViewStrings,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val halfOfScreenWidth = (LocalConfiguration.current.screenWidthDp / Floats.SCREEN_WIDTH_DIVISOR).dp

    ThemedView {
        Row(modifier = modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier =
                    Modifier
                        .width(halfOfScreenWidth)
                        .heightIn(max = Floats.frameMaxHeight)
                        .padding(start = Floats.leadingPadding, top = Floats.topPadding),
            ) {
                Components.FittedText(
                    strings.titleLabelText,
                    foregroundColor = colors.titleText,
                    fit = TextFit(minimumScaleFactor = Floats.TITLE_LABEL_MINIMUM_SCALE_FACTOR),
                    font = Font.systemBold(FontScale.Large),
                    modifier = Modifier.padding(bottom = Floats.titleLabelBottomPadding),
                )

                Components.FittedText(
                    strings.subtitleLabelText,
                    foregroundColor = Colors.subtitleLabelForeground,
                    fit = TextFit(minimumScaleFactor = Floats.SUBTITLE_LABEL_MINIMUM_SCALE_FACTOR),
                    font = Font.system(FontScale.Custom(Floats.SUBTITLE_LABEL_FONT_SIZE)),
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
