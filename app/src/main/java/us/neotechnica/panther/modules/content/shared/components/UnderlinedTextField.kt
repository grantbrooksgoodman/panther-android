//
//  UnderlinedTextField.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.constants.UnderlinedTextFieldFloats

/**
 * A single-line, center-aligned text field with a gray placeholder and
 * an underline divider, mirroring the iOS `GenericTextField`.
 *
 * @param value The current text.
 * @param placeholder The gray placeholder shown while empty.
 * @param onValueChange Called with the edited text.
 * @param keyboardType The soft-keyboard type.
 * @param modifier The modifier for this field.
 * @param visualTransformation A display-only transformation applied to
 *   the text (for example, phone-number formatting).
 */
@Composable
@Suppress("LongParameterList")
fun UnderlinedTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = LocalPantherColors.current
    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().heightIn(min = UnderlinedTextFieldFloats.fieldMinHeight),
        ) {
            if (value.isEmpty()) {
                Components.Text(
                    placeholder,
                    color = colors.subtitleText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = Font.system.textStyle.copy(color = colors.titleText, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider(color = colors.subtitleText.copy(alpha = UnderlinedTextFieldFloats.DIVIDER_ALPHA))
    }
}
