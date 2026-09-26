//
//  PersistentStorageKeys.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.modules.common.extensions.ApplicationStorageKey
import us.neotechnica.panther.modules.common.extensions.BreadcrumbsCaptureServiceStorageKey
import us.neotechnica.panther.modules.common.extensions.ContactPairArchiveServiceStorageKey
import us.neotechnica.panther.modules.common.extensions.MetadataServiceStorageKey
import us.neotechnica.panther.modules.common.extensions.ReviewServiceStorageKey
import us.neotechnica.panther.modules.common.extensions.UpdateServiceStorageKey
import us.neotechnica.panther.modules.session.entity.extensions.UserSessionServiceStorageKey
import us.neotechnica.panther.networking.modules.common.extensions.NetworkingStorageKey
import us.neotechnica.panther.networking.modules.common.extensions.networking
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.PermanentPersistentStorageKeyDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey

// MARK: - Types

/**
 * The delegate that declares which persistent storage keys are
 * preserved during a reset.
 */
object PermanentKeyDelegate : PermanentPersistentStorageKeyDelegate {
    override val permanentKeys: List<PersistentStorageKey> =
        listOf(
            PersistentStorageKey.application(ApplicationStorageKey.BUILD_MILESTONE_STRING),
            PersistentStorageKey.application(ApplicationStorageKey.HAS_RUN_ONCE),
            PersistentStorageKey.application(ApplicationStorageKey.IS_IN_STAGING_MODE),
            PersistentStorageKey.breadcrumbsCaptureService(
                BreadcrumbsCaptureServiceStorageKey.BREADCRUMBS_CAPTURE_HISTORY,
            ),
            PersistentStorageKey.networking(NetworkingStorageKey.IS_NETWORK_ACTIVITY_INDICATOR_ENABLED),
            PersistentStorageKey.networking(NetworkingStorageKey.NETWORK_ENVIRONMENT),
        )
}

// MARK: - Methods

/** Returns the persistent storage key for the specified application key. */
fun PersistentStorageKey.Companion.application(key: ApplicationStorageKey): PersistentStorageKey =
    PersistentStorageKey(key.rawValue)

/** Returns the persistent storage key for the specified breadcrumbs capture service key. */
fun PersistentStorageKey.Companion.breadcrumbsCaptureService(
    key: BreadcrumbsCaptureServiceStorageKey,
): PersistentStorageKey = PersistentStorageKey(key.rawValue)

/** Returns the persistent storage key for the specified contact pair archive service key. */
fun PersistentStorageKey.Companion.contactPairArchiveService(
    key: ContactPairArchiveServiceStorageKey,
): PersistentStorageKey = PersistentStorageKey(key.rawValue)

/** Returns the persistent storage key for the specified metadata service key. */
fun PersistentStorageKey.Companion.metadataService(key: MetadataServiceStorageKey): PersistentStorageKey =
    PersistentStorageKey(key.rawValue)

/** Returns the persistent storage key for the specified review service key. */
fun PersistentStorageKey.Companion.reviewService(key: ReviewServiceStorageKey): PersistentStorageKey =
    PersistentStorageKey(key.rawValue)

/** Returns the persistent storage key for the specified update service key. */
fun PersistentStorageKey.Companion.updateService(key: UpdateServiceStorageKey): PersistentStorageKey =
    PersistentStorageKey(key.rawValue)

/** Returns the persistent storage key for the specified user session service key. */
fun PersistentStorageKey.Companion.userSessionService(key: UserSessionServiceStorageKey): PersistentStorageKey =
    PersistentStorageKey(key.rawValue)
