//
//  ContactService+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.common.contacts.services.ContactPairArchiveService
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.services.PermissionService

// MARK: - Properties

/**
 * A Boolean value that indicates whether the contact pair archive
 * contains any contacts besides the current user.
 */
val ContactService.hasContactsBesidesCurrentUser: Boolean
    get() {
        val contactPairArchive = ContactPairArchiveService.allValues()
        if (contactPairArchive.isEmpty()) return false
        return contactPairArchive.any { !it.containsCurrentUser }
    }

// MARK: - Methods

/**
 * Synchronizes the contact pair archive when it is empty and contact
 * permission has been granted.
 *
 * @throws Exception if synchronization fails.
 */
suspend fun ContactService.syncIfNeeded() {
    val isArchiveEmpty = ContactPairArchiveService.allValues().isEmpty() || !hasContactsBesidesCurrentUser
    if (isArchiveEmpty &&
        PermissionService.contactPermissionStatus == PermissionService.PermissionStatus.GRANTED
    ) {
        syncContactPairArchive()
    }
}
