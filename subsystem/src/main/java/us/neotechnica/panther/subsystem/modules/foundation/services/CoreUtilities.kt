//
//  CoreUtilities.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.models.overriddenLanguageCode
import java.util.Locale

/**
 * Cross-cutting utilities shared across the app.
 */
object CoreUtilities {
    // MARK: - Methods

    /**
     * Clears the given cache domains, or every registered cache
     * domain when none are specified.
     *
     * @param domains The domains to clear, or `null` to clear all
     *   registered domains.
     */
    fun clearCaches(domains: List<CacheDomain>? = null) {
        (domains ?: CacheDomain.allCases).forEach { it.clear() }
    }

    /**
     * Erases every file in the app's documents directory.
     *
     * If the file store has not been initialized, this method has no
     * effect.
     */
    fun eraseDocumentsDirectory() {
        val directory = FileStore.documentsDirectory ?: return
        directory.listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * Restores the active language code to the device's system
     * language.
     */
    fun restoreDeviceLanguageCode() {
        setLanguageCode(Locale.getDefault().language)
    }

    /**
     * Sets the active language code for translation and localization.
     *
     * The new code is stored in [RuntimeStorage] and is read as the
     * target language for display-string translation.
     *
     * @param languageCode The language code to set (for example,
     *   `"fr"`).
     * @param override Pass `true` to persist the code as an override
     *   that takes precedence over the stored language code. The
     *   default is `false`.
     */
    fun setLanguageCode(
        languageCode: String,
        override: Boolean = false,
    ) {
        RuntimeStorage.store(languageCode, StoredItemKey.languageCode)
        if (!override) return
        RuntimeStorage.store(languageCode, StoredItemKey.overriddenLanguageCode)
    }
}
