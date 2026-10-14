//
//  ContactDetailView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.DisclosureIndicator
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.constants.StackFloats
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ContactDetailViewColors
import us.neotechnica.panther.modules.content.user.constants.ContactDetailViewFloats

// MARK: - Constants Accessors

private typealias DetailColors = ContactDetailViewColors
private typealias DetailFloats = ContactDetailViewFloats

/**
 * A row displaying a contact's avatar, title, and optional subtitle.
 *
 * Use [ContactDetailView] to summarize a contact in a list. When a
 * subtitle is provided, a chevron indicates that the row leads to
 * further detail.
 *
 * @param titleLabelText The text the title label displays.
 * @param subtitleLabelText The text the subtitle label displays. Pass
 *   `null` to omit the subtitle and chevron.
 * @param image The avatar image to display, or `null` to show a
 *   placeholder symbol.
 * @param modifier The modifier for this view.
 */
@Composable
fun ContactDetailView(
    titleLabelText: String,
    subtitleLabelText: String?,
    image: Bitmap?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val background =
        if (ThemeService.isDarkModeActive(isSystemInDarkTheme())) DetailColors.darkBackground else DetailColors.lightBackground

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StackFloats.defaultSpacing),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DetailFloats.cornerRadius))
                .background(background)
                .padding(DetailFloats.padding),
    ) {
        AvatarImageView(
            remember(image) { image?.asImageBitmap() },
            modifier = Modifier.padding(end = DetailFloats.avatarImageViewTrailingPadding),
        )

        Column {
            Components.Text(
                titleLabelText,
                foregroundColor = colors.titleText,
                font = Font.systemSemibold(),
            )

            if (!subtitleLabelText.isNullOrBlank()) {
                Components.Text(
                    subtitleLabelText,
                    foregroundColor = colors.titleText,
                    font = Font.system(FontScale.Small),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (subtitleLabelText != null) {
            DisclosureIndicator(foregroundColor = colors.subtitleText)
        }
    }
}
