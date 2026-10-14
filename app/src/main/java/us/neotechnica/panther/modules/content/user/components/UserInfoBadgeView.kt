//
//  UserInfoBadgeView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.models.FlagImage
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.modules.content.user.constants.UserInfoBadgeViewColors
import us.neotechnica.panther.modules.content.user.constants.UserInfoBadgeViewFloats
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import androidx.compose.material3.Text as Material3Text

// MARK: - Constants Accessors

private typealias BadgeColors = UserInfoBadgeViewColors
private typealias BadgeFloats = UserInfoBadgeViewFloats

/**
 * A badge displaying a user's language code and regional flag.
 *
 * Use [UserInfoBadgeView] alongside a user's name to indicate their
 * language and region. The flag is resolved from the user's region
 * code, falling back to their language code; when given an action, the
 * badge becomes a button.
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
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val bodyColor = if (isDarkModeActive) BadgeColors.bodyDarkForeground else BadgeColors.bodyLightForeground

    val context = LocalContext.current
    val density = LocalDensity.current
    val flagImage =
        remember(user.phoneNumber.regionCode, user.languageCode) {
            DependencyValues.current.commonServices.regionDetail.image(
                RegionDetailService.QueryStrategy.RegionCode(user.phoneNumber.regionCode),
            )
                ?: FlagImage.named(user.languageCode, context)
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .widthIn(max = BadgeFloats.bodyMaxWidth)
                .heightIn(max = BadgeFloats.bodyMaxHeight)
                .fillMaxSize()
                .clip(RoundedCornerShape(BadgeFloats.bodyCornerRadius))
                .background(bodyColor)
                .then(if (action != null) Modifier.clickable(onClick = action) else Modifier),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BadgeFloats.labelViewHStackSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The label's frame is narrower than its glyphs, so the text is
            // drawn unclipped and centred over the frame.
            // The opacity modulates each draw rather than compositing
            // offscreen, which would clip the shadow to the text bounds.
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(width = BadgeFloats.labelViewTextFrameWidth, height = BadgeFloats.labelViewTextFrameHeight)
                        .wrapContentSize(unbounded = true)
                        .graphicsLayer {
                            alpha = BadgeFloats.LABEL_VIEW_TEXT_OPACITY
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        },
            ) {
                Material3Text(
                    text = user.languageCode.uppercase(),
                    color = colors.titleText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    style =
                        Font.systemSemibold(FontScale.Small).textStyle.copy(
                            shadow =
                                Shadow(
                                    color = BadgeColors.labelViewTextShadow,
                                    blurRadius = with(density) { BadgeFloats.labelViewTextShadowRadius.toPx() },
                                ),
                        ),
                )
            }

            Box(
                modifier =
                    Modifier
                        .size(width = BadgeFloats.labelViewImageFrameWidth, height = BadgeFloats.labelViewImageFrameHeight)
                        .clip(RoundedCornerShape(BadgeFloats.labelViewImageCornerRadius)),
            ) {
                flagImage?.let {
                    Image(
                        painter = painterResource(it.resourceID),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier =
                            Modifier.size(
                                width = BadgeFloats.labelViewImageFrameWidth,
                                height = BadgeFloats.labelViewImageFrameHeight,
                            ),
                    )
                }
            }
        }
    }
}
