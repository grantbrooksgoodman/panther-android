//
//  GenericTextField.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.designsystem.modules.theming.views.ThemedView
import us.neotechnica.panther.modules.content.shared.constants.GenericTextFieldFloats

// MARK: - Constants Accessors

private typealias TextFieldFloats = GenericTextFieldFloats

/**
 * A single-line text field with an underline divider and configurable
 * input behavior.
 *
 * Use `GenericTextField` as the base text entry control for the app's
 * pages. The field displays the given text, shows placeholder text
 * while empty, and draws a divider beneath its content. Configure
 * keyboard, autocapitalization, autocorrection, alignment, and colors
 * through the parameters; every parameter but the text, its change
 * handler, and the placeholder has a default.
 *
 * @param text The text the field displays and edits.
 * @param onTextChange Called with the edited text.
 * @param placeholderText The placeholder string to display while the
 *   field is empty.
 * @param modifier The modifier for this field.
 * @param alignment The alignment of the field's text.
 * @param autocapitalization The autocapitalization behavior to apply,
 *   or `null` for the system default.
 * @param isAutocorrectEnabled Whether autocorrection is enabled.
 * @param isThemed Whether the field is wrapped in a themed container.
 * @param keyboardType The keyboard type to display during editing.
 * @param placeholderColor The placeholder's color, or `null` to use
 *   gray.
 * @param submitLabel The label of the keyboard's submit button.
 * @param textColor The color of the field's text, or `null` for the
 *   themed title color.
 * @param dividerXOffset The horizontal offset of the field's divider.
 * @param dividerYOffset The vertical offset of the field's divider.
 */
@Composable
@Suppress("LongParameterList")
fun GenericTextField(
    text: String,
    onTextChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    alignment: TextAlign = TextAlign.Center,
    autocapitalization: KeyboardCapitalization? = null,
    isAutocorrectEnabled: Boolean = false,
    isThemed: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholderColor: Color? = null,
    submitLabel: ImeAction = ImeAction.Done,
    textColor: Color? = null,
    dividerXOffset: Dp = TextFieldFloats.defaultDividerXOffset,
    dividerYOffset: Dp = TextFieldFloats.defaultDividerYOffset,
) {
    TextFieldContent(
        isEmpty = text.isEmpty(),
        placeholderText = placeholderText,
        placeholderColor = placeholderColor ?: Color.Gray,
        alignment = alignment,
        isThemed = isThemed,
        dividerXOffset = dividerXOffset,
        dividerYOffset = dividerYOffset,
        modifier = modifier,
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            singleLine = true,
            textStyle = fieldTextStyle(alignment, textColor),
            cursorBrush = SolidColor(LocalPantherColors.current.accent),
            keyboardOptions = keyboardOptions(autocapitalization, isAutocorrectEnabled, keyboardType, submitLabel),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A single-line text field with an underline divider and configurable
 * input behavior, whose value includes its selection.
 *
 * Use this overload in place of the one taking a string when the
 * field's text is rewritten as the user edits it – such as to format
 * it – so the rewrite can place the cursor, rather than leave it at
 * the same character index.
 *
 * @param value The text and selection the field displays and edits.
 * @param onValueChange Called with the edited value.
 * @param placeholderText The placeholder string to display while the
 *   field is empty.
 * @param modifier The modifier for this field.
 * @param alignment The alignment of the field's text.
 * @param autocapitalization The autocapitalization behavior to apply,
 *   or `null` for the system default.
 * @param isAutocorrectEnabled Whether autocorrection is enabled.
 * @param isThemed Whether the field is wrapped in a themed container.
 * @param keyboardType The keyboard type to display during editing.
 * @param placeholderColor The placeholder's color, or `null` to use
 *   gray.
 * @param submitLabel The label of the keyboard's submit button.
 * @param textColor The color of the field's text, or `null` for the
 *   themed title color.
 * @param dividerXOffset The horizontal offset of the field's divider.
 * @param dividerYOffset The vertical offset of the field's divider.
 */
@Composable
@Suppress("LongParameterList")
fun GenericTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    alignment: TextAlign = TextAlign.Center,
    autocapitalization: KeyboardCapitalization? = null,
    isAutocorrectEnabled: Boolean = false,
    isThemed: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholderColor: Color? = null,
    submitLabel: ImeAction = ImeAction.Done,
    textColor: Color? = null,
    dividerXOffset: Dp = TextFieldFloats.defaultDividerXOffset,
    dividerYOffset: Dp = TextFieldFloats.defaultDividerYOffset,
) {
    TextFieldContent(
        isEmpty = value.text.isEmpty(),
        placeholderText = placeholderText,
        placeholderColor = placeholderColor ?: Color.Gray,
        alignment = alignment,
        isThemed = isThemed,
        dividerXOffset = dividerXOffset,
        dividerYOffset = dividerYOffset,
        modifier = modifier,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = fieldTextStyle(alignment, textColor),
            cursorBrush = SolidColor(LocalPantherColors.current.accent),
            keyboardOptions = keyboardOptions(autocapitalization, isAutocorrectEnabled, keyboardType, submitLabel),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// MARK: - Content

// The field's placeholder and divider around the given text field,
// wrapped in a themed container when requested.
@Composable
@Suppress("LongParameterList")
private fun TextFieldContent(
    isEmpty: Boolean,
    placeholderText: String,
    placeholderColor: Color,
    alignment: TextAlign,
    isThemed: Boolean,
    dividerXOffset: Dp,
    dividerYOffset: Dp,
    modifier: Modifier,
    textField: @Composable () -> Unit,
) {
    val content: @Composable () -> Unit = {
        val colors = LocalPantherColors.current

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxWidth().heightIn(min = TextFieldFloats.fieldMinHeight),
        ) {
            if (isEmpty) {
                Components.Text(
                    placeholderText,
                    foregroundColor = placeholderColor,
                    textAlign = alignment,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            textField()

            HorizontalDivider(
                color = colors.subtitleText.copy(alpha = TextFieldFloats.DIVIDER_ALPHA),
                modifier = Modifier.offset(x = dividerXOffset, y = dividerYOffset),
            )
        }
    }

    if (isThemed) ThemedView(content) else content()
}

// MARK: - Auxiliary

@Composable
private fun fieldTextStyle(
    alignment: TextAlign,
    textColor: Color?,
): TextStyle = Font.system.textStyle.copy(color = textColor ?: LocalPantherColors.current.titleText, textAlign = alignment)

private fun keyboardOptions(
    autocapitalization: KeyboardCapitalization?,
    isAutocorrectEnabled: Boolean,
    keyboardType: KeyboardType,
    submitLabel: ImeAction,
): KeyboardOptions =
    KeyboardOptions(
        capitalization = autocapitalization ?: KeyboardCapitalization.Unspecified,
        autoCorrectEnabled = isAutocorrectEnabled,
        keyboardType = keyboardType,
        imeAction = submitLabel,
    )
