//
//  HostedTranslationService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import us.neotechnica.panther.designsystem.modules.alertkit.models.HUDConfig
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.encodedHash
import us.neotechnica.panther.networking.modules.translation.extensions.englishLanguageName
import us.neotechnica.panther.networking.modules.translation.extensions.sanitized
import us.neotechnica.panther.networking.modules.translation.extensions.system
import us.neotechnica.panther.networking.modules.translation.extensions.trimmingTrailingWhitespace
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.ArchiveStrategy
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationValidator
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.extensions.containsLetters
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.services.LanguageRecognitionService
import us.neotechnica.panther.translator.services.LocalTranslationArchiver
import us.neotechnica.panther.translator.services.TranslationService
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

// This service exceeds the file-length and type-body-length limits.

/**
 * Coordinates translation across the local archive, the hosted RTDB
 * archive, and the underlying [TranslationService].
 *
 * A [translate] call short-circuits on idempotent pairs, local-archive
 * hits, and text already in the target language; otherwise it consults
 * the hosted archive, falls back to a live web/API translation, and
 * writes the result to both archives.
 */
@Suppress("LargeClass", "TooManyFunctions")
class HostedTranslationService private constructor() : HostedTranslationDelegate {
    // MARK: - Types

    private enum class ArchiveTreatment {
        ADD_TO_BOTH_ARCHIVES,
        ADD_TO_HOSTED_ARCHIVE,
        ADD_TO_LOCAL_ARCHIVE,
    }

    private sealed interface PreprocessingResult {
        data class ArchiveHit(
            val translation: Translation,
        ) : PreprocessingResult

        data object ArchiveMiss : PreprocessingResult
    }

    // MARK: - Properties

    private val archiver = HostedTranslationArchiver()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Computed Properties

    private val localTranslationArchiver get() = Translator.config.archiverDelegate ?: LocalTranslationArchiver

    // MARK: - Find Archived Translation

