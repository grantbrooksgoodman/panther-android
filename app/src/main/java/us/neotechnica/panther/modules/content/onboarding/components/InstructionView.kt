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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

/**
 * A centered title-and-subtitle header shown atop onboarding pages.
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Components.Text(
            strings.titleLabelText,
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            modifier = Modifier,
            textAlign = TextAlign.Center,
        )
        Components.Text(
            strings.subtitleLabelText,
            color = colors.subtitleText,
            textAlign = TextAlign.Center,
        )
    }
}
