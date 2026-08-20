//
//  LanguageRecognitionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Estimates how confidently a string belongs to a given language.
 *
 * The iOS original combines Apple's `NLLanguageRecognizer` (two 0.4
 * legs: the dominant language and the top hypothesis) with a
 * `UITextChecker` spell heuristic (a 0.2 leg). Android has no headless
 * spell checker, so this port derives the two identification legs from
 * ML Kit's language identifier and lets the spell leg pass through as a
 * constant 0.2. The result is that a string both ML legs agree on
 * scores 1.0 – matching iOS's all-three-legs outcome, which is what the
 * `> 0.8` "already in the target language" short-circuit checks for.
 */
class LanguageRecognitionService private constructor() {
    // MARK: - Properties

    private val cachedResults = ConcurrentHashMap<CacheKey, Float>()

    private val identifier by lazy {
        LanguageIdentification.getClient(
            LanguageIdentificationOptions
                .Builder()
                .setConfidenceThreshold(HYPOTHESIS_FLOOR)
                .build(),
        )
    }

    // MARK: - Match Confidence

    /**
     * Returns a confidence in `0.0...1.0` that [string] is written in
     * the language identified by [languageCode].
     *
     * @param string The text to analyze.
     * @param languageCode The ISO 639-1 code to test against.
     */
    suspend fun matchConfidence(
        string: String,
        languageCode: String,
    ): Float {
        val cacheKey = CacheKey(string, languageCode)
        cachedResults[cacheKey]?.let { return it }

        var confidence = 0f

        val dominantLanguage = identifier.identifyLanguage(string).await()
        if (dominantLanguage != UNDETERMINED &&
            dominantLanguage.sanitized.startsWith(languageCode.sanitized)
        ) {
            confidence += IDENTIFICATION_LEG
        }

        val topHypothesis =
            identifier
                .identifyPossibleLanguages(string)
                .await()
                .maxByOrNull(IdentifiedLanguage::getConfidence)
        if (topHypothesis != null &&
            topHypothesis.languageTag.sanitized.startsWith(languageCode.sanitized) &&
            topHypothesis.confidence >= HYPOTHESIS_CONFIDENCE_THRESHOLD
        ) {
            confidence += IDENTIFICATION_LEG
        }

        // Spell leg (deviation): passes through, as noted in the type docs.
        confidence += SPELL_LEG

        cachedResults[cacheKey] = confidence
        return confidence
    }

    // MARK: - Auxiliary

    private val String.sanitized: String
        get() = lowercase().trim()

    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result -> continuation.resume(result) }
            addOnFailureListener { error -> continuation.resumeWithException(error) }
        }

    // MARK: - Cache Key

    private data class CacheKey(
        val string: String,
        val languageCode: String,
    )

    // MARK: - Companion

    companion object {
        /** The shared recognition service. */
        val shared = LanguageRecognitionService()

        private const val UNDETERMINED = "und"
        private const val IDENTIFICATION_LEG = 0.4f
        private const val SPELL_LEG = 0.2f
        private const val HYPOTHESIS_CONFIDENCE_THRESHOLD = 0.45f
        private const val HYPOTHESIS_FLOOR = 0.1f
    }
}
