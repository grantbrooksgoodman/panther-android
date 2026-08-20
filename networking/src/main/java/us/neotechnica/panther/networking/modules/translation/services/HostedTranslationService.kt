//
//  HostedTranslationService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.encodedHash
import us.neotechnica.panther.networking.modules.translation.extensions.sanitized
import us.neotechnica.panther.networking.modules.translation.extensions.system
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.ArchiveStrategy
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationValidator
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.extensions.containsLetters
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.services.LanguageRecognitionService
import us.neotechnica.panther.translator.services.LocalTranslationArchiver
import us.neotechnica.panther.translator.services.TranslationService

/**
 * Coordinates translation across the local archive, the hosted RTDB
 * archive, and the underlying [TranslationService].
 *
 * A [translate] call short-circuits on idempotent pairs, local-archive
 * hits, and text already in the target language; otherwise it consults
 * the hosted archive, falls back to a live web/API translation, and
 * writes the result to both archives.
 *
 * **Note:** the iOS original also performs AI (Gemini) enhancement and
 * shows a HUD; both are deferred here per the Phase 4 plan.
 */
class HostedTranslationService private constructor() : HostedTranslationDelegate {
    // MARK: - Types

    private enum class ArchiveTreatment {
        ADD_TO_BOTH,
        ADD_TO_HOSTED,
        ADD_TO_LOCAL,
    }

    // MARK: - Properties

    private val archiver = HostedTranslationArchiver()

    private val localArchiver get() = Translator.config.archiverDelegate ?: LocalTranslationArchiver

    // MARK: - Delegate: Reads

    override suspend fun findArchivedTranslation(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation = archiver.findArchivedTranslation(inputValueEncodedHash, languagePair)

    override fun hostedArchiveEntry(translation: Translation): Pair<String, Any>? = archiver.hostedArchiveEntry(translation)

    // MARK: - Delegate: Translate

    override suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
        archiveStrategy: ArchiveStrategy,
    ): Translation {
        prevalidateInput(input, languagePair, archiveStrategy)?.let { return it }

        Networking.config.activityIndicatorDelegate.show()
        try {
            checkHostedArchive(input, languagePair)?.let { return it }
            val translation = TranslationService.translate(input, languagePair)
            return postProcess(translation, ArchiveTreatment.ADD_TO_BOTH, archiveStrategy)
        } finally {
            Networking.config.activityIndicatorDelegate.hide()
        }
    }

    override suspend fun getTranslations(
        inputs: List<TranslationInput>,
        languagePair: LanguagePair,
    ): List<Translation> =
        coroutineScope {
            inputs
                .chunked(MAX_CONCURRENT_TRANSLATIONS)
                .flatMap { chunk ->
                    chunk.map { input -> async { translate(input, languagePair) } }.awaitAll()
                }
        }

    // MARK: - Delegate: Resolve

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

    // MARK: - Auxiliary

    private suspend fun prevalidateInput(
        input: TranslationInput,
        languagePair: LanguagePair,
        archiveStrategy: ArchiveStrategy,
    ): Translation? {
        TranslationValidator.validate(sender = this, inputs = listOf(input), languagePair = languagePair)

        if (languagePair.isIdempotent) {
            return postProcess(
                Translation(input = input, output = input.value.sanitized, languagePair = languagePair),
                treatment = null,
                archiveStrategy = archiveStrategy,
            )
        }

        val archived = localArchiver.getValue(input.value.encodedHash, languagePair)
        if (archived != null) {
            if (!archived.isWellFormed || archived.input.value == archived.output) {
                localArchiver.removeValue(input.value.encodedHash, languagePair)
                return null
            }
            return postProcess(archived, treatment = null, archiveStrategy = archiveStrategy)
        }

        val hasLetters = input.value.containsLetters()
        val alreadyTarget =
            LanguageRecognitionService.shared.matchConfidence(
                input.value,
                languagePair.to,
            ) > TARGET_LANGUAGE_CONFIDENCE

        if (!hasLetters || alreadyTarget) {
            return postProcess(
                Translation(input = input, output = input.value.sanitized, languagePair = languagePair),
                treatment = ArchiveTreatment.ADD_TO_BOTH,
                archiveStrategy = archiveStrategy,
            )
        }

        return null
    }

    private suspend fun checkHostedArchive(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation? {
        val translation =
            try {
                archiver.findArchivedTranslation(input, languagePair)
            } catch (_: Exception) {
                return null
            }

        val failsValidation =
            try {
                TranslationValidator.validate(sender = this, translation = translation)
                false
            } catch (_: Exception) {
                true
            }

        if (failsValidation || translation.input.value == translation.output) {
            archiver.removeArchivedTranslation(input, languagePair)
            return null
        }

        return postProcess(translation, ArchiveTreatment.ADD_TO_LOCAL, ArchiveStrategy.IMMEDIATE)
    }

    private suspend fun postProcess(
        translation: Translation,
        treatment: ArchiveTreatment?,
        archiveStrategy: ArchiveStrategy,
    ): Translation {
        TranslationValidator.validate(sender = this, translation = translation)

        if (archiveStrategy == ArchiveStrategy.IMMEDIATE &&
            (treatment == ArchiveTreatment.ADD_TO_BOTH || treatment == ArchiveTreatment.ADD_TO_HOSTED)
        ) {
            archiver.addToHostedArchive(translation)
        }

        if (translation.input.value != translation.output &&
            (treatment == ArchiveTreatment.ADD_TO_BOTH || treatment == ArchiveTreatment.ADD_TO_LOCAL)
        ) {
            localArchiver.addValue(translation)
        }

        return translation
    }

    // MARK: - Companion

    companion object {
        /** The shared hosted translation service. */
        val shared = HostedTranslationService()

        private const val TARGET_LANGUAGE_CONFIDENCE = 0.8f
        private const val MAX_CONCURRENT_TRANSLATIONS = 10
    }
}
