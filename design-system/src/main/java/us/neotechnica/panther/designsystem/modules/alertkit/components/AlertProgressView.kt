//
//  AlertProgressView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import us.neotechnica.panther.designsystem.modules.alertkit.constants.AlertProgressViewFloats
import us.neotechnica.panther.designsystem.modules.alertkit.models.AlertKitPalette

// MARK: - Constants Accessors

private typealias ProgressFloats = AlertProgressViewFloats

// A rounded progress bar for a progress alert, set in space reserved
// below the alert's message and pinned a fixed distance above the
// alert's bottom edge – or above its cancel button, when present. The
// bar animates to each new value of the given progress.
@Composable
internal fun AlertProgressView(
    progress: StateFlow<Double>,
    hasCancelAction: Boolean,
) {
    val fractionCompleted by progress.collectAsState()
    val animatedFraction by animateFloatAsState(
        animationSpec =
            tween(
                (ProgressFloats.PROGRESS_ANIMATION_DURATION * MILLISECONDS_PER_SECOND).toInt(),
            ),
        label = "fractionCompleted",
        targetValue = fractionCompleted.toFloat(),
    )

    val palette = AlertKitPalette.current
    val reservedHeight =
        if (hasCancelAction) ProgressFloats.RESERVED_HEIGHT_WITH_CANCEL_ACTION else ProgressFloats.RESERVED_HEIGHT
    val bottomInset =
        if (hasCancelAction) ProgressFloats.BAR_BOTTOM_INSET_WITH_CANCEL_ACTION else ProgressFloats.BAR_BOTTOM_INSET

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(reservedHeight.dp)
                .padding(horizontal = ProgressFloats.HORIZONTAL_INSET.dp),
    ) {
        // Above a cancel button, the bar extends above the reserved
        // space.
        Box(
            modifier =
                Modifier
                    .offset(y = (reservedHeight - bottomInset - ProgressFloats.BAR_HEIGHT).dp)
                    .fillMaxWidth()
                    .height(ProgressFloats.BAR_HEIGHT.dp)
                    .clip(CircleShape)
                    .background(palette.progressTrack),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedFraction)
                        .clip(CircleShape)
                        .background(palette.progressFill),
            )
        }
    }
}

private const val MILLISECONDS_PER_SECOND = 1_000f
