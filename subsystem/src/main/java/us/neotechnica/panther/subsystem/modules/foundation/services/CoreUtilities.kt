//
//  CoreUtilities.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import android.os.Debug
import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.models.overriddenLanguageCode
import java.io.File
import java.util.Locale
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Cross-cutting utilities shared across the app.
 */
object CoreUtilities {
    // MARK: - Computed Properties

    /**
     * The current memory usage of the app in megabytes, or `null`
     * when it cannot be determined.
     */
    val appMemoryFootprint: Int?
        get() {
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            val totalPssKilobytes = memoryInfo.totalPss
            if (totalPssKilobytes <= 0) return null
            return totalPssKilobytes / KILOBYTES_PER_MEGABYTE
        }

    /**
     * The mapping of supported language codes to language names,
     * localized for the active [RuntimeStorage.languageCode].
     */
    val localizedLanguageCodeDictionary: Map<String, String>?
        get() = localizedLanguageCodeDictionary(RuntimeStorage.languageCode)

    // MARK: - Methods

    /**
     * Clears the given cache domains, or every registered cache
     * domain when none are specified.
     *
     * @param domains The domains to clear, or `null` to clear all
     *   registered domains.
     */
    fun clearCaches(domains: List<CacheDomain>? = null) {
        (domains ?: CacheDomain.allCases).forEach { it.clear() }
    }

    /**
     * Erases every file in the app's documents directory.
     *
     * @throws Exception if the operation fails.
     */
    fun eraseDocumentsDirectory() {
        val directory = FileStore.documentsDirectory ?: return
        eraseDirectory(directory)
    }

    /**
     * Erases every file in the app's temporary directories.
     *
     * @throws Exception if the operation fails.
     */
    fun eraseTemporaryDirectory() {
        FileStore.temporaryDirectories.forEach { eraseDirectory(it) }
    }

    /**
     * Returns the mapping of supported language codes to language
     * names, localized for the given language code.
     *
     * Each entry maps a language code (for example, `"fr"`) to a
     * display name that includes the localized name and, when
     * different, the endonym in parentheses.
     *
     * @param languageCode The language code to localize the display
     *   names for.
     *
     * @return The localized dictionary, or `null` when no language-code
     *   dictionary has been stored in [RuntimeStorage].
     */
    fun localizedLanguageCodeDictionary(languageCode: String): Map<String, String>? {
        val languageCodeDictionary = RuntimeStorage.languageCodeDictionary ?: return null
        val locale = Locale(languageCode)
        return languageCodeDictionary.entries.associate { (code, name) ->
            val localizedName = Locale(code).getDisplayLanguage(locale).takeIf { it.isNotBlank() && it != code }
            if (localizedName == null) {
                code to name.trim()
            } else {
                code to localizedDisplayName(localizedName, name)
            }
        }
    }

    /**
     * Restores the active language code to the device's system
     * language.
     */
    fun restoreDeviceLanguageCode() {
        setLanguageCode(Locale.getDefault().language)
    }

    /**
     * Sets the active language code for translation and localization.
     *
     * The new code is stored in [RuntimeStorage] and is read as the
     * target language for display-string translation.
     *
     * @param languageCode The language code to set (for example,
     *   `"fr"`).
     * @param override Pass `true` to persist the code as an override
     *   that takes precedence over the stored language code. The
     *   default is `false`.
     */
    fun setLanguageCode(
        languageCode: String,
        override: Boolean = false,
    ) {
        RuntimeStorage.store(languageCode, StoredItemKey.languageCode)
        if (!override) return
        RuntimeStorage.store(languageCode, StoredItemKey.overriddenLanguageCode)
    }

    /**
     * Terminates the app after the given delay.
     *
     * @param terminateAfter The delay before the process exits. The
     *   default is one second.
     */
    fun exitGracefully(terminateAfter: Duration = 1.seconds) {
        Task.delayed(by = terminateAfter) { exitProcess(0) }
    }

    // MARK: - Auxiliary

    private fun localizedDisplayName(
        localizedName: String,
        name: String,
    ): String {
        val capitalizedName = localizedName.replaceFirstChar { it.uppercase() }
        val components = name.split("(")
        if (components.size == 2) {
            val endonym = components[1]
            val endonymCore = endonym.dropLast(1)
            val suffix = if (localizedName.lowercase() == endonymCore.lowercase()) "" else "($endonym"
            return "$capitalizedName $suffix".trim()
        }
        val suffix = if (localizedName.lowercase() == name.lowercase()) "" else "($name)"
        return "$capitalizedName $suffix".trim()
    }

    private fun eraseDirectory(directory: File) {
        runCatching {
            directory.listFiles()?.forEach { it.deleteRecursively() }
        }.onFailure { throwable ->
            throw Exception.from(throwable, ExceptionMetadata(this))
        }
    }

    private const val KILOBYTES_PER_MEGABYTE = 1024
}
