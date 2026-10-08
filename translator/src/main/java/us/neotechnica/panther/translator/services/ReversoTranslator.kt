//
//  ReversoTranslator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import android.webkit.WebView
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.extensions.lowercasedTrimmingWhitespaceAndNewlines
import us.neotechnica.panther.translator.extensions.trimmingBorderedNewlines
import us.neotechnica.panther.translator.extensions.trimmingTrailingNewlines
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationError
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.models.TranslationPlatform

/**
 * Translates using Reverso.
 *
 * Like [GoogleTranslator], [translate] tries Reverso's JSON API first
 * and falls back to the [BaseTranslator] web-view harness on failure.
 */
internal class ReversoTranslator : BaseTranslator(TranslationPlatform.REVERSO) {
    // MARK: - Translate

    override suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation =
        try {
            translateWithApi(input, languagePair)
        } catch (error: Exception) {
            Translator.config.loggerDelegate?.log(
                "API fast path failed, falling back to web view: ${Translator.descriptor(error)}",
                sender = this,
                fileName = "ReversoTranslator.kt",
                function = "translate",
                line = 0,
            )
            super.translate(input, languagePair)
        }

    // MARK: - Evaluate JavaScript

    override suspend fun evaluateJavaScript(
        webView: WebView,
        useAlternateString: Boolean,
    ): EvaluationResult {
        if (restoreLanguagePairIfNeeded(webView)) {
            return EvaluationResult.Retry(useAlternateString = !useAlternateString)
        }

        val evaluationResult = super.evaluateJavaScript(webView, useAlternateString)
        if (evaluationResult !is EvaluationResult.Success ||
            evaluationResult.output.lowercasedTrimmingWhitespaceAndNewlines == "!"
        ) {
            return EvaluationResult.Retry(useAlternateString = !useAlternateString)
        }

        val translationInput = translationInput ?: return EvaluationResult.Retry(useAlternateString = !useAlternateString)
        val inputNewlineCount = translationInput.value.split("\n").size
        val outputComponents = evaluationResult.output.trimmingBorderedNewlines.split("\n")

        if (outputComponents.size < inputNewlineCount || outputComponents.isEmpty()) {
            return EvaluationResult.Retry(useAlternateString = !useAlternateString)
        }

        val processedOutput =
            process(
                outputComponents
                    .take(maxOf(inputNewlineCount, 1))
                    .joinToString("\n")
                    .trimmingTrailingNewlines,
            )

        return EvaluationResult.Success(processedOutput)
    }

    // MARK: - API Fast Path

    private suspend fun translateWithApi(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation {
        val source = platform.identifier(languagePair.from) ?: throw TranslationError.FailedToGenerateRequestURL
        val target = platform.identifier(languagePair.to) ?: throw TranslationError.FailedToGenerateRequestURL

        val requestBody =
            JSONObject()
                .apply {
                    put("format", "text")
                    put("from", source)
                    put("to", target)
                    put("input", input.value)
                    put(
                        "options",
                        JSONObject().apply {
                            put("contextResults", false)
                            put("languageDetection", false)
                            put("origin", "translation.web")
                            put("sentenceSplitter", false)
                        },
                    )
                }.toString()

        val response =
            NetworkClient.postJson(
                urlString = API_URL,
                body = requestBody,
                headers =
                    mapOf(
                        "Content-Type" to "application/json",
                        "Accept" to "application/json",
                        "User-Agent" to USER_AGENT,
                    ),
            )

        val translations =
            JSONObject(response).optJSONArray("translation")
                ?: throw TranslationError.MalformedTranslationResult

        val output = joinedStrings(translations)
        if (output.lowercasedTrimmingWhitespaceAndNewlines.isEmpty()) {
            throw TranslationError.MalformedTranslationResult
        }

        return Translation(input = input, output = output, languagePair = languagePair)
    }

    // MARK: - Auxiliary

    private fun process(string: String): String {
        val seeMore = "See more translations"
        if (!string.contains(seeMore)) return string
        return string.split(seeMore)[0].trimmingBorderedNewlines
    }

    // Restores the page's original language pair when Reverso has
    // swapped it, returning whether a restoration occurred.
    private suspend fun restoreLanguagePairIfNeeded(webView: WebView): Boolean {
        val result =
            webView.evaluateJavascriptAwait(
                """
                (function() {
                  try {
                    document.getElementsByClassName('original-language-pair-link')[0].click();
                    return 'clicked';
                  } catch (e) {
                    return null;
                  }
                })();
                """.trimIndent(),
            )

        if (result != "clicked") return false
        delay(RESTORE_SETTLE_DELAY_MILLIS)
        return true
    }

    private fun joinedStrings(array: JSONArray): String =
        buildString {
            for (index in 0 until array.length()) {
                val piece = array.opt(index)
                if (piece is String) append(piece)
            }
        }

    // MARK: - Companion

    companion object {
        internal const val API_WARMUP_URL_STRING = "https://api.reverso.net"

        private const val API_URL = "https://api.reverso.net/translate/v1/translation"
        private const val RESTORE_SETTLE_DELAY_MILLIS = 500L
        private const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"
    }
}
