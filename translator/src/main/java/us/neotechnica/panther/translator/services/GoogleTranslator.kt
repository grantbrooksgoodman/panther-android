//
//  GoogleTranslator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import org.json.JSONArray
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.extensions.lowercasedTrimmingWhitespaceAndNewlines
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationError
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.models.TranslationPlatform

/**
 * Translates using Google Translate.
 *
 * A lightweight JSON endpoint responds far faster than loading
 * and scraping the full web app, so [translate] uses it alone.
 * The [BaseTranslator] web-view fallback is disabled: when the
 * endpoint fails, the error propagates and the next platform is
 * tried.
 */
internal class GoogleTranslator : BaseTranslator(TranslationPlatform.GOOGLE) {
    // MARK: - Translate

    override suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation =
        try {
            translateWithApi(input, languagePair)
        } catch (error: Exception) {
            Translator.config.loggerDelegate?.log(
                "API fast path failed; skipping the web view fallback: ${Translator.descriptor(error)}",
                sender = this,
                fileName = "GoogleTranslator.kt",
                function = "translate",
                line = 0,
            )

            // TODO: Restore the web-view fallback, `super.translate(input, languagePair)`, once Google's
            //  result can be extracted within the harness's evaluation threshold.
            throw error
        }

    // MARK: - Evaluate JavaScript

    override suspend fun evaluateJavaScript(
        webView: android.webkit.WebView,
        useAlternateString: Boolean,
    ): EvaluationResult {
        val evaluationResult = super.evaluateJavaScript(webView, useAlternateString)
        if (evaluationResult !is EvaluationResult.Success ||
            evaluationResult.output.contains("(feminine)")
        ) {
            return EvaluationResult.Retry(useAlternateString = true)
        }

        return EvaluationResult.Success(evaluationResult.output.replace("(masculine)", ""))
    }

    // MARK: - API Fast Path

    private suspend fun translateWithApi(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation {
        val source = platform.identifier(languagePair.from) ?: throw TranslationError.FailedToGenerateRequestURL
        val target = platform.identifier(languagePair.to) ?: throw TranslationError.FailedToGenerateRequestURL
        // The query is percent-encoded by hand: the endpoint decodes a
        // bare "+" as a space.
        val encoded =
            TranslationPlatform.percentEncoded(
                input.value,
                allowedCharacters = TranslationPlatform.URL_QUERY_ALLOWED_CHARACTERS.filterNot { it in "+&=?" },
            )

        val url = "$API_BASE_URL?client=gtx&dt=t&sl=$source&tl=$target&q=$encoded"
        val response = NetworkClient.get(url)

        val segments =
            JSONArray(response).optJSONArray(0)
                ?: throw TranslationError.MalformedTranslationResult

        val output =
            buildString {
                for (index in 0 until segments.length()) {
                    val piece = segments.optJSONArray(index)?.opt(0)
                    if (piece is String) append(piece)
                }
            }

        if (output.lowercasedTrimmingWhitespaceAndNewlines.isEmpty()) {
            throw TranslationError.MalformedTranslationResult
        }

        return Translation(input = input, output = output, languagePair = languagePair)
    }

    // MARK: - Companion

    companion object {
        private const val API_BASE_URL = "https://translate.googleapis.com/translate_a/single"
        internal const val API_WARMUP_URL_STRING = "$API_BASE_URL?client=gtx&dt=t&sl=en&tl=es&q=a"
    }
}
