//
//  LocalTranslationArchiver.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.extensions.encodedHash
import us.neotechnica.panther.translator.interfaces.TranslationArchiverDelegate
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * A persistent, on-device store for caching completed translations.
 *
 * [LocalTranslationArchiver] saves and retrieves [Translation]
 * values using persistent storage as its backing store. It serves
 * as the default archiver for [TranslationService] when no custom
 * [TranslationArchiverDelegate] is registered through
 * [Translator.config].
 *
 * All read and write operations are serialized using an internal
 * lock, making the archiver safe to call from any thread or
 * concurrency context.
 *
 * **Important:** Because this archiver uses persistent storage, it
 * is best suited for moderate amounts of cached data. For
 * large-scale translation caching, consider implementing a custom
 * archiver backed by a database or file-based storage.
 */
object LocalTranslationArchiver : TranslationArchiverDelegate {
    // MARK: - Types

    private data class State(
        val archiveIndex: Map<String, Translation> = emptyMap(),
        val didLoadArchive: Boolean = false,
    )

    // MARK: - Properties

    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val state = LockIsolated(State())

    // MARK: - Preload

    /**
     * Loads and indexes the archive from disk ahead of the first
     * lookup.
     *
     * Call this method early in the app lifecycle (for example, at
     * launch) to move the one-time cost of decoding and indexing
     * the archive off the first translation request. Calling it
     * more than once has no effect.
     */
    fun preload() {
        persistenceScope.launch { state.withValue { loadArchiveIfNeeded(it) } }
    }

    // MARK: - TranslationArchiverDelegate Conformance

    /**
     * Adds a single translation to the archive.
     *
     * If a translation with the same input and language pair
     * already exists in the archive, it is replaced with the new
     * value.
     *
     * @param translation The translation to store.
     */
    override fun addValue(translation: Translation) {
        state.withValue {
            loadArchiveIfNeeded(it)
            it.value = it.value.copy(archiveIndex = it.value.archiveIndex + (indexKey(translation) to translation))
            persistArchive(it.value.archiveIndex)
        }
    }

    /**
     * Adds a set of translations to the archive.
     *
     * Use this method to store multiple translations in a single
     * operation. Existing entries in the archive are preserved;
     * the new translations are merged in.
     *
     * @param translations A set of translations to store.
     */
    override fun addValues(translations: Set<Translation>) {
        state.withValue { reference ->
            loadArchiveIfNeeded(reference)
            reference.value =
                reference.value.copy(
                    archiveIndex = reference.value.archiveIndex + translations.associateBy { indexKey(it) },
                )
            persistArchive(reference.value.archiveIndex)
        }
    }

    /**
     * Retrieves a cached translation matching the given input hash
     * and language pair.
     *
     * @param inputValueEncodedHash The encoded hash of the original
     *   input string to look up.
     * @param languagePair The language pair to match against.
     *
     * @return The matching translation, or `null` if no cached
     *   translation is found.
     */
    override fun getValue(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation? =
        state.withValue {
            loadArchiveIfNeeded(it)
            it.value.archiveIndex[indexKey(inputValueEncodedHash, languagePair)]
        }

    /**
     * Removes a cached translation matching the given input hash
     * and language pair.
     *
     * If no matching translation exists in the archive, this
     * method does nothing.
     *
     * @param inputValueEncodedHash The encoded hash of the original
     *   input string to remove.
     * @param languagePair The language pair to match against.
     */
    override fun removeValue(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ) {
        state.withValue {
            loadArchiveIfNeeded(it)
            it.value = it.value.copy(archiveIndex = it.value.archiveIndex - indexKey(inputValueEncodedHash, languagePair))
            persistArchive(it.value.archiveIndex)
        }
    }

    /** Removes all cached translations from the archive. */
    override fun clearArchive() {
        state.withValue {
            it.value = it.value.copy(archiveIndex = emptyMap(), didLoadArchive = true)
            persistArchive(emptyMap())
        }
    }

    // MARK: - Auxiliary

    @Suppress("LoopWithTooManyJumpStatements")
    private fun decodedArchive(encodedArchive: String): Map<String, Translation> {
        val archiveIndex = mutableMapOf<String, Translation>()

        try {
            val array = JSONArray(encodedArchive)
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                val languagePair = LanguagePair.fromString(entry.optString(LANGUAGE_PAIR_KEY)) ?: continue
                val original = entry.optString(ORIGINAL_KEY)
                if (original.isEmpty()) continue

                val translation =
                    Translation(
                        input =
                            TranslationInput(
                                original,
                                alternate = entry.optString(ALTERNATE_KEY).takeIf { it.isNotEmpty() },
                            ),
                        output = entry.optString(OUTPUT_KEY),
                        languagePair = languagePair,
                    )

                archiveIndex[indexKey(translation)] = translation
            }
        } catch (exception: JSONException) {
            Translator.config.loggerDelegate?.log(
                Translator.descriptor(exception),
                sender = this,
                fileName = "LocalTranslationArchiver.kt",
                function = "decodedArchive",
                line = 0,
            )
        }

        return archiveIndex
    }

    private fun encodedArchive(archiveIndex: Map<String, Translation>): String {
        val array = JSONArray()
        for (translation in archiveIndex.values) {
            array.put(
                JSONObject().apply {
                    put(LANGUAGE_PAIR_KEY, translation.languagePair.string)
                    put(ORIGINAL_KEY, translation.input.original)
                    translation.input.alternate?.let { put(ALTERNATE_KEY, it) }
                    put(OUTPUT_KEY, translation.output)
                },
            )
        }

        return array.toString()
    }

    private fun indexKey(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): String = "$inputValueEncodedHash|${languagePair.string}"

    private fun indexKey(translation: Translation): String = indexKey(translation.input.value.encodedHash, translation.languagePair)

    private fun loadArchiveIfNeeded(reference: LockIsolated.Ref<State>) {
        if (reference.value.didLoadArchive) return

        val encodedArchive = runCatching { Persistent.string(ARCHIVE_STORAGE_KEY) }.getOrNull()
        val archiveIndex = encodedArchive?.let { decodedArchive(it) } ?: emptyMap()

        reference.value =
            reference.value.copy(
                archiveIndex = archiveIndex + reference.value.archiveIndex,
                didLoadArchive = true,
            )
    }

    // Serializes disk writes off the calling thread; lookups and
    // mutations operate on the in-memory archive and never wait for
    // the disk.
    private fun persistArchive(archiveIndex: Map<String, Translation>) {
        persistenceScope.launch {
            runCatching { Persistent.setString(ARCHIVE_STORAGE_KEY, encodedArchive(archiveIndex)) }
        }
    }

    // MARK: - Companion

    private val ARCHIVE_STORAGE_KEY = PersistentStorageKey("translationArchive")

    private const val ALTERNATE_KEY = "alternate"
    private const val LANGUAGE_PAIR_KEY = "languagePair"
    private const val ORIGINAL_KEY = "original"
    private const val OUTPUT_KEY = "output"
}
