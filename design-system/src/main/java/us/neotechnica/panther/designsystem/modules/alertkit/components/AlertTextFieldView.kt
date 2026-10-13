//
//  AlertTextFieldView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.alertkit.constants.AlertTextFieldViewFloats
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.alertkit.models.AlertKitPalette
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale

// MARK: - Constants Accessors

private typealias TextFieldFloats = AlertTextFieldViewFloats

// A rounded, single-line text field for a text input alert, inset from
// the alert's edges and configured with the given attributes. The field takes focus when it appears,
// with the cursor after its sample text, and shows a clear button
// according to the attributes' clear button mode. Pressing the
// keyboard's Done key performs the given action.
@Composable
internal fun AlertTextFieldView(
    attributes: TextFieldAttributes,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val palette = AlertKitPalette.current
    val shape = RoundedCornerShape(TextFieldFloats.CORNER_RADIUS.dp)
    val textStyle = Font.system(FontScale.Custom(TextFieldFloats.TEXT_FONT_SIZE)).textStyle

    var isFocused by remember { mutableStateOf(false) }
    var value by remember {
        val sampleText = attributes.sampleText?.sanitized.orEmpty()
        mutableStateOf(TextFieldValue(sampleText, selection = TextRange(sampleText.length)))
    }

    val setText: (TextFieldValue) -> Unit = {
        if (it.text != value.text) onValueChange(it.text)
        value = it
    }

    val showsClearButton = showsClearButton(attributes.clearButtonMode, value.text, isFocused)
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            Modifier
                .padding(
                    bottom = TextFieldFloats.ALERT_BOTTOM_PADDING.dp,
                    end = TextFieldFloats.ALERT_HORIZONTAL_PADDING.dp,
                    start = TextFieldFloats.ALERT_HORIZONTAL_PADDING.dp,
                ).fillMaxWidth()
                .height(TextFieldFloats.HEIGHT.dp)
                .background(palette.textFieldBackground, shape)
                .border(
                    width = with(LocalDensity.current) { TextFieldFloats.BORDER_WIDTH_PIXELS.toDp() },
                    color = palette.separator,
                    shape = shape,
                ),
    ) {
        BasicTextField(
            cursorBrush = SolidColor(palette.cursor),
            decorationBox = { innerTextField ->
                // Propagating the full width lets the text align within
                // the field rather than within its own bounds.
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth(),
                    propagateMinConstraints = true,
                ) {
                    if (value.text.isEmpty()) {
                        attributes.placeholderText?.let {
                            Text(
                                it.sanitized,
                                color = palette.placeholder,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                                style = textStyle.copy(textAlign = attributes.textAlignment),
                            )
                        }
                    }

                    innerTextField()
                }
            },
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            keyboardOptions =
                KeyboardOptions(
                    autoCorrectEnabled = attributes.correctionType != TextFieldAttributes.CorrectionType.NO,
                    capitalization = attributes.capitalizationType,
                    imeAction = ImeAction.Done,
                    keyboardType = attributes.keyboardType,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset(y = TextFieldFloats.TEXT_VERTICAL_OFFSET.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .padding(
                        end =
                            if (showsClearButton) {
                                TextFieldFloats.CLEAR_BUTTON_AREA_WIDTH.dp
                            } else {
                                TextFieldFloats.TEXT_HORIZONTAL_PADDING.dp
                            },
                        start = TextFieldFloats.TEXT_HORIZONTAL_PADDING.dp,
                    ),
            onValueChange = setText,
            singleLine = true,
            textStyle = textStyle.copy(color = palette.label, textAlign = attributes.textAlignment),
            value = value,
            visualTransformation =
                if (attributes.isSecureTextEntry) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
        )

        if (showsClearButton) {
            ClearButton(
                palette = palette,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) { setText(TextFieldValue()) }
        }
    }
}

// MARK: - Clear Button

// A filled gray circle with a knocked-out cross, at the trailing end
// of a tappable area the height of the field.
@Composable
private fun ClearButton(
    palette: AlertKitPalette,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier =
            modifier
                .width(TextFieldFloats.CLEAR_BUTTON_AREA_WIDTH.dp)
                .fillMaxHeight()
                .clickable(
                    indication = null,
                    interactionSource = null,
                    onClick = onClick,
                ).padding(end = TextFieldFloats.CLEAR_BUTTON_TRAILING_PADDING.dp),
    ) {
        Canvas(modifier = Modifier.size(TextFieldFloats.CLEAR_BUTTON_DIAMETER.dp)) {
            val halfLength = TextFieldFloats.CLEAR_BUTTON_GLYPH_HALF_LENGTH.dp.toPx()
            val strokeWidth = TextFieldFloats.CLEAR_BUTTON_GLYPH_STROKE_WIDTH.dp.toPx()
            drawCircle(palette.clearButton)
            listOf(1f, -1f).forEach { direction ->
                drawLine(
                    cap = StrokeCap.Round,
                    color = palette.textFieldBackground,
                    end = center + Offset(halfLength, halfLength * direction),
                    start = center - Offset(halfLength, halfLength * direction),
                    strokeWidth = strokeWidth,
                )
            }
        }
    }
}

// MARK: - Auxiliary

private fun showsClearButton(
    clearButtonMode: TextFieldAttributes.ClearButtonMode,
    text: String,
    isFocused: Boolean,
): Boolean {
    if (text.isEmpty()) return false
    return when (clearButtonMode) {
        TextFieldAttributes.ClearButtonMode.NEVER -> false
        TextFieldAttributes.ClearButtonMode.WHILE_EDITING -> isFocused
        TextFieldAttributes.ClearButtonMode.UNLESS_EDITING -> !isFocused
        TextFieldAttributes.ClearButtonMode.ALWAYS -> true
    }
}
