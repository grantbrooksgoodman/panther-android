//
//  DeviceContact.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.contacts.models

import us.neotechnica.panther.modules.common.models.PhoneNumber

/**
 * A contact read from the device's address book, grouped by its
 * address-book identifier.
 *
 * Carries the raw name fields the device exposes across its structured
 * name, nickname, and organization records, together with the merged
 * phone numbers belonging to the contact. These fields feed name
 * resolution and the matching of contacts to registered users.
 */
internal data class DeviceContact(
    val id: String,
    val givenName: String,
    val familyName: String,
    val phoneticGivenName: String,
    val phoneticFamilyName: String,
    val nickname: String,
    val organizationName: String,
    val phoneticOrganizationName: String,
    val phoneNumbers: List<PhoneNumber>,
)
