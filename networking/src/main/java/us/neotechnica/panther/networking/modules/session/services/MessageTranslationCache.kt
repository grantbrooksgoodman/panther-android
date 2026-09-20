//
//  MessageTranslationCache.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.translator.models.Translation

/**
 * A process-lifetime cache of resolved message translations, keyed by
 * message identifier and display language.
 *
 * A hosted (wire) message carries only a reference to its translation, so
 * resolving it means an archive read; that read is redundant once done,
 * because a message's translation never changes. Caching the result lets
 * a reopened chat present its history synchronously instead of resolving
 * every message again, standing in for the whole-tree archive snapshot the
 * iOS app keeps in memory.
 */
internal object MessageTranslationCache {
    // MARK: - Properties

    private val store = LockIsolated(mutableMapOf<String, Translation>())

    // MARK: - Methods

    /** Returns the cached translation for [messageID] in [languageCode], or `null`. */
    fun get(
        messageID: String,
        languageCode: String,
    ): Translation? = store.withValue { it.value[cacheKey(messageID, languageCode)] }

    /** Caches [translation] as the resolution of [messageID] in [languageCode]. */
    fun put(
        messageID: String,
        languageCode: String,
        translation: Translation,
    ) {
        store.withValue { it.value[cacheKey(messageID, languageCode)] = translation }
    }

    // MARK: - Auxiliary

    private fun cacheKey(
        messageID: String,
        languageCode: String,
    ): String = "$messageID|$languageCode"
}
