//
//  TranslationPlatform.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.models

import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.interfaces.Translatorable
import us.neotechnica.panther.translator.services.DeepLTranslator
import us.neotechnica.panther.translator.services.GoogleTranslator
import us.neotechnica.panther.translator.services.LaraTranslator
import us.neotechnica.panther.translator.services.ReversoTranslator
import java.net.URLEncoder

/**
 * A web translation provider.
 *
 * Each platform describes how to build a request URL, which language
 * codes it supports, the JavaScript used to extract a result from its
 * web app, and – where available – a result-observer script that
 * surfaces the result as soon as it renders.
 */
enum class TranslationPlatform {
    DEEP_L,
    GOOGLE,
    LARA,
    REVERSO,
    ;

    // MARK: - Computed Properties

    internal val alternateJavaScriptString: String
        get() =
            when (this) {
                DEEP_L -> Translator.Constants.DEEP_L_ALTERNATE_JAVA_SCRIPT_STRING
                GOOGLE -> Translator.Constants.GOOGLE_ALTERNATE_JAVA_SCRIPT_STRING
                LARA -> Translator.Constants.LARA_JAVA_SCRIPT_STRING
                REVERSO -> Translator.Constants.REVERSO_ALTERNATE_JAVA_SCRIPT_STRING
            }

    internal val instance: Translatorable
        get() =
            when (this) {
                DEEP_L -> DeepLTranslator()
                GOOGLE -> GoogleTranslator()
                LARA -> LaraTranslator()
                REVERSO -> ReversoTranslator()
            }

    internal val javaScriptString: String
        get() =
            when (this) {
                DEEP_L -> Translator.Constants.DEEP_L_JAVA_SCRIPT_STRING
                GOOGLE -> Translator.Constants.GOOGLE_JAVA_SCRIPT_STRING
                LARA -> Translator.Constants.LARA_JAVA_SCRIPT_STRING
                REVERSO -> Translator.Constants.REVERSO_JAVA_SCRIPT_STRING
            }

    internal val prewarmURL: String
        get() =
            when (this) {
                DEEP_L -> "https://www.deepl.com/en/translator"
                GOOGLE -> "https://translate.google.com/?hl=en"
                LARA -> "https://laratranslate.com/translate"
                REVERSO -> "https://www.reverso.net/text-translation"
            }

    internal val resultObserverScript: String?
        get() {
            val handlerName = Translator.Constants.RESULT_OBSERVER_MESSAGE_HANDLER_NAME
            if (this == LARA) {
                return """
                    (function() {
                      window.addEventListener('message', function(event) {
                        try {
                          var data = JSON.parse(event.data);
                          if (data.type === 'laraTranslation' && data.text) {
                            window.__translatorResult = data.text;
                            try { window.$handlerName.postMessage(''); } catch (e) {}
                          }
                        } catch (e) {}
                      });
                    })();
                    """.trimIndent()
            }

            val readyCheck =
                when (this) {
                    DEEP_L ->
                        """
                        var results = document.querySelectorAll('[aria-labelledby="translation-results-heading"]');
                        var element = results[results.length - 1];
                        if (element && element.innerText && element.innerText.trim()) { return true; }
                        results = document.querySelectorAll('[aria-labelledby="translation-target-heading"]');
                        element = results[results.length - 1];
                        return !!(element && element.innerText && element.innerText.trim());
                        """.trimIndent()

                    GOOGLE ->
                        """
                        var element = document.getElementsByClassName('lRu31')[0];
                        return !!(element && element.innerText && element.innerText.trim());
                        """.trimIndent()

                    REVERSO ->
                        """
                        var element = document.getElementsByClassName('textarea translation-box__translated-text translation-box__translated-text_favorite')[0] ||
                            document.getElementsByClassName('translation-input__main translation-input__result')[0];
                        return !!(element && element.innerText && element.innerText.trim() && element.innerText.trim() !== '!');
                        """.trimIndent()

                    LARA -> ""
                }

            return """
                (function() {
                  function isReady() {
                    try {
                      $readyCheck
                    } catch (e) { return false; }
                  }
                  function notify() {
                    try { window.$handlerName.postMessage(''); } catch (e) {}
                  }
                  function observe() {
                    if (isReady()) { return notify(); }
                    var observer = new MutationObserver(function() {
                      if (isReady()) {
                        observer.disconnect();
                        notify();
                      }
                    });
                    observer.observe(document.documentElement, { childList: true, subtree: true, characterData: true });
                    setTimeout(function() { observer.disconnect(); }, 15000);
                  }
                  if (document.documentElement) { observe(); }
                  else { document.addEventListener('DOMContentLoaded', observe); }
                })();
                """.trimIndent()
        }

