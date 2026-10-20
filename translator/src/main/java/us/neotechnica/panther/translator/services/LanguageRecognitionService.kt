//
//  LanguageRecognitionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import android.content.Context
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import us.neotechnica.panther.translator.Translator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Estimates how confidently a string belongs to a given language.
 *
 * Use [LanguageRecognitionService] to decide whether text already
 * reads as a given language before translating it:
 *
 * ```kotlin
 * val confidence =
 *     LanguageRecognitionService.shared.matchConfidence(
 *         "Bonjour le monde",
 *         languageCode = "fr",
 *     )
 * ```
 *
 * The confidence is a composite of three weighted signals: whether
 * the identified dominant language matches the code (0.4), whether
 * the top language hypothesis matches it with at least 45%
 * probability (0.4), and whether most of the string's words pass
 * spell checking for that language (0.2). Results are cached for
 * the lifetime of the service; identical queries return without
 * recomputation.
 */
class LanguageRecognitionService internal constructor(
    private val dependencies: Dependencies = Dependencies(),
) {
    // MARK: - Types

    // Reports which of the given words are misspelled in a language,
    // or `null` when no checker is available for it.
    internal fun interface SpellChecker {
        suspend fun misspelledWords(
            words: List<String>,
            languageCode: String,
        ): Set<String>?
    }

    private data class CacheKey(
        val string: String,
        val languageCode: String,
    )

    // MARK: - Dependencies

    internal data class Dependencies(
        val identifyLanguage: suspend (String) -> String = { identifier.identifyLanguage(it).await() },
        val identifyPossibleLanguages: suspend (String) -> List<Pair<String, Float>> = { string ->
            identifier.identifyPossibleLanguages(string).await().map { it.languageTag to it.confidence }
        },
        val spellChecker: SpellChecker? = SystemSpellChecker,
    )

    // MARK: - Properties

    private val cachedResults = ConcurrentHashMap<CacheKey, Float>()

    // MARK: - Methods

    /**
     * Returns a confidence in `0.0...1.0` that [string] is written in
     * the language identified by [languageCode].
     *
     * A value greater than 0.8 indicates the text very likely already
     * reads as that language.
     *
     * @param string The text to evaluate.
     * @param languageCode The ISO 639-1 code to test against.
     *
     * @return A value between `0` and `1`, where higher values
     *   indicate greater confidence that the string is written in
     *   the specified language.
     */
    suspend fun matchConfidence(
        string: String,
        languageCode: String,
    ): Float {
        val cacheKey = CacheKey(string, languageCode)
        cachedResults[cacheKey]?.let { return it }

        var confidenceValue = 0f

        val dominantLanguageCode = dependencies.identifyLanguage(string)
        if (dominantLanguageCode != UNDETERMINED &&
            dominantLanguageCode.sanitized.startsWith(languageCode.sanitized)
        ) {
            confidenceValue += IDENTIFICATION_LEG
        }

        val dominantHypothesis = dependencies.identifyPossibleLanguages(string).maxByOrNull { it.second }
        if (dominantHypothesis != null &&
            dominantHypothesis.first.sanitized.startsWith(languageCode.sanitized) &&
            dominantHypothesis.second >= HYPOTHESIS_CONFIDENCE_THRESHOLD
        ) {
            confidenceValue += IDENTIFICATION_LEG
        }

        if (isValidSentence(string, languageCode)) confidenceValue += SPELL_LEG

        cachedResults[cacheKey] = confidenceValue
        return confidenceValue
    }

    // MARK: - Auxiliary

    private val String.sanitized: String
        get() = lowercase().trim()

    private suspend fun isValidSentence(
        string: String,
        languageCode: String,
    ): Boolean {
        // Skip tiny words, numbers, URL-like, and punctuation-heavy tokens.
        fun shouldCheck(word: String): Boolean {
            if (word.length < MINIMUM_CHECKED_WORD_LENGTH ||
                word.any { it.isDigit() } ||
                word.contains("://") ||
                word.contains(".")
            ) {
                return false
            }

            return word.any { it.isLetter() }
        }

        val splitString = string.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val checkableWords = splitString.filter(::shouldCheck)
        val misspelled =
            dependencies.spellChecker?.misspelledWords(checkableWords, languageCode)
                ?: return true

        var checkedWords = 0
        var misspelledWords = 0
        var validWords = 0

        for (word in checkableWords) {
            checkedWords += 1
            if (word in misspelled) misspelledWords += 1 else validWords += 1

            if (checkedWords >= splitString.size / SAMPLE_DIVISOR) {
                if (validWords >= misspelledWords + DECISION_MARGIN) return true
                if (misspelledWords >= validWords + DECISION_MARGIN) return false
            }

            if (checkedWords >= (splitString.size * SAMPLE_CAP_NUMERATOR / SAMPLE_CAP_DENOMINATOR).toInt()) break
        }

        // If nothing meaningful could be checked, do not penalize.
        if (checkedWords == 0) return true
        return validWords >= misspelledWords
    }

    // MARK: - Companion

    companion object {
        /** The shared recognition service. */
        val shared = LanguageRecognitionService()

        private val identifier by lazy {
            LanguageIdentification.getClient(
                LanguageIdentificationOptions
                    .Builder()
                    .setConfidenceThreshold(0f)
                    .build(),
            )
        }

        private const val UNDETERMINED = "und"
        private const val IDENTIFICATION_LEG = 0.4f
        private const val SPELL_LEG = 0.2f
        private const val HYPOTHESIS_CONFIDENCE_THRESHOLD = 0.45f
        private const val MINIMUM_CHECKED_WORD_LENGTH = 3
        private const val SAMPLE_DIVISOR = 3
        private const val SAMPLE_CAP_NUMERATOR = 3.0
        private const val SAMPLE_CAP_DENOMINATOR = 4.0
        private const val DECISION_MARGIN = 2

        private suspend fun <T> Task<T>.await(): T =
            suspendCancellableCoroutine { continuation ->
                addOnSuccessListener { result -> continuation.resume(result) }
                addOnFailureListener { error -> continuation.resumeWithException(error) }
            }
    }
}

