//
//  TranslationArchiverDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.interfaces

import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation

/**
 * A protocol you adopt to provide translation caching.
 *
 * [TranslationService][us.neotechnica.panther.translator.services.TranslationService]
 * consults the registered archiver before every network request and
 * stores each successful translation through it afterward. When no
 * custom archiver is registered, the service falls back to
 * [LocalTranslationArchiver][us.neotechnica.panther.translator.services.LocalTranslationArchiver].
 *
 * Register an archiver through
 * [Translator.config][us.neotechnica.panther.translator.Translator.config].
 */
interface TranslationArchiverDelegate {
    // MARK: - Add Value

    /** Stores a single translation in the archive. */
    fun addValue(translation: Translation)

    /** Stores a set of translations in the archive. */
    fun addValues(translations: Set<Translation>)

    // MARK: - Get Value

    /**
     * Retrieves a cached translation matching the given input hash
     * and language pair, or `null` on a cache miss.
     *
     * @param inputValueEncodedHash The encoded hash of the original
     *   input string.
     * @param languagePair The language pair to match against.
     */
    fun getValue(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation?

    // MARK: - Remove Value

    /**
     * Removes a cached translation matching the given input hash and
     * language pair, doing nothing if none exists.
     *
     * @param inputValueEncodedHash The encoded hash of the input to
     *   remove.
     * @param languagePair The language pair to match against.
     */
    fun removeValue(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    )

    // MARK: - Clear Archive

    /** Removes all cached translations from the archive. */
    fun clearArchive()
}
