//
//  PersistentStorageKey+EntitySessionExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.extensions

/** The persistent storage keys scoped to the user session service. */
enum class UserSessionServiceStorageKey(
    val rawValue: String,
) {
    CURRENT_USER_ID("currentUserID"),
}
