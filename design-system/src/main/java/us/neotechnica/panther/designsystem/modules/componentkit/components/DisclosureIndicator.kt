//
//  DisclosureIndicator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.models.SFSymbol

/**
 * The chevron that trails a list row that opens another page, as a
 * list draws its disclosure indicator.
 *
 * While SF Symbols are in use, the chevron is the semibold
 * `chevron.forward` at the small scale, sized for a body-text row.
 * Otherwise, it is the nearest Material Symbol.
 *
 * @param foregroundColor The tint of the chevron.
 * @param modifier The modifier for this view.
 */
@Composable
fun DisclosureIndicator(
    foregroundColor: Color,
    modifier: Modifier = Modifier,
) {
    if (SFSymbol.resolvesToSFSymbol(SYSTEM_NAME)) {
        // The variant is drawn at the indicator's size.
        return Icon(
            imageVector = SFSymbol.imageVector(SYSTEM_NAME, FontWeight.SemiBold, SFSymbol.Scale.SMALL),
            contentDescription = null,
            modifier = modifier,
            tint = foregroundColor,
        )
    }

    Icon(
        imageVector = SFSymbol.imageVector(SYSTEM_NAME),
        contentDescription = null,
        modifier = modifier.size(MATERIAL_SYMBOL_SIZE),
        tint = foregroundColor,
    )
}

private const val SYSTEM_NAME = "chevron.forward"

// The size at which the Material chevron's glyph matches the indicator.
private val MATERIAL_SYMBOL_SIZE = 16.dp