// Checks spelling through the system spell checker service, when one
// is enabled and the app has a current activity to reach it from.
private object SystemSpellChecker : LanguageRecognitionService.SpellChecker {
    override suspend fun misspelledWords(
        words: List<String>,
        languageCode: String,
    ): Set<String>? {
        if (words.isEmpty()) return emptySet()
        val context: Context =
            Translator.config.currentActivityProvider
                ?.invoke()
                ?.applicationContext ?: return null
        val manager = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager ?: return null

        // An unanswered check counts as no checker, so a session whose
        // service never answers cannot hold the translation.
        return withTimeoutOrNull(SPELL_CHECK_TIMEOUT_MILLIS) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    var session: SpellCheckerSession? = null
                    val listener =
                        object : SpellCheckerSession.SpellCheckerSessionListener {
                            override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                                // Only sentence suggestions are requested.
                            }

                            override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
                                session?.close()
                                if (!continuation.isActive) return
                                continuation.resume(misspelledWords(words, results))
                            }
                        }

                    session =
                        manager.newSpellCheckerSession(
                            null,
                            Locale(languageCode),
                            listener,
                            false,
                        )
                    val openSession = session
                    if (openSession == null) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }

                    continuation.invokeOnCancellation { openSession.close() }
                    openSession.getSentenceSuggestions(words.map { TextInfo(it) }.toTypedArray(), 1)
                }
            }
        }
    }

    // Collects the words whose top result looks like a typo.
    private fun misspelledWords(
        words: List<String>,
        results: Array<out SentenceSuggestionsInfo>?,
    ): Set<String> {
        val sentences = results ?: return emptySet()
        return words
            .filterIndexed { index, _ ->
                val sentence = sentences.getOrNull(index) ?: return@filterIndexed false
                (0 until sentence.suggestionsCount).any { suggestionIndex ->
                    val attributes = sentence.getSuggestionsInfoAt(suggestionIndex).suggestionsAttributes
                    attributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0
                }
            }.toSet()
    }

    private const val SPELL_CHECK_TIMEOUT_MILLIS = 2_000L
}
