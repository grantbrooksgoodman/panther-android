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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import us.neotechnica.panther.modules.common.extensions.partiallyFormatted
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
 * partially formatted national number whenever the text or region
 * changes.
 *
 * **Note:** Formatting rewrites the text through [onTextChange] after
 * a change, so the value observed immediately after an edit may not
 * yet be formatted.
 *
 * @param text The phone number string the field displays and edits.
 * @param onTextChange Called with the edited, then formatted, text.
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
    LaunchedEffect(text, regionCode) {
        if (text.isBlank()) return@LaunchedEffect
        val partiallyFormatted = partiallyFormatted(text, regionCode)
        if (partiallyFormatted != text) onTextChange(partiallyFormatted)
    }

    GenericTextField(
        text = text,
        onTextChange = onTextChange,
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
