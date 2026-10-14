//
//  PhoneNumberTextField.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.networking.modules.common.extensions.digits

/**
 * A text field that formats an entered phone number as the user types.
 *
 * Use `PhoneNumberTextField` to accept phone number input for a
 * specific region. The field displays an example number for the given
 * region as its placeholder and reformats the entered digits into a
 * partially formatted national number with each edit, and whenever
 * the text or region changes.
 *
 * Reformatting keeps the cursor after the same digit, so each digit
 * lands where it was typed, however many formatting characters the
 * rewrite adds or removes ahead of it.
 *
 * @param text The phone number string the field displays and edits.
 * @param onTextChange Called with the edited and formatted text.
 * @param regionCode The code of the region used for formatting and the
 *   placeholder.
 * @param modifier The modifier for this field.
 */
@Composable
fun PhoneNumberTextField(
    text: String,
    onTextChange: (String) -> Unit,
    regionCode: String,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }

    // Formats the given value, reporting its text if it changed.
    fun update(newValue: TextFieldValue) {
        val formattedValue = newValue.formatted(regionCode)
        val didChangeText = formattedValue.text != value.text
        value = formattedValue
        if (didChangeText) onTextChange(formattedValue.text)
    }

    // Text set from outside the field – a prefilled number, say – replaces
    // the field's own with the cursor at its end, and is formatted like an
    // edit, as is the text when the region changes.
    LaunchedEffect(text, regionCode) {
        if (text != value.text) value = TextFieldValue(text, TextRange(text.length))
        update(value)
    }

    GenericTextField(
        value = value,
        onValueChange = ::update,
        keyboardType = KeyboardType.Phone,
        placeholderText = PhoneNumberService.exampleNationalNumberString(regionCode),
        modifier = modifier,
    )
}

// MARK: - Auxiliary

private fun partiallyFormatted(
    text: String,
    regionCode: String,
): String =
    PhoneNumber(
        callingCode = RegionDetailService.callingCode(regionCode) ?: PhoneNumberService.deviceCallingCode,
        nationalNumberString = text.digits,
        regionCode = regionCode,
        label = null,
        internalFormattedString = null,
    ).partiallyFormatted(regionCode)

// The value with its text partially formatted for the given region, and
// each end of its selection kept after the same number of digits.
private fun TextFieldValue.formatted(regionCode: String): TextFieldValue {
    if (text.isBlank()) return this
    val formattedText = partiallyFormatted(text, regionCode)
    if (formattedText == text) return this

    fun formattedIndex(index: Int) = formattedText.indexAfterDigits(text.take(index).count { it.isDigit() })
    return TextFieldValue(formattedText, TextRange(formattedIndex(selection.start), formattedIndex(selection.end)))
}

// The index just after the given number of digits in the string, or its
// end when it has fewer.
private fun String.indexAfterDigits(count: Int): Int {
    if (count == 0) return 0

    var digitCount = 0
    forEachIndexed { index, character ->
        if (character.isDigit()) digitCount++
        if (digitCount == count) return index + 1
    }

    return length
}
