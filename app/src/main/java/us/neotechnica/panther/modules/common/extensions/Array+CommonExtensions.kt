//
//  Array+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.PhoneNumber

// MARK: - ContactPair

/**
 * The unique compiled number strings for every phone number belonging
 * to the array's contacts.
 *
 * A compiled number string is a phone number's calling code followed
 * by its national number, containing digits only.
 */
@get:JvmName("contactPairsCompiledNumberStrings")
val List<ContactPair>.compiledNumberStrings: List<String>
    get() = flatMap { it.contact.phoneNumbers.compiledNumberStrings }.distinct()

// MARK: - PhoneNumber

/**
 * The compiled number strings for every phone number in the array.
 *
 * A compiled number string is a phone number's calling code followed
 * by its national number, containing digits only.
 */
val List<PhoneNumber>.compiledNumberStrings: List<String>
    get() = map { it.compiledNumberString }
