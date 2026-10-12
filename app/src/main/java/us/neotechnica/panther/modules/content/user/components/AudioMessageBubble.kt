//
//  AudioMessageBubble.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.extensions.darker
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceStrings
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewStrings
import us.neotechnica.panther.modules.content.user.constants.UserContentExtensionsFloats
import us.neotechnica.panther.modules.networking.message.models.AudioFile
import us.neotechnica.panther.subsystem.modules.foundation.extensions.durationString

// MARK: - Constants Accessors

private typealias AudioItemFloats = UserContentExtensionsFloats.AudioItem
private typealias AudioDisplayColors = ChatPageViewColors.MessagesDisplayDelegate
private typealias AudioDisplayFloats = ChatPageViewFloats.MessagesDisplayDelegate
private typealias AudioDisplayStrings = ChatPageViewStrings.MessagesDisplayDelegate

/**
 * An audio message bubble: a play/stop button, a playback-progress bar,
 * and a duration label, sized to the audio item frame.
 *
 * The label shows the total duration when idle and the elapsed time
 * while playing. The current user's bubble tints its controls white
 * over a track darkened from the bubble color; a received bubble tints
 * them in the accent color.
 *
 * @param item The audio file the message plays.
 * @param isOwn Whether the message was sent by the current user.
 * @param isPlaying Whether the message is currently being played.
 * @param progress The playback progress, in `0.0...1.0`.
 * @param onPlay Invoked when the play/stop button is tapped.
 */
@Composable
fun AudioMessageBubble(
    item: AudioFile,
    isOwn: Boolean,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    onPlay: () -> Unit = {},
) {
    val colors = LocalPantherColors.current
    val duration = item.contentDuration ?: 0f
    val labelSeconds = if (isPlaying && duration > 0f) progress * duration else duration
    val labelText =
        if (duration > 0f) labelSeconds.durationString else ChatPageViewServiceStrings.AudioMessagePlayback.CELL_DEFAULT_DURATION_LABEL_TEXT

    val bubbleColor = if (isOwn) colors.senderBubble else colors.receiverBubble
    val accentColor = if (isOwn) AudioDisplayColors.audioCellProgressViewCurrentUserAccent else colors.accent
    val trackColor =
        if (isOwn) {
            bubbleColor
                .darker(by = AudioDisplayFloats.AUDIO_CELL_PROGRESS_VIEW_TRACK_TINT_COLOR_DARKENING_PERCENTAGE)
                .copy(alpha = AudioDisplayFloats.AUDIO_CELL_PROGRESS_VIEW_TRACK_TINT_COLOR_ALPHA_COMPONENT)
        } else {
            accentColor.copy(alpha = RECEIVED_TRACK_ALPHA)
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SPACING),
        modifier =
            Modifier
                .size(AudioItemFloats.sizeWidth, AudioItemFloats.sizeHeight)
                .clip(bubbleShape(isOwn))
                .background(bubbleColor)
                .padding(horizontal = HORIZONTAL_PADDING),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(PLAY_BUTTON_SIZE)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPlay,
                    ).semantics { contentDescription = if (isPlaying) "Stop" else "Play" },
        ) {
            Components.Symbol(
                if (isPlaying) AudioDisplayStrings.STOP_IMAGE_SYSTEM_NAME else AudioDisplayStrings.PLAY_IMAGE_SYSTEM_NAME,
                foregroundColor = accentColor,
                modifier = Modifier.size(GLYPH_SIZE),
            )
        }

        LinearProgressIndicator(
            progress = { if (duration > 0f) progress.coerceIn(0f, 1f) else 0f },
            color = accentColor,
            trackColor = trackColor,
            modifier = Modifier.weight(1f),
        )

        Components.Text(labelText, foregroundColor = accentColor, font = Font.system(FontScale.Small))
    }
}

private val GLYPH_SIZE = 18.dp
private val HORIZONTAL_PADDING = 8.dp
private val PLAY_BUTTON_SIZE = 25.dp
private val SPACING = 6.dp
private const val RECEIVED_TRACK_ALPHA = 0.3f
