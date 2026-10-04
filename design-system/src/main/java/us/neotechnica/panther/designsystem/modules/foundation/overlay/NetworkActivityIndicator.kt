//
//  NetworkActivityIndicator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A small circular indicator that reflects in-flight network activity. The networking module
 * has no Compose dependency, so the activity state and colors are
 * supplied by the caller (see `NetworkActivityIndicatorService`).
 *
 * @param isActive Whether any network operation is in flight.
 * @param backgroundColor The background color, or `null` to adopt a
 *   system-blue default.
 * @param progressViewTintColor The tint of the progress spinner.
 * @param onTap The action performed when the indicator is tapped.
 * @param modifier The modifier for this indicator.
 */
@Composable
fun NetworkActivityIndicator(
    isActive: Boolean,
    backgroundColor: Color?,
    progressViewTintColor: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isActive) return

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(INDICATOR_SIZE)
                .clip(CircleShape)
                .background(backgroundColor ?: DEFAULT_BACKGROUND)
                .clickable(onClick = onTap),
    ) {
        CircularProgressIndicator(
            color = progressViewTintColor,
            strokeWidth = STROKE_WIDTH,
            modifier = Modifier.size(SPINNER_SIZE),
        )
    }
}

private val INDICATOR_SIZE = 32.dp
private val SPINNER_SIZE = 18.dp
private val STROKE_WIDTH = 2.dp
private val DEFAULT_BACKGROUND = Color(0xFF007AFF)
