//
//  HostedTranslationArchiver.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.services

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.models.DataSample
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.getValues
import us.neotechnica.panther.networking.modules.database.services.CoreDatabaseStore
import us.neotechnica.panther.networking.modules.translation.extensions.decodedTranslationComponents
import us.neotechnica.panther.networking.modules.translation.extensions.reference
import us.neotechnica.panther.networking.modules.translation.extensions.translations
import us.neotechnica.panther.networking.modules.translation.models.TranslationConstants
import us.neotechnica.panther.networking.modules.translation.models.TranslationDataSample
import us.neotechnica.panther.networking.modules.translation.models.TranslationValidator
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.services.LocalTranslationArchiver
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

// LargeClass suppressed: the snapshot, derivation, and archive
// read/write responsibilities are cohesive.

/**
 * Reads and writes translations in the hosted RTDB archive, keyed
 * `translations/<from>-<to>/<encodedHash(input)>`.
 *
 * The archiver keeps a whole-tree snapshot of the hosted archive,
 * refreshed on an interval. With a fresh snapshot, lookups are served
 * from memory – making absence authoritative and enabling transitive
 * derivation of a new language pair from two archived ones – and fall
 * back to per-hash network reads only while the snapshot is stale.
 */
@Suppress("LargeClass", "TooManyFunctions")
internal class HostedTranslationArchiver {
    // MARK: - Types

    private data class State(
        val hasEnteredBackground: Boolean = false,
        val isPopulating: Boolean = false,
        val translationDataSample: TranslationDataSample = TranslationDataSample.empty,
    )

    // MARK: - Dependencies

    private val database get() = Networking.config.databaseDelegate

    private val localArchiver get() = Translator.config.archiverDelegate ?: LocalTranslationArchiver

    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val state = LockIsolated(State())

    // MARK: - Computed Properties

    private val hasFreshTranslationDataSnapshot: Boolean
        get() = state.wrappedValue.translationDataSample.let { !it.isEmpty && !it.isExpired }

    // MARK: - Init

    init {
        registerForegroundRefreshObserver()

        scope.launch {
            delay(INITIAL_REFRESH_DELAY_MILLIS)
            while (true) {
                refreshTranslationDataSnapshot()
                delay(nextRefreshDelayMillis())
            }
        }
    }

    // MARK: - Add to Hosted Archive

    suspend fun addToHostedArchive(translation: Translation) {
        TranslationValidator.validate(translation = translation, metadata = ExceptionMetadata(this))

        val entry =
            hostedArchiveEntry(translation) ?: throw Exception(
                "Translation language pair is idempotent; ineligible for hosted archive.",
                metadata = ExceptionMetadata(this),
            )

        database.commit(mapOf(entry.first to entry.second))

        Logger.log(
            Exception(
                "Added retrieved translation to hosted archive.",
                isReportable = false,
                userInfo = mapOf("ReferenceHostingKey" to translation.reference.hostingKey),
                metadata = ExceptionMetadata(this),
            ),
            domain = LoggerDomain.Networking.hostedTranslation,
        )
    }

    fun hostedArchiveEntry(translation: Translation): Pair<String, Any>? {
        try {
            TranslationValidator.validate(translation = translation, metadata = ExceptionMetadata(this))
        } catch (_: Exception) {
            return null
        }

        if (translation.languagePair.isIdempotent) return null

        val reference = translation.reference
        val referenceValue = reference.type.value ?: return null
        val key =
            listOf(
                NetworkPath.translations.rawValue,
                translation.languagePair.string,
                reference.type.key,
            ).joinToString("/")

        return key to referenceValue
    }

    // MARK: - Find Archived Translation

