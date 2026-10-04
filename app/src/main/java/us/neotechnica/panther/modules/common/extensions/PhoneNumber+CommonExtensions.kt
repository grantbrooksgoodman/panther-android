//
//  PhoneNumber+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import com.google.i18n.phonenumbers.PhoneNumberUtil
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.modules.common.models.PhoneNumber

/**
 * Returns the national number formatted for the given region's
 * conventions, without a calling-code prefix.
 *
 * Uses libphonenumber's `AsYouTypeFormatter`.
 *
 * @param regionCode The region whose conventions to use; defaults to
 *   the number's own region.
 */
fun PhoneNumber.partiallyFormatted(regionCode: String? = null): String {
    val region = (regionCode ?: this.regionCode).uppercase()
    val national = nationalNumberString.ifEmpty { compiledNumberString }.digits
    if (national.isEmpty()) return national

    val formatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(region)
    var formatted = ""
    for (character in national) formatted = formatter.inputDigit(character)
    return formatted.trim()
}

/**
 * Returns the number formatted for display, prefixed with `+` and its
 * calling code.
 *
 * @param regionCode The region whose conventions to use; defaults to
 *   the number's own region.
 */
fun PhoneNumber.formattedString(regionCode: String? = null): String {
    val national = partiallyFormatted(regionCode)
    return "+$callingCode $national".trim()
}
