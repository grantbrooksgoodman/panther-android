//
//  DeliveryProgressView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.DeliveryProgressIndicatorColors
import us.neotechnica.panther.modules.content.user.constants.DeliveryProgressIndicatorFloats

// MARK: - Constants Accessors

private typealias Colors = DeliveryProgressIndicatorColors
private typealias Floats = DeliveryProgressIndicatorFloats

/**
 * The thin delivery progress bar shown beneath the chat header while
 * a message is being sent.
 *
 * @param progress The fraction filled, from `0` to `1`.
 * @param alpha The bar's opacity, from `0` (hidden) to `1` (shown).
 * @param modifier The modifier for this bar.
 */
@Composable
fun DeliveryProgressView(
    progress: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val tint = if (ThemeService.isAppDefaultThemeApplied) Colors.progressBarTint else LocalPantherColors.current.accent
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(Floats.viewFrameHeight)
                .alpha(alpha)
                .background(tint.copy(alpha = TRACK_ALPHA)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(tint),
        )
    }
}

private const val TRACK_ALPHA = 0.24f
