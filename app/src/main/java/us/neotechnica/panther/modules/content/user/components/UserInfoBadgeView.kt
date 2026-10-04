//
//  UserInfoBadgeView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.user.constants.UserInfoBadgeViewColors
import us.neotechnica.panther.modules.content.user.constants.UserInfoBadgeViewFloats
import us.neotechnica.panther.modules.networking.user.models.User

/**
 * A badge displaying a user's language code and regional flag.
 *
 * Use [UserInfoBadgeView] alongside a user's name to indicate their
 * language and region. The flag is resolved from the user's region
 * code; when given an action, the badge becomes a button.
 *
 * @param user The user the badge describes.
 * @param action The action performed when the user taps the badge, or
 *   `null` for no action.
 */
@Composable
fun UserInfoBadgeView(
    user: User,
    action: (() -> Unit)? = null,
) {
    val colors = LocalPantherColors.current
    val isDark = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val bodyColor = if (isDark) UserInfoBadgeViewColors.bodyDarkForeground else UserInfoBadgeViewColors.bodyLightForeground

    val flag = RegionDetailService.emojiFlag(user.phoneNumber.regionCode)
    val labelText = user.languageCode.uppercase() + if (flag.isNotBlank()) " $flag" else ""

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .then(if (action != null) Modifier.clickable(onClick = action) else Modifier)
                .widthIn(max = UserInfoBadgeViewFloats.bodyMaxWidth)
                .clip(RoundedCornerShape(UserInfoBadgeViewFloats.bodyCornerRadius))
                .background(bodyColor)
                .padding(
                    horizontal = UserInfoBadgeViewFloats.labelViewHorizontalPadding,
                    vertical = UserInfoBadgeViewFloats.labelViewVerticalPadding,
                ),
    ) {
        Components.Text(labelText, color = colors.subtitleText, font = Font.systemSemibold(FontScale.Small))
    }
}
