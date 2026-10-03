//
//  Contact+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.networking.modules.common.extensions.digits

// MARK: - Properties

/** Resolves a last name using all available content sources. */
val Contact.absoluteLastName: String
    get() =
        when {
            lastName.isNotBlank() -> lastName
            firstName.isNotBlank() -> firstName
            fullName.isNotBlank() -> fullName
            else -> phoneNumbers.firstOrNull()?.formattedString() ?: "�"
        }

/**
 * The contact's section title in an alphabetical list: the first
 * letter of its last name, or `#` for names beginning with a phone
 * number.
 */
val Contact.tableViewSectionTitle: String
    get() =
        if (absoluteLastName.startsWith("+") && absoluteLastName.digits.isNotBlank()) {
            "#"
        } else {
            absoluteLastName.take(1)
        }
