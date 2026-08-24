//
//  ContactRow.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

/**
 * A tappable contact row: a circular avatar (the contact's initials, or a
 * person glyph when none) followed by the contact's name.
 *
 * @param name The contact's display name.
 * @param initials The contact's initials, or blank for the person glyph.
 * @param onClick Invoked when the row is tapped.
 * @param modifier The modifier for this row.
 */
@Composable
fun ContactRow(
    name: String,
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(AVATAR_SIZE).clip(CircleShape).background(AVATAR_BACKGROUND),
        ) {
            if (initials.isNotBlank()) {
                Components.Text(initials, color = colors.background, font = Font.systemSemibold(FontScale.Small))
            } else {
                Components.Symbol("person", color = colors.background, modifier = Modifier.size(AVATAR_GLYPH_SIZE))
            }
        }
        Components.Text(
            name,
            color = colors.titleText,
            font = Font.systemSemibold(),
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private val AVATAR_SIZE = 40.dp
private val AVATAR_GLYPH_SIZE = 22.dp
private val AVATAR_BACKGROUND = Color(0xFFC7C7CC)
