//
//  LanguageRecognitionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Estimates how confidently a string belongs to a given language.
 *
 * Combines two identification legs (0.4 each) from ML Kit's language
 * identifier with a 0.2 spell-validation leg. No synchronous spell
 * checker is available, so the spell leg never contributes and the
 * composite confidence tops out at 0.8.
 */
class LanguageRecognitionService private constructor() {
    // MARK: - Properties

    private val cachedResults = ConcurrentHashMap<CacheKey, Float>()

    private val identifier by lazy { LanguageIdentification.getClient() }

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

        if (isValidSentence(string, languageCode)) confidence += SPELL_LEG

        cachedResults[cacheKey] = confidence
        return confidence
    }

    // MARK: - Auxiliary

    private val String.sanitized: String
        get() = lowercase().trim()

    // No synchronous spell checker is available, so spell validation
    // never passes.
    @Suppress("FunctionOnlyReturningConstant", "UnusedParameter", "UnusedPrivateMember")
    private fun isValidSentence(
        string: String,
        languageCode: String,
    ): Boolean = false

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
    }
}
