//
//  Exception+AuthExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.auth.extensions

import us.neotechnica.panther.subsystem.modules.foundation.extensions.notReportable
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

/**
 * The user-info key under which an authentication exception carries
 * its underlying Firebase error code.
 *
 * The key matches the one the iOS Firebase SDK populates, so
 * error-code checks read identically across platforms.
 */
const val FIREBASE_AUTH_ERROR_CODE_KEY = "FIRAuthErrorUserInfoNameKey"

/**
 * Returns a copy of the exception marked non-reportable when its
 * Firebase error code identifies a user-caused failure.
 *
 * Pass the set of error codes that represent user error for the
 * operation at hand – an invalid entry, an expired session, a
 * cancelled web context. When the exception's underlying Firebase
 * error code is one of those, the returned exception is marked
 * non-reportable; otherwise the exception is returned unchanged.
 *
 * @param codes The error codes to treat as user-caused.
 */
fun Exception.notReportableForAuthCodes(codes: Set<String>): Exception {
    val code = userInfo?.get(FIREBASE_AUTH_ERROR_CODE_KEY) as? String
    return if (code in codes) notReportable() else this
}
