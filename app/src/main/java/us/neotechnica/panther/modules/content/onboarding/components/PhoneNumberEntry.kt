//
//  PhoneNumberEntry.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.modules.content.onboarding.constants.PhoneNumberEntryFloats
import us.neotechnica.panther.modules.content.shared.components.PhoneNumberVisualTransformation
import us.neotechnica.panther.modules.content.shared.components.RegionMenu
import us.neotechnica.panther.modules.content.shared.components.UnderlinedTextField
import us.neotechnica.panther.networking.modules.common.extensions.digits

/**
 * The region picker paired with a phone-number field, shared by the
 * phone-entry onboarding pages.
 *
 * The field's buffer holds raw digits; formatting is applied for display
 * per [selectedRegionCode], and edits are reported as raw digits.
 *
 * @param selectedRegionCode The currently selected region code.
 * @param phoneNumber The entered national number, as raw digits.
 * @param onRegionCodeSelected Called with the newly selected region code.
 * @param onPhoneNumberChange Called with the edited number's raw digits.
 * @param modifier The modifier for this row.
 */
@Composable
fun PhoneNumberEntry(
    selectedRegionCode: String,
    phoneNumber: String,
    onRegionCodeSelected: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        RegionMenu(
            selectedRegionCode = selectedRegionCode,
            onRegionCodeSelected = onRegionCodeSelected,
            modifier =
                Modifier.padding(
                    start = PhoneNumberEntryFloats.regionMenuLeadingPadding,
                    end = PhoneNumberEntryFloats.regionMenuTrailingPadding,
                ),
        )

        UnderlinedTextField(
            value = phoneNumber,
            placeholder = PhoneNumberService.exampleNationalNumberString(selectedRegionCode),
            onValueChange = { onPhoneNumberChange(it.digits) },
            keyboardType = KeyboardType.Phone,
            visualTransformation = remember(selectedRegionCode) { PhoneNumberVisualTransformation(selectedRegionCode) },
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        end = PhoneNumberEntryFloats.textFieldTrailingPadding,
                        top = PhoneNumberEntryFloats.textFieldVerticalPadding,
                        bottom = PhoneNumberEntryFloats.textFieldVerticalPadding,
                    ),
        )
    }
}