    // MARK: - Methods

    internal fun identifier(languageCode: String): String? {
        val normalized = languageCode.lowercase().trim()
        return when (this) {
            DEEP_L -> {
                val supported =
                    setOf(
                        "bg",
                        "cs",
                        "da",
                        "de",
                        "el",
                        "en",
                        "es",
                        "et",
                        "fi",
                        "fr",
                        "hu",
                        "id",
                        "it",
                        "ja",
                        "lt",
                        "lv",
                        "nl",
                        "pl",
                        "pt",
                        "ro",
                        "ru",
                        "sk",
                        "sl",
                        "sv",
                        "tr",
                        "zh",
                    )
                if (normalized in supported) normalized else null
            }

            GOOGLE ->
                when (normalized) {
                    "he" -> "iw"
                    "zh" -> "zh-CN"
                    else -> normalized
                }

            LARA -> normalized

            REVERSO -> REVERSO_LANGUAGE_CODES[normalized]
        }
    }

    internal fun requestURL(
        text: String,
        languagePair: LanguagePair,
    ): String? {
        val source = identifier(languagePair.from) ?: return null
        val target = identifier(languagePair.to) ?: return null
        val encoded = urlQueryEncoded(text)

        return when (this) {
            DEEP_L -> "https://www.deepl.com/en/translator#$source/$target/$encoded"
            GOOGLE -> "https://translate.google.com/?hl=en&sl=$source&tl=$target&text=$encoded&op=translate"
            LARA -> "https://laratranslate.com/translate?source=$source&text=$encoded&target=$target"
            REVERSO -> "https://www.reverso.net/text-translation#sl=$source&tl=$target&text=$encoded"
        }
    }

    // MARK: - Companion

    companion object {
        /** Every platform, in declaration order. */
        val allCases: List<TranslationPlatform> get() = entries

        private val REVERSO_LANGUAGE_CODES =
            mapOf(
                "ar" to "ara",
                "cz" to "cze",
                "da" to "dan",
                "de" to "ger",
                "el" to "gre",
                "en" to "eng",
                "es" to "spa",
                "fa" to "per",
                "fr" to "fra",
                "he" to "heb",
                "hi" to "hin",
                "hu" to "hun",
                "it" to "ita",
                "ja" to "jpn",
                "ko" to "kor",
                "nl" to "dut",
                "pl" to "pol",
                "pt" to "por",
                "ro" to "rum",
                "ru" to "rus",
                "sk" to "slo",
                "sv" to "swe",
                "th" to "tha",
                "tr" to "tur",
                "uk" to "ukr",
                "zh" to "chi",
            )

        /**
         * The characters allowed unescaped in a URL query component:
         * unreserved characters plus `!$&'()*+,-./:;=?@_~`.
         */
        internal const val URL_QUERY_ALLOWED_CHARACTERS = "!$&'()*+,-./:;=?@_~"

        /**
         * Percent-encodes [text] for a URL query, escaping every byte
         * outside the alphanumerics and [allowedCharacters].
         */
        internal fun percentEncoded(
            text: String,
            allowedCharacters: String = URL_QUERY_ALLOWED_CHARACTERS,
        ): String =
            buildString {
                for (byte in text.toByteArray(Charsets.UTF_8)) {
                    val character = byte.toInt().toChar()
                    if (byte >= 0 && (character.isLetterOrDigit() || character in allowedCharacters)) {
                        append(character)
                    } else {
                        append('%')
                        append(HEX_DIGITS[(byte.toInt() shr HEX_SHIFT) and HEX_MASK])
                        append(HEX_DIGITS[byte.toInt() and HEX_MASK])
                    }
                }
            }

        private fun urlQueryEncoded(text: String): String = percentEncoded(text)

        private const val HEX_DIGITS = "0123456789ABCDEF"
        private const val HEX_MASK = 0x0F
        private const val HEX_SHIFT = 4
    }
}
