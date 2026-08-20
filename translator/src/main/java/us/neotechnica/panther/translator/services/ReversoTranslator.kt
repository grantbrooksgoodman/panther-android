//
//  ReversoTranslator.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import org.json.JSONArray
import org.json.JSONObject
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.extensions.lowercasedTrimmingWhitespaceAndNewlines
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
        if (output.lowercasedTrimmingWhitespaceAndNewlines().isEmpty()) {
            throw TranslationError.MalformedTranslationResult
        }

        return Translation(input = input, output = output, languagePair = languagePair)
    }

    // MARK: - Auxiliary

    private fun joinedStrings(array: JSONArray): String =
        buildString {
            for (index in 0 until array.length()) {
                val piece = array.opt(index)
                if (piece is String) append(piece)
            }
        }

    // MARK: - Companion

    companion object {
        private const val API_URL = "https://api.reverso.net/translate/v1/translation"
        private const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"
    }
}
