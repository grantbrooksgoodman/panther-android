//
//  RemoteCacheStatus.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

/**
 * The validity of a user's remote cache.
 *
 * The remote cache status is stored per user in the remote
 * database. When the status is [INVALID], the app resets its local
 * data during bundle initialization and re-fetches it from the
 * server.
 */
enum class RemoteCacheStatus {
    /** The cache has been invalidated and must be rebuilt. */
    INVALID,

    /** The cache is valid. */
    VALID,
}