    override suspend fun findArchivedTranslation(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation = archiver.findArchivedTranslation(inputValueEncodedHash, languagePair)

    // MARK: - Hosted Archive Entry

    override fun hostedArchiveEntry(translation: Translation): Pair<String, Any>? = archiver.hostedArchiveEntry(translation)

    // MARK: - Label String Resolution

    override suspend fun resolve(strings: TranslatedLabelStrings): List<TranslationOutputMap> {
        val system = LanguagePair.system
        if (!system.isWellFormed || system.isIdempotent) return strings.defaultOutputMap

        val translations = getTranslations(strings.keyPairs.map { it.input }, system)

        return strings.keyPairs.map { keyPair ->
            val translation = translations.firstOrNull { it.input.value == keyPair.input.value }
            if (translation != null) {
                TranslationOutputMap(key = keyPair.key, value = translation.output)
            } else {
                keyPair.defaultOutputMap
            }
        }
    }

    // MARK: - Translation

    override suspend fun getTranslations(
        inputs: List<TranslationInput>,
        languagePair: LanguagePair,
        hudConfig: HUDConfig?,
    ): List<Translation> {
        val resolvedTranslations = arrayOfNulls<Translation>(inputs.size)
        val archiveMisses = mutableListOf<Pair<Int, TranslationInput>>()

        coroutineScope {
            inputs
                .mapIndexed { index, input ->
                    async {
                        index to preprocess(input, languagePair)
                    }
                }.awaitAll()
                .forEach { (index, preprocessingResult) ->
                    when (preprocessingResult) {
                        is PreprocessingResult.ArchiveHit -> resolvedTranslations[index] = preprocessingResult.translation
                        PreprocessingResult.ArchiveMiss -> archiveMisses.add(index to inputs[index])
                    }
                }
        }

        if (archiveMisses.isEmpty()) return resolvedTranslations.filterNotNull()

        Networking.config.activityIndicatorDelegate.show()
        try {
            val missedInputs =
                archiveMisses.map { (_, input) ->
                    TranslationInput(
                        input.value.trimmingTrailingWhitespace,
                        alternate = input.alternate?.trimmingTrailingWhitespace,
                    )
                }

            val translations =
                try {
                    TranslationService.getTranslations(missedInputs, languagePair)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    throw (throwable as? Exception) ?: Exception.from(throwable, ExceptionMetadata(this))
                }

            if (translations.size != archiveMisses.size) {
                throw Exception(
                    "Mismatched ratio returned.",
                    metadata = ExceptionMetadata(this),
                )
            }

            coroutineScope {
                archiveMisses
                    .zip(translations)
                    .map { (slot, translation) ->
                        async {
                            slot.first to
                                postProcess(
                                    translation,
                                    archiveTreatment = ArchiveTreatment.ADD_TO_BOTH_ARCHIVES,
                                    archiveStrategy = ArchiveStrategy.IMMEDIATE,
                                )
                        }
                    }.awaitAll()
                    .forEach { (index, processedTranslation) ->
                        resolvedTranslations[index] =
                            Translation(
                                input = inputs[index],
                                output = processedTranslation.output,
                                languagePair = processedTranslation.languagePair,
                            )
                    }
            }

            val finalTranslations = resolvedTranslations.filterNotNull()
            if (finalTranslations.size != inputs.size) {
                throw Exception(
                    "Mismatched ratio returned.",
                    metadata = ExceptionMetadata(this),
                )
            }

            return finalTranslations
        } finally {
            Networking.config.activityIndicatorDelegate.hide()
        }
    }

    override suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
        hudConfig: HUDConfig?,
        archiveStrategy: ArchiveStrategy,
    ): Translation {
        prevalidateInput(input, languagePair, archiveStrategy)?.let { return it }

        Networking.config.activityIndicatorDelegate.show()
        try {
            checkHostedArchive(input, languagePair)?.let { return it }

            val sourceLanguageName = languagePair.from.englishLanguageName ?: languagePair.from.uppercase()
            val targetLanguageName = languagePair.to.englishLanguageName ?: languagePair.to.uppercase()

            Logger.log(
                Exception(
                    "Translating text from $sourceLanguageName to $targetLanguageName.",
                    isReportable = false,
                    userInfo =
                        mapOf(
                            "InputValue" to input.value,
                            "LanguagePair" to languagePair.string,
                        ),
                    metadata = ExceptionMetadata(this),
                ),
                domain = LoggerDomain.Networking.hostedTranslation,
            )

            return try {
                postProcess(
                    translateWithTranslator(input, languagePair, hudConfig),
                    archiveTreatment = ArchiveTreatment.ADD_TO_BOTH_ARCHIVES,
                    archiveStrategy = archiveStrategy,
                )
            } catch (exception: Exception) {
                if (!exception.isEqual(
                        toAny =
                            listOf(
                                AppException.Networking.Translation.exhaustedAvailablePlatforms,
                                AppException.Networking.Translation.sameTranslationInputOutput,
                            ),
                    )
                ) {
                    throw exception
                }

                postProcess(
                    Translation(
                        input = input,
                        output = input.value.sanitized,
                        languagePair = languagePair,
                    ),
                    archiveTreatment = ArchiveTreatment.ADD_TO_BOTH_ARCHIVES,
                    archiveStrategy = archiveStrategy,
                )
            }
        } finally {
            Networking.config.activityIndicatorDelegate.hide()
        }
    }

    // MARK: - Auxiliary

