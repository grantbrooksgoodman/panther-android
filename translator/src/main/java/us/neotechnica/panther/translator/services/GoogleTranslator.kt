//
//  GoogleTranslator.kt
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
import java.net.URLEncoder

/**
 * Translates using Google Translate.
 *
 * A lightweight JSON endpoint responds far faster than loading and
 * scraping the full web app, so [translate] tries it first and falls
 * back to the [BaseTranslator] web-view harness on any failure.
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
                "API fast path failed, falling back to web view: ${Translator.descriptor(error)}",
                sender = this,
                fileName = "GoogleTranslator.kt",
                function = "translate",
                line = 0,
            )
            super.translate(input, languagePair)
        }

    // MARK: - Extraction Override

    override suspend fun extractOutput(
        webView: android.webkit.WebView,
        useAlternate: Boolean,
    ): String? {
        val output = super.extractOutput(webView, useAlternate) ?: return null
        if (output.contains("(feminine)")) return null
        return output.replace("(masculine)", "")
    }

    // MARK: - API Fast Path

    private suspend fun translateWithApi(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation {
        val source = platform.identifier(languagePair.from) ?: throw TranslationError.FailedToGenerateRequestURL
        val target = platform.identifier(languagePair.to) ?: throw TranslationError.FailedToGenerateRequestURL
        val encoded = URLEncoder.encode(input.value, "UTF-8").replace("+", "%20")

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

        if (output.lowercasedTrimmingWhitespaceAndNewlines().isEmpty()) {
            throw TranslationError.MalformedTranslationResult
        }

        return Translation(input = input, output = output, languagePair = languagePair)
    }

    // MARK: - Companion

    companion object {
        private const val API_BASE_URL = "https://translate.googleapis.com/translate_a/single"
    }
}
