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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.DeliveryProgressIndicatorColors
import us.neotechnica.panther.modules.content.user.constants.DeliveryProgressIndicatorFloats
import us.neotechnica.panther.modules.content.user.services.DeliveryProgressIndicatorService
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

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

/**
 * Creates and registers a [DeliveryProgressIndicatorService] bound
 * to the current composition, tearing it down when the composition
 * leaves.
 *
 * @return The registered delivery progress indicator service.
 */
@Composable
internal fun rememberRegisteredDeliveryProgressIndicatorService(): DeliveryProgressIndicatorService {
    val scope = rememberCoroutineScope()
    val service = remember { DeliveryProgressIndicatorService(scope) }
    DisposableEffect(service) {
        DependencyValues.current.clientSession.registerDeliveryProgressIndicator(service)
        onDispose { service.teardown() }
    }
    return service
}

/** Returns whether any outbox entry is currently sending. */
internal fun anyOutboxSending(): Boolean = MessageOutboxService.allEntries.any { it.state == OutboxEntry.State.SENDING }

private const val TRACK_ALPHA = 0.24f