    suspend fun findArchivedTranslation(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation {
        val inputValueEncodedHash = encodedHashOf(listOf(input.value))

        // With a fresh snapshot, absence is authoritative; skip the per-hash network read.
        if (hasFreshTranslationDataSnapshot) {
            TranslationValidator.validate(languagePair = languagePair, metadata = ExceptionMetadata(this))
            archivedTranslationFromSnapshot(inputValueEncodedHash, languagePair)?.let { return it }
            return deriveTranslation(input, inputValueEncodedHash, languagePair)
        }

        return try {
            findArchivedTranslation(inputValueEncodedHash, languagePair)
        } catch (exception: Exception) {
            if (!exception.isEqual(to = AppException.Networking.Database.noValueExists)) throw exception
            deriveTranslation(input, inputValueEncodedHash, languagePair)
        }
    }

    suspend fun findArchivedTranslation(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation {
        val path = "${NetworkPath.translations.rawValue}/${languagePair.string}/$inputValueEncodedHash"
        val userInfo = mapOf<String, Any>("Path" to path)

        try {
            TranslationValidator.validate(languagePair = languagePair, metadata = ExceptionMetadata(this))
        } catch (exception: Exception) {
            throw exception.appending(userInfo = userInfo)
        }

        archivedTranslationFromSnapshot(inputValueEncodedHash, languagePair)?.let { return it }

        val raw: String =
            try {
                database.getValues<String>(path)
            } catch (exception: Exception) {
                if (!exception.isEqual(to = AppException.Networking.Database.noValueExists)) {
                    throw exception.appending(userInfo = userInfo)
                }

                return deriveTranslation(null, inputValueEncodedHash, languagePair)
            }

        val components =
            raw.decodedTranslationComponents ?: throw Exception.Networking
                .decodingFailed(
                    raw,
                    ExceptionMetadata(this),
                ).appending(userInfo = userInfo)

        return Translation(
            input = TranslationInput(components.first),
            output = components.second,
            languagePair = languagePair,
        )
    }

    // MARK: - Remove Archived Translation

    suspend fun removeArchivedTranslation(
        input: TranslationInput,
        languagePair: LanguagePair,
    ) {
        val path =
            listOf(
                Networking.config.environment.shortString,
                NetworkPath.translations.rawValue,
                languagePair.string,
            ).joinToString("/")

        database.updateChildValues(
            key = path,
            data = mapOf(encodedHashOf(listOf(input.value)) to null),
            prependingEnvironment = false,
        )

        CoreDatabaseStore.removeValue("$path/${encodedHashOf(listOf(input.value))}")
    }

    // MARK: - Auxiliary

    private fun archivedTranslationFromSnapshot(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation? {
        if (!hasFreshTranslationDataSnapshot) return null
        val dataForLanguagePair = state.wrappedValue.translationDataSample.data[languagePair.string] as? Map<*, *> ?: return null
        val components = (dataForLanguagePair[inputValueEncodedHash] as? String)?.decodedTranslationComponents ?: return null

        return Translation(
            input = TranslationInput(components.first),
            output = components.second,
            languagePair = languagePair,
        )
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private suspend fun deriveTranslation(
        originalInput: TranslationInput?,
        originalInputHash: String,
        originalLanguagePair: LanguagePair,
    ): Translation {
        if (hasFreshTranslationDataSnapshot) {
            val data = state.wrappedValue.translationDataSample.data
            for (archivedLanguagePairKey in data.keys) {
                val archivedLanguagePair = LanguagePair.fromString(archivedLanguagePairKey) ?: continue
                val derivedTranslation =
                    deriveTranslationFromPair(archivedLanguagePair, originalInput, originalInputHash, originalLanguagePair, data)
                        ?: continue

                if (!derivedTranslation.languagePair.isIdempotent) {
                    addToHostedArchive(derivedTranslation)
                }

                Logger.log(
                    Exception(
                        "Successfully derived translation from existing data.",
                        isReportable = false,
                        userInfo =
                            mapOf(
                                "IntermediateLanguagePair" to archivedLanguagePair.string,
                                "SynthesisLanguagePair" to "${archivedLanguagePair.to}-${originalLanguagePair.to}",
                                "TargetLanguagePair" to originalLanguagePair.string,
                            ),
                        metadata = ExceptionMetadata(this),
                    ),
                    domain = LoggerDomain.Networking.hostedTranslation,
                    with = AlertType.toastInPrerelease(style = ToastStyle.SUCCESS),
                )

                return derivedTranslation
            }
        }

        throw Exception("Failed to derive translation from existing data.", metadata = ExceptionMetadata(this))
    }

    private fun deriveTranslationFromPair(
        archivedLanguagePair: LanguagePair,
        originalInput: TranslationInput?,
        originalInputHash: String,
        originalLanguagePair: LanguagePair,
        data: Map<String, Any>,
    ): Translation? {
        val sourceData = data[archivedLanguagePair.string] as? Map<*, *> ?: return null
        val sourceComponents = (sourceData[originalInputHash] as? String)?.decodedTranslationComponents ?: return null
        val targetData = data["${archivedLanguagePair.to}-${originalLanguagePair.to}"] as? Map<*, *> ?: return null
        val targetEncoded = targetData[encodedHashOf(listOf(sourceComponents.second))] as? String ?: return null
        val targetComponents = targetEncoded.decodedTranslationComponents ?: return null

        return Translation(
            input = TranslationInput(originalInput?.value ?: sourceComponents.first),
            output = targetComponents.second,
            languagePair = originalLanguagePair,
        )
    }

    private fun nextRefreshDelayMillis(): Long {
        val sample = state.wrappedValue.translationDataSample
        val elapsed = System.currentTimeMillis() - sample.date
        return maxOf(REFRESH_INTERVAL_MILLIS - elapsed, MINIMUM_REFRESH_DELAY_MILLIS)
    }

    private suspend fun populateTranslationDataSnapshot() {
        val shouldProceed =
            state.withValue { ref ->
                val sample = ref.value.translationDataSample
                val isStale =
                    sample.isEmpty ||
                        sample.isExpired ||
                        (System.currentTimeMillis() - sample.date) >= REFRESH_INTERVAL_MILLIS
                if (ref.value.isPopulating || !isStale) {
                    false
                } else {
                    ref.value = ref.value.copy(isPopulating = true)
                    true
                }
            }

        if (!shouldProceed) return

        val translationData: Map<String, Any> =
            try {
                database.getValues<Map<String, Any>>(NetworkPath.translations.rawValue)
            } catch (exception: Exception) {
                state.withValue { it.value = it.value.copy(isPopulating = false) }
                throw exception
            }

        state.withValue {
            it.value =
                it.value.copy(
                    translationDataSample =
                        TranslationDataSample(
                            translationData,
                            TranslationConstants.TRANSLATION_DATA_SAMPLE_EXPIRY_THRESHOLD_MILLIS,
                        ),
                    isPopulating = false,
                )
        }

        Logger.log(
            "Populated translation data snapshot.",
            domain = LoggerDomain.Networking.hostedTranslation,
        )

        warmCachesFromSnapshot(translationData)
    }

    private fun warmCachesFromSnapshot(translationData: Map<String, Any>) {
        scope.launch {
            val captureDate = System.currentTimeMillis()
            val pathPrefix = "${Networking.config.environment.shortString}/${NetworkPath.translations.rawValue}/"
            val dataSamples = mutableMapOf<String, DataSample>()
            val translations = mutableSetOf<Translation>()

            for ((languagePairKey, value) in translationData) {
                val languagePairData = value as? Map<*, *> ?: continue
                val keyPrefix = "$pathPrefix$languagePairKey/"
                val languagePair = LanguagePair.fromString(languagePairKey)

                for ((translationKey, translationValue) in languagePairData) {
                    val key = translationKey as? String ?: continue
                    dataSamples["$keyPrefix$key"] =
                        DataSample(
                            data = translationValue as Any,
                            expiryThreshold = DATA_SAMPLE_EXPIRY_MILLIS,
                            date = captureDate,
                        )

                    val components = (translationValue as? String)?.decodedTranslationComponents
                    if (languagePair != null && components != null) {
                        translations.add(Translation(TranslationInput(components.first), components.second, languagePair))
                    }
                }
            }

            runCatching { CoreDatabaseStore.addValues(dataSamples) }
            runCatching { localArchiver.addValues(translations) }
        }
    }

    private suspend fun refreshTranslationDataSnapshot() {
        try {
            populateTranslationDataSnapshot()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Logger.log(exception, domain = LoggerDomain.Networking.hostedTranslation)
        }
    }

    // Lifecycle observation requires a main looper; environments
    // without one (such as unit tests) skip registration.
    private fun registerForegroundRefreshObserver() {
        val mainDispatcher =
            runCatching {
                Dispatchers.Main.also { it.isDispatchNeeded(EmptyCoroutineContext) }
            }.getOrNull() ?: return

        runCatching {
            scope.launch(mainDispatcher) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(
                    object : DefaultLifecycleObserver {
                        override fun onStart(owner: LifecycleOwner) {
                            // The foreground callback also fires during cold
                            // launch, where the snapshot download would
                            // monopolize the realtime socket and starve the
                            // launch-critical reads queued behind it; refresh
                            // only on genuine background-to-foreground returns
                            // and leave initial population to the delayed
                            // refresh loop.
                            val hasEnteredBackground = state.wrappedValue.hasEnteredBackground
                            if (!hasEnteredBackground) return

                            scope.launch { refreshTranslationDataSnapshot() }
                        }

                        override fun onStop(owner: LifecycleOwner) {
                            state.withValue { it.value = it.value.copy(hasEnteredBackground = true) }
                        }
                    },
                )
            }
        }
    }

    // MARK: - Companion

    private companion object {
        const val DATA_SAMPLE_EXPIRY_MILLIS = 600_000L
        const val INITIAL_REFRESH_DELAY_MILLIS = 10_000L
        const val MINIMUM_REFRESH_DELAY_MILLIS = 10_000L
        const val REFRESH_INTERVAL_MILLIS = TranslationConstants.TRANSLATION_DATA_SAMPLE_REFRESH_INTERVAL_MILLIS
    }
}
