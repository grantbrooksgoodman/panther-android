//
//  NetworkServices.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.models

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.auth.interfaces.AuthDelegate
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.networking.modules.health.interfaces.NetworkHealthDelegate
import us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate

/**
 * A collection of the networking delegates that power
 * authentication, database, health estimation, storage, and
 * translation operations.
 *
 * Access the current services through the dependency injection
 * system:
 *
 * ```kotlin
 * private val networking: NetworkServices by Dependency { it.networking }
 * ```
 */
object NetworkServices {
    // MARK: - Computed Properties

    /** The delegate that handles user authentication. */
    val auth: AuthDelegate
        get() = Networking.config.authDelegate

    /** The delegate that handles database operations. */
    val database: DatabaseDelegate
        get() = Networking.config.databaseDelegate

    /** The delegate that provides passive network quality estimation. */
    val health: NetworkHealthDelegate
        get() = Networking.config.healthDelegate

    /** The delegate that handles hosted translations. */
    val hostedTranslation: HostedTranslationDelegate
        get() = Networking.config.hostedTranslationDelegate

    /** The delegate that handles file storage operations. */
    val storage: StorageDelegate
        get() = Networking.config.storageDelegate
}
