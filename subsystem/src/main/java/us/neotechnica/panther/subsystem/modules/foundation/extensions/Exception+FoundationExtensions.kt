//
//  Exception+FoundationExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

/**
 * Returns a copy of the exception marked as non-reportable.
 *
 * Use this when a failure is known to be caused by the user – an
 * invalid entry, an expired session, a cancelled operation – and
 * should therefore not offer to file a report. When the exception
 * is already non-reportable, it is returned unchanged.
 *
 * The returned exception preserves the descriptor, user info,
 * underlying exceptions, and metadata of the original.
 */
fun Exception.notReportable(): Exception {
    if (!isReportable) return this
    return Exception(
        descriptor,
        isReportable = false,
        userInfo = userInfo,
        underlyingExceptions = underlyingExceptions,
        metadata = metadata,
    )
}
