//
//  LocalTranslationArchiver.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.translator.extensions.encodedHash
import us.neotechnica.panther.translator.interfaces.TranslationArchiverDelegate
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation

/**
 * The default in-memory translation archive.
 *
 * [TranslationService][us.neotechnica.panther.translator.services.TranslationService]
 * falls back to this archiver when no custom
 * [TranslationArchiverDelegate] is registered through
 * [Translator.config][us.neotechnica.panther.translator.Translator.config].
 *
 * **Note:** the iOS original persists to `UserDefaults`. This built-in
 * fallback is in-memory only; the host app registers a persistent
 * delegate at startup, which supersedes it.
 */
object LocalTranslationArchiver : TranslationArchiverDelegate {
    // MARK: - Properties

    private val entries = LockIsolated(mapOf<String, Translation>())

    // MARK: - Prewarm

    /** Prepares the archive for use. In-memory, so this is a no-op. */
    fun preload() {
        // Nothing to preload: the archive is held entirely in memory.
    }

    // MARK: - Delegate

    override fun addValue(translation: Translation) {
        entries.withValue { it.value = it.value + (keyFor(translation) to translation) }
    }

    override fun addValues(translations: Set<Translation>) {
        entries.withValue { current ->
            current.value = current.value + translations.associateBy { keyFor(it) }
        }
    }

    override fun getValue(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation? = entries.withValue { it.value[key(inputValueEncodedHash, languagePair)] }

    override fun removeValue(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ) {
        entries.withValue { it.value = it.value - key(inputValueEncodedHash, languagePair) }
    }

    override fun clearArchive() {
        entries.withValue { it.value = mapOf() }
    }

    // MARK: - Auxiliary

    private fun keyFor(translation: Translation): String = key(translation.input.value.encodedHash, translation.languagePair)

    private fun key(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): String = "$inputValueEncodedHash|${languagePair.string}"
}
