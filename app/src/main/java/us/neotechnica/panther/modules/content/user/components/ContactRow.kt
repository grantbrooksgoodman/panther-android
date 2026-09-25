//
//  ContactRow.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ContactRowFloats

/**
 * A tappable contact row: a circular avatar (the contact's initials, or a
 * person glyph when none) followed by the contact's name and an optional
 * annotation.
 *
 * @param name The contact's display name.
 * @param initials The contact's initials, or blank for the person glyph.
 * @param onClick Invoked when the row is tapped.
 * @param modifier The modifier for this row.
 * @param enabled Whether the row responds to taps; when `false`, the row
 *   is dimmed and ignores taps.
 * @param annotation A trailing note shown after the name, or `null` for
 *   none.
 */
@Composable
@Suppress("LongParameterList")
fun ContactRow(
    name: String,
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    annotation: String? = null,
) {
    val colors = LocalPantherColors.current
    val foregroundColor = if (enabled) colors.titleText else colors.disabled
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = ContactRowFloats.horizontalPadding, vertical = ContactRowFloats.verticalPadding),
    ) {
        AvatarImageView(
            modifier = Modifier.size(ContactRowFloats.avatarSize),
            initials = initials,
            glyphSize = ContactRowFloats.avatarGlyphSize,
            initialsFont = Font.systemSemibold(FontScale.Small),
        )
        Components.Text(
            name,
            color = foregroundColor,
            font = Font.systemSemibold(),
            modifier = Modifier.padding(start = ContactRowFloats.nameStartPadding),
        )
        annotation?.let {
            Components.Text(
                it,
                color = foregroundColor,
                modifier = Modifier.padding(start = ContactRowFloats.annotationStartPadding),
            )
        }
    }
}
