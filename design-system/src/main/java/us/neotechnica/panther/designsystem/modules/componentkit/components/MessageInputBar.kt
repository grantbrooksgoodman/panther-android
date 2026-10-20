//
//  MessageInputBar.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.constants.MessageInputBarColors
import us.neotechnica.panther.designsystem.modules.componentkit.constants.MessageInputBarFloats
import us.neotechnica.panther.designsystem.modules.componentkit.constants.MessageInputBarStrings
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.MessageInputBarImages
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Constants Accessors

private typealias Colors = MessageInputBarColors
private typealias Floats = MessageInputBarFloats
private typealias Strings = MessageInputBarStrings

/**
 * The message composer bar shared by the chat and new-chat pages:
 * a leading attach button in a fixed-width stack, a bordered
 * rounded text field, and a send button inset inside the field's
 * trailing edge.
 *
 * @param text The composed text.
 * @param placeholder The placeholder shown when [text] is empty.
 * @param isSending Whether a send is in progress. While `true`,
 *   the send button shows an activity indicator in place of its
 *   glyph, the attach button is disabled, and the text field
 *   hides its cursor.
 * @param onTextChange Invoked as the text changes.
 * @param onSend Invoked when the send button is tapped.
 * @param onAttach Invoked when the leading attach button is
 *   tapped.
 * @param attachMediaButtonSymbol The name of the symbol the
 *   attach button shows when no [images] are given.
 * @param sendButtonSymbol The name of the symbol the send button
 *   shows when no [images] are given.
 * @param isSendEnabled Whether the send button responds to taps.
 * @param isAttachEnabled Whether the attach button responds to
 *   taps.
 * @param images The images the attach and send buttons display,
 *   or `null` to show [attachMediaButtonSymbol] and
 *   [sendButtonSymbol].
 * @param focusRequester A requester that can give the text field
 *   focus, or `null`.
 * @param onFocusChanged Invoked as the text field gains or loses
 *   focus, or `null`.
 * @param onBeginEditing Invoked as editing begins – as the text
 *   field gains focus, or as a tap on the focused text field shows
 *   the hidden keyboard – or `null`. A keyboard that shows again
 *   without a tap doesn't begin editing.
 * @param keyboardOptions The software keyboard options for the
 *   text field.
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
    attachMediaButtonSymbol: String,
    sendButtonSymbol: String,
    isSendEnabled: Boolean = true,
    isAttachEnabled: Boolean = true,
    images: MessageInputBarImages? = null,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onBeginEditing: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val canSend = isSendEnabled && !isSending
    val canAttach = isAttachEnabled && !isSending

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Floats.dividerHeight)
                    .background(colors.subtitleText.copy(alpha = Floats.DIVIDER_ALPHA)),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Floats.horizontalPadding, vertical = Floats.verticalPadding),
        ) {
            // The left stack: the attach button and a fixed space.
            Box(
                modifier =
                    Modifier
                        .width(Floats.leftStackViewWidthConstant)
                        .heightIn(min = Floats.buttonSizeHeight)
                        .padding(bottom = Floats.buttonBottomPadding),
                contentAlignment = Alignment.CenterStart,
            ) {
                InputBarButton(
                    symbol = attachMediaButtonSymbol,
                    glyphColor = if (canAttach) colors.accent else colors.disabled,
                    onClick = onAttach,
                    description = Strings.ATTACH_MEDIA_BUTTON_CONTENT_DESCRIPTION,
                    enabled = canAttach,
                    image = images?.attachMediaButton,
                    highlightedImage = images?.attachMediaButtonHighlighted,
                )
            }

            // The text field, with the send button inset inside its trailing edge.
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = Floats.middleContentViewPaddingRight)
                        .heightIn(min = Floats.buttonSizeHeight)
                        .clip(RoundedCornerShape(Floats.layerCornerRadius))
                        .border(
                            width = Floats.layerBorderWidth,
                            color = Colors.inputTextViewLayerBorder,
                            shape = RoundedCornerShape(Floats.layerCornerRadius),
                        ),
            ) {
                InputTextView(
                    text = text,
                    placeholder = placeholder,
                    isSending = isSending,
                    onTextChange = onTextChange,
                    focusRequester = focusRequester,
                    onFocusChanged = onFocusChanged,
                    onBeginEditing = onBeginEditing,
                    keyboardOptions = keyboardOptions,
                )

                InputBarButton(
                    symbol = sendButtonSymbol,
                    glyphColor = if (canSend || isSending) colors.accent else colors.disabled,
                    onClick = onSend,
                    description = Strings.SEND_BUTTON_CONTENT_DESCRIPTION,
                    enabled = canSend,
                    isLoading = isSending,
                    image = images?.sendButton,
                    highlightedImage = images?.sendButtonHighlighted,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = sendButtonTrailingInset, bottom = Floats.buttonBottomPadding),
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun InputBarButton(
    symbol: String,
    glyphColor: Color,
    onClick: () -> Unit,
    description: String,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    image: Painter? = null,
    highlightedImage: Painter? = null,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(Floats.buttonSizeWidth, Floats.buttonSizeHeight)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled && !isLoading,
                    onClick = onClick,
                ).semantics { contentDescription = description },
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = glyphColor,
                strokeWidth = Floats.indicatorStrokeWidth,
                modifier = Modifier.size(Floats.indicatorSize),
            )
        } else if (image != null) {
            Image(
                painter = if (isPressed) highlightedImage ?: image else image,
                contentDescription = null,
                alpha = if (enabled) 1f else Floats.DISABLED_IMAGE_ALPHA,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Components.Symbol(symbol, foregroundColor = glyphColor, modifier = Modifier.size(Floats.glyphSize))
        }
    }
}

// The text field, inset within the bar's bordered field, and its
// placeholder while the text is empty. A tap on the focused text field
// shows the hidden keyboard, which begins editing just as a tap that
// focuses the field does.
@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongParameterList")
private fun InputTextView(
    text: String,
    placeholder: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester?,
    onFocusChanged: ((Boolean) -> Unit)?,
    onBeginEditing: (() -> Unit)?,
    keyboardOptions: KeyboardOptions,
) {
    val colors = LocalPantherColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnBeginEditing by rememberUpdatedState(onBeginEditing)
    val isImeVisible by rememberUpdatedState(WindowInsets.isImeVisible)
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        var pressShowsKeyboard = false
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressShowsKeyboard = isFocused && !isImeVisible
                is PressInteraction.Release -> if (pressShowsKeyboard) currentOnBeginEditing?.invoke()
            }
        }
    }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = Floats.textContainerInsetLeft,
                    end = Floats.textContainerInsetRightIncrement + Floats.buttonSizeWidth,
                    top = Floats.textContainerInsetVertical,
                    bottom = Floats.textContainerInsetVertical,
                ),
    ) {
        if (text.isEmpty()) {
            Components.Text(placeholder, foregroundColor = colors.subtitleText)
        }
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = Font.system.textStyle.copy(color = colors.titleText),
            cursorBrush = SolidColor(if (isSending) Colors.inputTextViewAlternateTint else colors.navigationBarButton),
            keyboardOptions = keyboardOptions,
            maxLines = Floats.FIELD_MAX_LINES,
            interactionSource = interactionSource,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged {
                        val didGainFocus = it.isFocused && !isFocused
                        isFocused = it.isFocused
                        onFocusChanged?.invoke(it.isFocused)
                        if (didGainFocus) onBeginEditing?.invoke()
                    },
        )
    }
}

// The send button's inset from the text field's trailing edge. The
// button's trailing edge sits its width less the decrement inside the
// bar's content edge, which lies the middle content padding beyond the
// field.
private val sendButtonTrailingInset: Dp
    get() =
        Floats.buttonSizeWidth -
            Floats.sendButtonTrailingAnchorConstraintConstantDecrement -
            Floats.middleContentViewPaddingRight