    private suspend fun checkHostedArchive(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation? {
        val translation =
            try {
                archiver.findArchivedTranslation(input, languagePair)
            } catch (exception: Exception) {
                if (!exception.isEqual(
                        toAny =
                            listOf(
                                AppException.Networking.Database.noValueExists,
                                AppException.Networking.Translation.translationDerivationFailed,
                            ),
                    )
                ) {
                    throw exception
                }

                return null
            }

        val translationFailsValidation =
            try {
                TranslationValidator.validate(
                    translation = translation,
                    metadata = ExceptionMetadata(this),
                )
                false
            } catch (_: Exception) {
                true
            }

        if (translationFailsValidation || translation.input.value == translation.output) {
            archiver.removeArchivedTranslation(input, languagePair)
            return null
        }

        return postProcess(
            translation,
            archiveTreatment = ArchiveTreatment.ADD_TO_LOCAL_ARCHIVE,
            archiveStrategy = ArchiveStrategy.IMMEDIATE,
        )
    }

    private suspend fun postProcess(
        translation: Translation,
        archiveTreatment: ArchiveTreatment?,
        archiveStrategy: ArchiveStrategy,
    ): Translation {
        TranslationValidator.validate(
            translation = translation,
            metadata = ExceptionMetadata(this),
        )

        if (archiveStrategy == ArchiveStrategy.IMMEDIATE &&
            (
                archiveTreatment == ArchiveTreatment.ADD_TO_BOTH_ARCHIVES ||
                    archiveTreatment == ArchiveTreatment.ADD_TO_HOSTED_ARCHIVE
            )
        ) {
            archiver.addToHostedArchive(translation)
        }

        if (translation.input.value == translation.output ||
            (
                archiveTreatment != ArchiveTreatment.ADD_TO_BOTH_ARCHIVES &&
                    archiveTreatment != ArchiveTreatment.ADD_TO_LOCAL_ARCHIVE
            )
        ) {
            return translation
        }

        localTranslationArchiver.addValue(translation)
        return translation
    }

    private suspend fun preprocess(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): PreprocessingResult {
        prevalidateInput(
            input,
            languagePair = languagePair,
            archiveStrategy = ArchiveStrategy.IMMEDIATE,
        )?.let { return PreprocessingResult.ArchiveHit(it) }

        checkHostedArchive(
            input,
            languagePair = languagePair,
        )?.let { return PreprocessingResult.ArchiveHit(it) }

        return PreprocessingResult.ArchiveMiss
    }

    private suspend fun prevalidateInput(
        input: TranslationInput,
        languagePair: LanguagePair,
        archiveStrategy: ArchiveStrategy,
    ): Translation? {
        TranslationValidator.validate(
            inputs = listOf(input),
            languagePair = languagePair,
            metadata = ExceptionMetadata(this),
        )

        // If language pair is idempotent, return original input.

        if (languagePair.isIdempotent) {
            return postProcess(
                Translation(input = input, output = input.value.sanitized, languagePair = languagePair),
                archiveTreatment = null,
                archiveStrategy = archiveStrategy,
            )
        }

        // Attempt to find a suitable locally archived translation.

        val archivedTranslation = localTranslationArchiver.getValue(input.value.encodedHash, languagePair)
        if (archivedTranslation != null) {
            val translationFailsValidation =
                try {
                    TranslationValidator.validate(
                        translation = archivedTranslation,
                        metadata = ExceptionMetadata(this),
                    )
                    false
                } catch (_: Exception) {
                    true
                }

            if (translationFailsValidation || archivedTranslation.input.value == archivedTranslation.output) {
                localTranslationArchiver.removeValue(input.value.encodedHash, languagePair)
                return null
            }

            return postProcess(
                archivedTranslation,
                archiveTreatment = null,
                archiveStrategy = archiveStrategy,
            )
        }

        // If no letters or input is already in target language, return original input.

        val hasUnicodeLetters = input.value.containsLetters()
        val sameInputOutputLanguage =
            LanguageRecognitionService.shared.matchConfidence(
                input.value,
                languagePair.to,
            ) > TARGET_LANGUAGE_CONFIDENCE

        if (!hasUnicodeLetters || sameInputOutputLanguage) {
            return postProcess(
                Translation(input = input, output = input.value.sanitized, languagePair = languagePair),
                archiveTreatment = ArchiveTreatment.ADD_TO_BOTH_ARCHIVES,
                archiveStrategy = archiveStrategy,
            )
        }

        return null
    }

    // Bounds the live translation and drives the optional HUD; the
    // translator call itself carries no presentation concerns.
    private suspend fun translateWithTranslator(
        input: TranslationInput,
        languagePair: LanguagePair,
        hudConfig: HUDConfig?,
    ): Translation {
        val hudJob: Job? =
            hudConfig?.let { config ->
                scope.launch {
                    delay(config.appearsAfter)
                    AppSubsystem.delegates.hud?.showProgress(isModal = config.isModal)
                }
            }

        try {
            return withTimeoutOrNull(TRANSLATOR_TIMEOUT) {
                try {
                    TranslationService.translate(
                        TranslationInput(
                            input.value.trimmingTrailingWhitespace,
                            alternate = input.alternate?.trimmingTrailingWhitespace,
                        ),
                        languagePair,
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    throw (throwable as? Exception) ?: Exception.from(throwable, ExceptionMetadata(this))
                }
            } ?: throw Exception.timedOut(ExceptionMetadata(this))
        } finally {
            hudJob?.cancel()
            if (hudConfig != null) AppSubsystem.delegates.hud?.hide()
        }
    }

    // MARK: - Companion

    companion object {
        /** The shared hosted translation service. */
        val shared = HostedTranslationService()

        private const val TARGET_LANGUAGE_CONFIDENCE = 0.8f
        private val TRANSLATOR_TIMEOUT = 10.seconds
    }
}
