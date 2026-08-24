//
//  MessageInputBar.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

/**
 * The message composer bar shared by the chat and new-chat pages: a
 * leading attach button, a rounded text field, and a send button that is
 * accent-filled when sendable and disabled otherwise.
 *
 * @param text The composed text.
 * @param placeholder The placeholder shown when [text] is empty.
 * @param isSending Whether a send is in progress (disables the button).
 * @param onTextChange Invoked as the text changes.
 * @param onSend Invoked when the send button is tapped.
 * @param onAttach Invoked when the leading attach button is tapped.
 * @param enabled An additional gate on the send button, beyond a
 *   non-blank, non-sending message (for example, requiring a recipient).
 * @param modifier The modifier for this bar.
 */
@Composable
@Suppress("LongParameterList")
fun MessageInputBar(
    text: String,
    placeholder: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(DIVIDER_HEIGHT)
                    .background(colors.subtitleText.copy(alpha = DIVIDER_ALPHA)),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            InputBarButton(
                symbol = "plus",
                glyphColor = colors.titleText,
                background = colors.background,
                onClick = onAttach,
                description = "Attach media",
                elevated = true,
            )

            Box(
                contentAlignment = Alignment.CenterStart,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = BUTTON_SIZE)
                        .clip(RoundedCornerShape(FIELD_RADIUS))
                        .border(
                            width = 1.dp,
                            color = colors.subtitleText.copy(alpha = FIELD_BORDER_ALPHA),
                            shape = RoundedCornerShape(FIELD_RADIUS),
                        ).padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                if (text.isEmpty()) {
                    Components.Text(placeholder, color = colors.subtitleText)
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = Font.system.textStyle.copy(color = colors.titleText),
                    cursorBrush = SolidColor(colors.accent),
                    maxLines = FIELD_MAX_LINES,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val canSend = enabled && text.isNotBlank() && !isSending
            InputBarButton(
                symbol = "arrow.up",
                glyphColor = Color.White,
                background = if (canSend) colors.accent else colors.disabled,
                onClick = onSend,
                description = "Send",
                enabled = canSend,
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun InputBarButton(
    symbol: String,
    glyphColor: Color,
    background: Color,
    onClick: () -> Unit,
    description: String,
    enabled: Boolean = true,
    elevated: Boolean = false,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(BUTTON_SIZE)
                .then(if (elevated) Modifier.shadow(2.dp, CircleShape) else Modifier)
                .clip(CircleShape)
                .background(background)
                .clickable(enabled = enabled, onClick = onClick)
                .semantics { contentDescription = description },
    ) {
        Components.Symbol(symbol, color = glyphColor, modifier = Modifier.size(GLYPH_SIZE))
    }
}

private val BUTTON_SIZE = 40.dp
private val GLYPH_SIZE = 22.dp
private val FIELD_RADIUS = 20.dp
private val DIVIDER_HEIGHT = 0.5.dp
private const val FIELD_BORDER_ALPHA = 0.3f
private const val DIVIDER_ALPHA = 0.15f
private const val FIELD_MAX_LINES = 5
