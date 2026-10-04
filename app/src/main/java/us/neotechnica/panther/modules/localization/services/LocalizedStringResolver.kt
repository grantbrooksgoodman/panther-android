//
//  LocalizedStringResolver.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.localization.services

import android.content.Context
import org.json.JSONObject
import us.neotechnica.panther.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.interfaces.LocalizedStringKeyRepresentable
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

    private val tables = ConcurrentHashMap<LocalizationSource, Map<String, Map<String, String>>>()

    @Volatile
    private var appContext: Context? = null

    // MARK: - Initialization

    /**
     * Prepares the resolver for use.
     *
     * @param context Any context; its application context is retained.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
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

        // A key filed under a different table than the one requested – for
        // example, a subsystem string ("cancel", "done") resolved with the
        // default app source – still resolves here from the other table
        // instead of showing the MISSING placeholder.
        for (other in LocalizationSource.entries) {
            if (other == source) continue
            resolve(key, other, language)?.let { return it }
        }

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
