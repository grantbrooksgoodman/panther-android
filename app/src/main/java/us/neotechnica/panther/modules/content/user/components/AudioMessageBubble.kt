//
//  AudioMessageBubble.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 03/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.networking.modules.schema.message.models.AudioMessageReference
import java.util.Locale

/**
 * An audio message bubble: a play/pause button, a playback-progress bar,
 * and a duration label, mirroring the iOS `AudioMessageCell`.
 *
 * The label shows the total duration when idle and the elapsed time while
 * playing. Playback itself (driving [isPlaying] and [progress]) lands with
 * the playback service (Phase R4.3).
 *
 * @param reference The message's resolved audio.
 * @param isOwn Whether the message was sent by the current user.
 * @param isPlaying Whether the message is currently being played.
 * @param progress The playback progress, in `0.0...1.0`.
 * @param onPlay Invoked when the play/pause button is tapped.
 */
@Composable
fun AudioMessageBubble(
    reference: AudioMessageReference,
    isOwn: Boolean,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    onPlay: () -> Unit = {},
) {
    val colors = LocalPantherColors.current
    val displayedAudio = if (isOwn) reference.original else reference.translated
    val duration = displayedAudio.contentDuration ?: 0f
    val labelSeconds = if (isPlaying && duration > 0f) progress * duration else duration

    val bubbleColor = if (isOwn) colors.senderBubble else colors.receiverBubble
    val glyphColor = if (isOwn) Color.White else colors.titleText
    val shape =
        RoundedCornerShape(
            topStart = BUBBLE_RADIUS,
            topEnd = BUBBLE_RADIUS,
            bottomStart = if (isOwn) BUBBLE_RADIUS else BUBBLE_TAIL_RADIUS,
            bottomEnd = if (isOwn) BUBBLE_TAIL_RADIUS else BUBBLE_RADIUS,
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SPACING),
        modifier =
            Modifier
                .width(BUBBLE_WIDTH)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(PLAY_BUTTON_SIZE)
                    .clip(CircleShape)
                    .background(glyphColor.copy(alpha = PLAY_BUTTON_ALPHA))
                    .clickable(onClick = onPlay)
                    .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
        ) {
            Components.Symbol(
                if (isPlaying) "pause.fill" else "play.fill",
                color = bubbleColor,
                modifier = Modifier.size(GLYPH_SIZE),
            )
        }

        LinearProgressIndicator(
            progress = { if (duration > 0f) progress.coerceIn(0f, 1f) else 0f },
            color = glyphColor,
            trackColor = glyphColor.copy(alpha = TRACK_ALPHA),
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(PROGRESS_RADIUS)),
        )

        Components.Text(durationString(labelSeconds), color = glyphColor, font = Font.system(FontScale.Small))
    }
}

private fun durationString(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", total / SECONDS_PER_MINUTE, total % SECONDS_PER_MINUTE)
}

private val BUBBLE_WIDTH = 240.dp
private val BUBBLE_RADIUS = 18.dp
private val BUBBLE_TAIL_RADIUS = 4.dp
private val SPACING = 10.dp
private val HORIZONTAL_PADDING = 12.dp
private val VERTICAL_PADDING = 10.dp
private val PLAY_BUTTON_SIZE = 34.dp
private val GLYPH_SIZE = 18.dp
private val PROGRESS_RADIUS = 3.dp
private const val PLAY_BUTTON_ALPHA = 0.9f
private const val TRACK_ALPHA = 0.3f
private const val SECONDS_PER_MINUTE = 60
