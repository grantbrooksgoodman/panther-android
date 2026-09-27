//
//  LocalTranslationArchiverDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.translation.delegates

import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.interfaces.TranslationArchiverDelegate
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The persistent translation archive.
 *
 * Mirrors the iOS `LocalTranslationArchiverDelegate`: lookups and
 * mutations operate on an in-memory archive and its index, while disk
 * writes are serialized onto a background thread off the calling path.
 * The archive is decoded from disk at most once per launch and persisted
 * through [Persistent] under [PersistentStorageKey.translationArchive],
 * so a message's resolved translation survives relaunches and a reopened
 * chat presents its history without re-resolving every message.
 *
 * Registered through
 * [Translator.config][us.neotechnica.panther.translator.Translator.config],
 * superseding the in-memory `LocalTranslationArchiver` default.
 */
class LocalTranslationArchiverDelegate : TranslationArchiverDelegate {
    // MARK: - Properties

    private val ioLock = ReentrantLock()

    /**
     * Serializes disk writes off the calling thread; lookups and
     * mutations operate on the in-memory archive and never wait for the
     * disk.
     */
    private val persistenceExecutor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "translation-archiver-persistence").apply { isDaemon = true }
        }

    /**
     * Maps "<input value hash>|<language pair>" to its translation for
     * constant-time lookups, avoiding a per-entry hash computation on
     * every query.
     */
    private var archiveIndex = mutableMapOf<String, Translation>()

    /** The decoded archive, loaded from disk at most once per launch. */
    private var cachedArchive: MutableSet<Translation>? = null

    // MARK: - Init

    init {
        // Decode and index the archive ahead of the first lookup, keeping
        // the one-time cost off both the launch path and the translation path.
        persistenceExecutor.execute { ioLock.withLock { loadArchiveIfNeeded() } }
    }

    // MARK: - Add Value

    override fun addValue(translation: Translation) {
        ioLock.withLock {
            val archive = loadArchiveIfNeeded()
            insert(translation, archive)

            cachedArchive = archive
            persistArchive(archive)
        }
    }

    override fun addValues(translations: Set<Translation>) {
        ioLock.withLock {
            val archive = loadArchiveIfNeeded()
            for (translation in translations) insert(translation, archive)

            cachedArchive = archive
            persistArchive(archive)
        }
    }

    // MARK: - Get Value

    override fun getValue(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation? =
        ioLock.withLock {
            loadArchiveIfNeeded()
            archiveIndex[indexKey(inputValueEncodedHash, languagePair)]
        }

    // MARK: - Remove Value

    override fun removeValue(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ) {
        ioLock.withLock {
            val key = indexKey(inputValueEncodedHash, languagePair)
            val value = archiveIndex[key] ?: return@withLock

            val archive = loadArchiveIfNeeded()
            archive.remove(value)
            archiveIndex.remove(key)

            cachedArchive = archive
            persistArchive(archive)
        }
    }

    // MARK: - Clear Archive

    override fun clearArchive() {
        ioLock.withLock {
            archiveIndex = mutableMapOf()
            cachedArchive = mutableSetOf()
            persistArchive(emptySet())
        }
    }

    // MARK: - Auxiliary

    private fun indexKey(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): String = "$inputValueEncodedHash|${languagePair.string}"

    private fun indexKey(translation: Translation): String =
        indexKey(encodedHashOf(listOf(translation.input.value)), translation.languagePair)

    /**
     * Inserts the translation into both the archive and the index,
     * replacing any existing translation for the same input and language
     * pair.
     */
    private fun insert(
        translation: Translation,
        archive: MutableSet<Translation>,
    ) {
        val key = indexKey(translation)
        archiveIndex[key]?.let { archive.remove(it) }

        archive.add(translation)
        archiveIndex[key] = translation
    }

    private fun loadArchiveIfNeeded(): MutableSet<Translation> {
        cachedArchive?.let { return it }

        val archive =
            Persistent.archive(PersistentStorageKey.translationArchive) { maps ->
                maps.mapNotNull { decode(it) }.toMutableSet()
            } ?: mutableSetOf()

        cachedArchive = archive
        archiveIndex = archive.associateBy { indexKey(it) }.toMutableMap()

        return archive
    }

    /**
     * Writes the archive on a background thread, keeping disk I/O off the
     * translation path. Writes are serialized in submission order, so the
     * last snapshot always wins.
     */
    private fun persistArchive(archive: Set<Translation>) {
        val snapshot = archive.toList()
        persistenceExecutor.execute {
            Persistent.setArchive(
                PersistentStorageKey.translationArchive,
                if (snapshot.isEmpty()) null else snapshot.map { encode(it) },
            )
        }
    }

    private fun encode(translation: Translation): Map<String, Any?> =
        buildMap {
            put(KEY_ORIGINAL, translation.input.original)
            translation.input.alternate?.let { put(KEY_ALTERNATE, it) }
            put(KEY_OUTPUT, translation.output)
            put(KEY_FROM, translation.languagePair.from)
            put(KEY_TO, translation.languagePair.to)
        }

    private fun decode(map: Map<String, Any?>): Translation? {
        val original = map[KEY_ORIGINAL] as? String ?: return null
        val output = map[KEY_OUTPUT] as? String ?: return null
        val from = map[KEY_FROM] as? String ?: return null
        val to = map[KEY_TO] as? String ?: return null

        return Translation(
            input = TranslationInput(original, alternate = map[KEY_ALTERNATE] as? String),
            output = output,
            languagePair = LanguagePair(from = from, to = to),
        )
    }

    // MARK: - Testing

    /**
     * Blocks until every persistence write submitted so far has
     * completed. For tests only, where the file store's directory is a
     * shared, reassignable static; production writes run fire-and-forget.
     */
    internal fun flushForTesting() {
        val latch = java.util.concurrent.CountDownLatch(1)
        persistenceExecutor.execute { latch.countDown() }
        latch.await()
    }

    // MARK: - Companion

    companion object {
        /** Registers a fresh persistent archiver with the translator. */
        fun registerWithDependencies() {
            Translator.config.registerArchiverDelegate(LocalTranslationArchiverDelegate())
        }

        private const val KEY_ORIGINAL = "original"
        private const val KEY_ALTERNATE = "alternate"
        private const val KEY_OUTPUT = "output"
        private const val KEY_FROM = "from"
        private const val KEY_TO = "to"
    }
}
