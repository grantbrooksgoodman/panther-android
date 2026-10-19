//
//  LocalizedStringResolver.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.localization.services

import android.content.Context
import org.json.JSONObject
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.interfaces.LocalizedStringKeyRepresentable
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves localized strings from the bundled JSON tables.
 *
 * [initialize] must be called once, with the application context,
 * before any string is resolved. Strings are looked up for
 * [languageCode] – the language the user has selected, defaulting to
 * the device language – falling back to English and then to the
 * [MISSING] placeholder.
 */
object LocalizedStringResolver {
    // MARK: - Constants

    /** The placeholder returned when a key resolves in no language. */
    const val MISSING = "�"

    /** The language always used as the final fallback. */
    private const val FALLBACK_LANGUAGE_CODE = "en"

    /** The subsystem table key whose value maps language codes to display names. */
    private const val LANGUAGE_CODES_KEY = "language_codes"

    // MARK: - Properties

    /**
     * The language strings are resolved for.
     *
     * Delegates to [RuntimeStorage.languageCode] – the single source of
     * truth for the user's selected language, shared with the on-the-fly
     * translation path – so pre-localized strings resolve in the same
     * language as translated ones. It defaults to the device language
     * until the user's stored preference is applied at launch.
     */
    val languageCode: String
        get() = RuntimeStorage.languageCode

    internal val supportedLanguageCodes: Set<String>
        get() = languageDisplayNames().keys - UNSUPPORTED_LANGUAGE_CODES

    private val tables = ConcurrentHashMap<LocalizationSource, Map<String, Map<String, String>>>()

    private val UNSUPPORTED_LANGUAGE_CODES = setOf("ba", "ceb", "jv", "la", "mr", "ms", "udm")

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var testTableProvider: ((LocalizationSource) -> Map<String, Map<String, String>>)? = null

    // MARK: - Initialization

    /**
     * Prepares the resolver for use.
     *
     * @param context Any context; its application context is retained.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Stores the supported languages and validates the active
     * language code.
     *
     * Reads the subsystem table's `language_codes` entry, omitting the
     * languages the app does not support, and stores the result in
     * [RuntimeStorage.languageCodeDictionary]. When the active
     * language code is not among them, the active language reverts to
     * English and the event is logged.
     *
     * Call this method once at launch, after [initialize] and after
     * the active language code has been set.
     */
    fun initialize() {
        val supportedLanguages =
            languageDisplayNames().filterKeys { it !in UNSUPPORTED_LANGUAGE_CODES }
        RuntimeStorage.languageCodeDictionary = supportedLanguages

        if (RuntimeStorage.languageCodeDictionary?.get(RuntimeStorage.languageCode) == null ||
            supportedLanguages.isEmpty()
        ) {
            Logger.log(
                Exception(
                    "Unsupported language code; reverting to English.",
                    userInfo = mapOf("LanguageCode" to RuntimeStorage.languageCode),
                    metadata = ExceptionMetadata(this),
                ),
                domain = LoggerDomain.localization,
            )

            CoreUtilities.setLanguageCode(FALLBACK_LANGUAGE_CODE)
        }
    }

    /**
     * Prepares the resolver for tests, loading each table from the
     * given provider instead of the application's assets.
     *
     * @param tableProvider A closure returning the `key → language →
     *   value` table for a localization source.
     */
    fun initializeForTesting(tableProvider: (LocalizationSource) -> Map<String, Map<String, String>>) {
        testTableProvider = tableProvider
        tables.clear()
    }

    /** Removes every loaded localization table, forcing a reload on next access. */
    fun clearCache() {
        tables.clear()
    }

    // MARK: - Resolution

    /**
     * Resolves the string for [key] in [source].
     *
     * @param key The key to resolve.
     * @param source The table to resolve from.
     * @param language The language to resolve for; defaults to
     *   [languageCode].
     * @return The localized value, the English value, or [MISSING].
     */
    fun string(
        key: LocalizedStringKeyRepresentable,
        source: LocalizationSource = LocalizationSource.APP,
        language: String = languageCode,
    ): String {
        resolve(key, source, language)?.let { return it }

        return MISSING
    }

    private fun resolve(
        key: LocalizedStringKeyRepresentable,
        source: LocalizationSource,
        language: String,
    ): String? {
        val translations = table(source)[key.referent] ?: return null
        return translations[language] ?: translations[FALLBACK_LANGUAGE_CODE]
    }

    // MARK: - Language Names

    /**
     * The supported languages as a map of ISO 639-1 code to display
     * name (e.g. `"af" -> "Afrikaans (Afrikaans)"`), from the
     * subsystem table's `language_codes` entry.
     */
    fun languageDisplayNames(): Map<String, String> = table(LocalizationSource.SUBSYSTEM)[LANGUAGE_CODES_KEY] ?: emptyMap()

    // MARK: - Tables

    private fun table(source: LocalizationSource): Map<String, Map<String, String>> = tables.getOrPut(source) { load(source) }

    private fun load(source: LocalizationSource): Map<String, Map<String, String>> {
        testTableProvider?.let { return it(source) }

        val context = appContext ?: return emptyMap()
        val json =
            context.assets
                .open(source.assetName)
                .bufferedReader()
                .use { it.readText() }

        val root = JSONObject(json)
        val table = HashMap<String, Map<String, String>>(root.length())
        for (key in root.keys()) {
            val languages = root.optJSONObject(key) ?: continue
            val translations = HashMap<String, String>(languages.length())
            for (language in languages.keys()) {
                translations[language] = languages.getString(language)
            }
            table[key] = translations
        }
        return table
    }
}
