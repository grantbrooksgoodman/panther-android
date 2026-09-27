//
//  CacheClearingService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.networking.modules.database.services.CoreDatabaseStore
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.services.LocalTranslationArchiver

/**
 * Clears the app's caches, standing in for the cache-clearing set of the
 * iOS `SettingsPageViewService.clearCaches()`.
 *
 * **Note:** the iOS original performs a full `Application.reset`; this
 * port clears the resolvable caches whose indexes rebuild on next use –
 * the persistent translation archive and the hosted database read cache.
 * Downloaded media re-resolves on demand and is left in place.
 */
object CacheClearingService {
    /** Empties the translation archive and database caches. */
    fun clearCaches() {
        (Translator.config.archiverDelegate ?: LocalTranslationArchiver).clearArchive()
        CoreDatabaseStore.clearStore()
    }
}
