//
//  LocalTranslationArchiverDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.translation.delegates

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import us.neotechnica.panther.modules.networking.sources.translation.constants.LocalTranslationArchiverDelegateFloats
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.interfaces.TranslationArchiverDelegate
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.TranslationArchiverDelegate as SubsystemTranslationArchiverDelegate

// MARK: - Constants Accessors

private typealias Floats = LocalTranslationArchiverDelegateFloats

/**
 * The persistent translation archive.
 *
 * Lookups and mutations operate on an in-memory index of the
 * archive, while disk writes run on a background thread off the
 * calling path. The archive is decoded from disk at most once per
 * launch and persisted through [Persistent] under
 * [PersistentStorageKey.translationArchive], so a message's
 * resolved translation survives relaunches and a reopened chat
 * presents its history without re-resolving every message.
 *
 * Changes are written to disk together, about a second after the
 * first of them, and any pending write runs as soon as the app
 * stops. Adding a translation that the archive already holds
 * writes nothing.
 *
 * Registered through
 * [Translator.config][us.neotechnica.panther.translator.Translator.config],
 * superseding the in-memory `LocalTranslationArchiver` default.
 */
class LocalTranslationArchiverDelegate :
    TranslationArchiverDelegate,
    SubsystemTranslationArchiverDelegate {
    // MARK: - Properties

    private val ioLock = ReentrantLock()

    /**
     * Serializes disk writes off the calling thread; lookups and
     * mutations operate on the in-memory archive and never wait
     * for the disk.
     */
    private val persistenceExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "translation-archiver-persistence").apply { isDaemon = true }
        }

    /**
     * Maps "<input value hash>|<language pair>" to its
     * translation for constant-time lookups, avoiding a per-entry
     * hash computation on every query. The index is the archive's
     * only in-memory copy; writes persist its values.
     */
    private var archiveIndex = mutableMapOf<String, Translation>()

    /**
     * Whether the archive has been loaded from disk, which happens
     * at most once per launch.
     */
    private var isArchiveLoaded = false

    /** Whether a write of the archive is scheduled and has yet to run. */
    private var isWritePending = false

    // MARK: - Init

    init {
        // Decode and index the archive ahead of the first lookup, keeping
        // the one-time cost off both the launch path and the translation path.
        persistenceExecutor.execute { ioLock.withLock { loadArchiveIfNeeded() } }
    }

    // MARK: - Add Value

    override fun addValue(translation: Translation) {
        ioLock.withLock {
            loadArchiveIfNeeded()
            if (insert(translation)) schedulePersistence()
        }
    }

    override fun addValues(translations: Set<Translation>) {
        ioLock.withLock {
            loadArchiveIfNeeded()
            var didChangeArchive = false
            for (translation in translations) {
                didChangeArchive = insert(translation) || didChangeArchive
            }

            if (didChangeArchive) schedulePersistence()
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
            loadArchiveIfNeeded()
            if (archiveIndex.remove(indexKey(inputValueEncodedHash, languagePair)) != null) schedulePersistence()
        }
    }

    // MARK: - Clear Archive

    override fun clearArchive() {
        ioLock.withLock {
            archiveIndex = mutableMapOf()
            isArchiveLoaded = true
            schedulePersistence()
        }
    }

    // MARK: - Testing

    /**
     * Runs any pending write at once and blocks until it, and every
     * write submitted before it, has completed. For tests only,
     * where the file store's directory is a shared, reassignable
     * static; production writes run fire-and-forget.
     */
    internal fun flushForTesting() {
        persistenceExecutor.submit { persistPendingArchive() }.get()
    }

    // MARK: - Auxiliary

    private fun indexKey(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): String = "$inputValueEncodedHash|${languagePair.string}"

    private fun indexKey(translation: Translation): String =
        indexKey(encodedHashOf(listOf(translation.input.value)), translation.languagePair)

    /**
     * Inserts the translation into the index, replacing any
     * existing translation for the same input and language pair.
     * Returns `false` when the index already holds the
     * translation.
     */
    private fun insert(translation: Translation): Boolean {
        val key = indexKey(translation)
        if (archiveIndex[key] == translation) return false
        archiveIndex[key] = translation
        return true
    }

    private fun loadArchiveIfNeeded() {
        if (isArchiveLoaded) return
        isArchiveLoaded = true

        val archive = Persistent.archiveElements(PersistentStorageKey.translationArchive) { decode(it) }
        archiveIndex = archive.orEmpty().associateByTo(mutableMapOf()) { indexKey(it) }
    }

    /**
     * Writes the archive if a write is pending, streaming it to
     * disk one entry at a time. Runs on the persistence thread.
     */
    private fun persistPendingArchive() {
        val snapshot =
            ioLock.withLock {
                if (!isWritePending) return
                isWritePending = false
                archiveIndex.values.toList()
            }

        runCatching {
            Persistent.setArchive(
                PersistentStorageKey.translationArchive,
                snapshot.ifEmpty { null },
                ::encode,
            )
        }.onFailure { Logger.log(Exception.from(it, ExceptionMetadata(this))) }
    }

    /**
     * Schedules a write of the archive on the persistence thread.
     * Every change made before the write runs is coalesced into
     * it, so a burst of changes encodes the archive once. Call
     * while holding the I/O lock.
     */
    private fun schedulePersistence() {
        if (isWritePending) return
        isWritePending = true

        persistenceExecutor.schedule(
            { persistPendingArchive() },
            Floats.WRITE_DELAY_MILLISECONDS,
            TimeUnit.MILLISECONDS,
        )
    }

    private fun encode(translation: Translation): Map<String, Any?> =
        buildMap {
            put(
                KEY_INPUT,
                buildMap {
                    put(KEY_ORIGINAL, translation.input.original)
                    translation.input.alternate?.let { put(KEY_ALTERNATE, it) }
                },
            )
            put(KEY_LANGUAGE_PAIR, mapOf(KEY_FROM to translation.languagePair.from, KEY_TO to translation.languagePair.to))
            put(KEY_OUTPUT, translation.output)
        }

    // Reads the nested shape, falling back to the flat shape earlier
    // archives were written in.
    private fun decode(map: Map<String, Any?>): Translation? {
        val input = map[KEY_INPUT] as? Map<*, *> ?: map
        val languagePair = map[KEY_LANGUAGE_PAIR] as? Map<*, *> ?: map
        val original = input[KEY_ORIGINAL] as? String ?: return null
        val output = map[KEY_OUTPUT] as? String ?: return null
        val from = languagePair[KEY_FROM] as? String ?: return null
        val to = languagePair[KEY_TO] as? String ?: return null

        return Translation(
            input = TranslationInput(original, alternate = input[KEY_ALTERNATE] as? String),
            output = output,
            languagePair = LanguagePair(from = from, to = to),
        )
    }

    // MARK: - Companion

    companion object {
        /**
         * Registers a fresh persistent archiver with the translator
         * and, for cache clearing, with the subsystem.
         */
        fun registerWithDependencies() {
            val archiver = LocalTranslationArchiverDelegate()
            Translator.config.registerArchiverDelegate(archiver)
            AppSubsystem.delegates.registerTranslationArchiverDelegate(archiver)

            // A pending write reaches the disk as soon as the app
            // stops, before the process can be reclaimed.
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        archiver.persistenceExecutor.execute { archiver.persistPendingArchive() }
                    }
                },
            )
        }

        private const val KEY_INPUT = "input"
        private const val KEY_ORIGINAL = "original"
        private const val KEY_ALTERNATE = "alternate"
        private const val KEY_LANGUAGE_PAIR = "languagePair"
        private const val KEY_OUTPUT = "output"
        private const val KEY_FROM = "from"
        private const val KEY_TO = "to"
    }
}
