//
//  TranslationService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.extensions.capitalized
import us.neotechnica.panther.translator.extensions.containsLetters
import us.neotechnica.panther.translator.extensions.encodedHash
import us.neotechnica.panther.translator.extensions.lowercasedTrimmingWhitespaceAndNewlines
import us.neotechnica.panther.translator.extensions.replacing
import us.neotechnica.panther.translator.extensions.tokenized
import us.neotechnica.panther.translator.extensions.trimmingTrailingWhitespaceAndNewlines
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationError
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.models.TranslationPlatform

/**
 * Translates text between languages using multiple providers.
 *
 * [translate] attempts Google first, then DeepL and Reverso, and
 * finally Lara, accepting the first provider whose output differs from
 * the input. Completed translations are cached through the registered
 * archiver (or [LocalTranslationArchiver]); a subsequent request for
 * the same input and language pair returns the cached result without a
 * network round-trip. Concurrent identical requests are coalesced.
 */
object TranslationService {
    // MARK: - Properties

    private val coalescer = Coalescer<String, Translation>()

    // MARK: - Prewarm

    /**
     * Warms the underlying network connections to translation
     * service hosts.
     *
     * Call this method early in the app lifecycle (for example, at
     * launch) to establish DNS resolution and TLS sessions ahead of
     * the first translation request, reducing latency on the first
     * call to [translate] without retaining any web views.
     *
     * @param platforms The platforms to prewarm connections for.
     *   Defaults to every platform.
     */
    fun prewarm(platforms: List<TranslationPlatform> = TranslationPlatform.entries) {
        BaseTranslator.prewarm(platforms)

        // Decode and index the local archive ahead of the first lookup.
        if (Translator.config.archiverDelegate == null) {
            LocalTranslationArchiver.preload()
        }
    }

    // MARK: - Translate

    /**
     * Translates [input] into the target language with automatic
     * provider fallback, coalescing concurrent identical requests.
     */
    suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation =
        coalescer("${input.value.encodedHash}|${languagePair.string}") {
            translateWithFallback(input, languagePair)
        }

    /**
     * Translates [input] using a single [platform], without fallback.
     */
    suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
        platform: TranslationPlatform,
    ): Translation {
        val tokenizedInput = input.withTokenizedDetectorAttributes
        if (!tokenizedInput.isWellFormed || !languagePair.isWellFormed) {
            throw TranslationError.InvalidArguments
        }

        val archiver = Translator.config.archiverDelegate ?: LocalTranslationArchiver
        val delimiter = Translator.Constants.PROCESSING_DELIMITER

        if (!tokenizedInput.value.containsLetters() || languagePair.isIdempotent) {
            return Translation(
                input = tokenizedInput,
                output = tokenizedInput.value.replace(delimiter, ""),
                languagePair = languagePair,
            )
        }

        archivedTranslation(archiver, tokenizedInput, languagePair)?.let { archived ->
            if (!archived.isWellFormed) {
                archiver.removeValue(
                    inputValueEncodedHash = archived.input.value.encodedHash,
                    languagePair = archived.languagePair,
                )
                return translate(tokenizedInput, languagePair, platform)
            }
            return Translation(input = tokenizedInput, output = archived.output, languagePair = languagePair)
        }

        if (LanguageRecognitionService.shared.matchConfidence(
                tokenizedInput.value,
                languagePair.to,
            ) > TARGET_LANGUAGE_CONFIDENCE
        ) {
            return Translation(
                input = tokenizedInput,
                output = tokenizedInput.value.replace(delimiter, ""),
                languagePair = languagePair,
            )
        }

        return translateAndArchive(tokenizedInput, languagePair, platform, archiver)
    }

    // MARK: - Get Translations

    /**
     * Translates multiple inputs into the target language, up to ten
     * concurrently, returning results in input order.
     */
    suspend fun getTranslations(
        inputs: List<TranslationInput>,
        languagePair: LanguagePair,
    ): List<Translation> {
        if (inputs.isEmpty() || !inputs.all { it.isWellFormed } || !languagePair.isWellFormed) {
            throw TranslationError.InvalidArguments
        }

        // Pre-allocate result slots to preserve order.
        val translations = arrayOfNulls<Translation>(inputs.size)

        coroutineScope {
            val semaphore = Semaphore(minOf(MAX_CONCURRENT_TRANSLATIONS, inputs.size))
            inputs
                .mapIndexed { index, input ->
                    async {
                        semaphore.withPermit {
                            translations[index] = translate(input, languagePair)
                        }
                    }
                }.awaitAll()
        }

        if (translations.any { it == null }) {
            throw TranslationError.Unknown("Batch translation results were incomplete.")
        }

        return translations.filterNotNull()
    }

    // MARK: - Auxiliary

    private suspend fun translateWithFallback(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation {
        for (platform in FALLBACK_PLATFORMS) {
            val translation = runCatching { translate(input, languagePair, platform) }.getOrNull()
            if (translation != null &&
                translation.output.lowercasedTrimmingWhitespaceAndNewlines !=
                input.value.lowercasedTrimmingWhitespaceAndNewlines
            ) {
                return translation
            }
        }

        return translate(input, languagePair, TranslationPlatform.LARA)
    }

    private fun archivedTranslation(
        archiver: us.neotechnica.panther.translator.interfaces.TranslationArchiverDelegate,
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation? =
        archiver.getValue(
            inputValueEncodedHash = input.value.encodedHash,
            languagePair = languagePair,
        ) ?: archiver.getValue(
            inputValueEncodedHash = input.value.trimmingTrailingWhitespaceAndNewlines.encodedHash,
            languagePair = languagePair,
        )

    private suspend fun translateAndArchive(
        input: TranslationInput,
        languagePair: LanguagePair,
        platform: TranslationPlatform,
        archiver: us.neotechnica.panther.translator.interfaces.TranslationArchiverDelegate,
    ): Translation {
        val delimiter = Translator.Constants.PROCESSING_DELIMITER
        val processingToken = Translator.Constants.PROCESSING_TOKEN

        val inputTokens = input.value.tokenized(delimiter)
        val translation =
            platform.instance.translate(
                TranslationInput(inputTokens.first.trimmingTrailingWhitespaceAndNewlines),
                languagePair,
            )

        if (inputTokens.second.isNotEmpty() && !translation.output.contains(processingToken)) {
            throw TranslationError.MalformedTranslationResult
        }

        val processedOutput =
            translation.output
                .replacing(processingToken, with = inputTokens.second)
                .replace(processingToken, "")
                .replace(delimiter, "")
                .trimmingTrailingWhitespaceAndNewlines
                .capitalized(relativeTo = input.value)

        val processedTranslation =
            Translation(
                input = input,
                output = processedOutput,
                languagePair = translation.languagePair,
            )

        if (!processedTranslation.isWellFormed) throw TranslationError.MalformedTranslationResult

        archiver.addValue(processedTranslation)
        return processedTranslation
    }

    // MARK: - Companion

    private const val TARGET_LANGUAGE_CONFIDENCE = 0.8f
    private const val MAX_CONCURRENT_TRANSLATIONS = 10

    private val FALLBACK_PLATFORMS =
        listOf(
            TranslationPlatform.GOOGLE,
            TranslationPlatform.DEEP_L,
            TranslationPlatform.REVERSO,
        )
}
