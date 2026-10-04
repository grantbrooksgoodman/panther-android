//
//  PhoneNumberTextField.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.google.i18n.phonenumbers.PhoneNumberUtil
import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.networking.modules.common.extensions.digits

/**
 * A text field that formats an entered phone number as the user types.
 *
 * Use [PhoneNumberTextField] to accept phone number input for a specific
 * region. The field displays an example number for [regionCode] as its
 * placeholder and reformats the entered digits into a partially
 * formatted national number whenever the text or region changes. The
 * underlying edit buffer stays plain digits; [onValueChange] reports
 * those digits.
 *
 * @param value The entered national number, as raw digits.
 * @param onValueChange Called with the edited number's raw digits.
 * @param regionCode The region whose formatting and placeholder to use.
 * @param modifier The modifier for this field.
 */
@Composable
fun PhoneNumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    regionCode: String,
    modifier: Modifier = Modifier,
) {
    GenericTextField(
        value = value,
        placeholder = PhoneNumberService.exampleNationalNumberString(regionCode),
        onValueChange = { onValueChange(it.digits) },
        keyboardType = KeyboardType.Phone,
        visualTransformation = remember(regionCode) { PhoneNumberVisualTransformation(regionCode) },
        modifier = modifier,
    )
}

/**
 * A [VisualTransformation] that displays a raw-digit phone number
 * formatted for [regionCode] while keeping the underlying edit buffer as
 * plain digits.
 *
 * Because the buffer never contains the inserted separators, the caret
 * advances one step per digit and is never displaced when formatting
 * adds a `-`, `)`, or space; the [OffsetMapping] translates between the
 * raw-digit offsets and the formatted display offsets by counting
 * digits.
 *
 * @param regionCode The region whose formatting conventions to apply.
 */
class PhoneNumberVisualTransformation(
    private val regionCode: String,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val formatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(regionCode.uppercase())
        var formatted = ""
        for (character in digits) if (character.isDigit()) formatted = formatter.inputDigit(character)

        // The display index of each raw digit, so offsets can map both ways.
        val digitDisplayOffsets = formatted.indices.filter { formatted[it].isDigit() }

        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 0) return 0
                    if (offset > digitDisplayOffsets.size) return formatted.length
                    return digitDisplayOffsets[offset - 1] + 1
                }

                override fun transformedToOriginal(offset: Int): Int {
                    val clamped = offset.coerceIn(0, formatted.length)
                    return (0 until clamped).count { formatted[it].isDigit() }
                }
            }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
