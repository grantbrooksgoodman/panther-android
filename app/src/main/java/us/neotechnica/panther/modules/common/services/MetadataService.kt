//
//  MetadataService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import us.neotechnica.panther.bundle.metadataService
import us.neotechnica.panther.bundle.shared
import us.neotechnica.panther.modules.common.extensions.MetadataServiceStorageKey
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.getValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.SingleSlotCoalescer
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import java.net.URI

/**
 * Reads app configuration values hosted in the remote database.
 *
 * Each value is persisted across launches and served immediately
 * through its corresponding property. Call [resolveValues] to
 * revalidate the persisted snapshot against the network, so callers
 * converge on authoritative data without blocking on the fetch.
 */
object MetadataService {
    // MARK: - Types

    private enum class MetadataServiceKey(
        val rawValue: String,
    ) {
        APP_SHARE_LINK("appShareLink"),
        APP_STORE_BUILD_NUMBER("appStoreBuildNumber"),
        GEMINI_API_KEY("geminiApiKey"),
        IS_PREVARICATION_MODE_ENABLED("isPrevaricationModeEnabled"),
        PLAY_STORE_BUILD_NUMBER("playStoreBuildNumber"),
        PLAY_STORE_SHARE_LINK("playStoreShareLink"),
        REDIRECTION_KEY("redirectionKey"),
        SHOULD_FORCE_UPDATE("shouldForceUpdate"),
        SHOULD_FORCE_UPDATE_ANDROID("shouldForceUpdateAndroid"),
        STORAGE_REFERENCE_URL("storageReferenceURL"),
        ;

        // MARK: - Computed Properties

        val path: String
            get() = "${NetworkPath.shared.rawValue}/$rawValue"
    }

    // MARK: - Properties

    private val coalescer = SingleSlotCoalescer<Unit>()

    // MARK: - Computed Properties

    /** The app's share link, or `null` if it has not been resolved. */
    val appShareLink: String?
        get() = Persistent.string(scopedKey(MetadataServiceStorageKey.APP_SHARE_LINK))

    /** The App Store build number, or `null` if it has not been resolved. */
    val appStoreBuildNumber: Int?
        get() = Persistent.int(scopedKey(MetadataServiceStorageKey.APP_STORE_BUILD_NUMBER))

    /** The Gemini API key, or an empty string if it has not been resolved. */
    val apiKey: String
        get() = geminiAPIKey ?: ""

    /** The Gemini API key, or `null` if it has not been resolved. */
    val geminiAPIKey: String?
        get() = Persistent.string(scopedKey(MetadataServiceStorageKey.GEMINI_API_KEY))

    /**
     * A Boolean value that indicates whether prevarication mode may
     * be enabled, or `null` if it has not been resolved.
     */
    val isPrevaricationModeEnabled: Boolean?
        get() = Persistent.booleanOrNull(scopedKey(MetadataServiceStorageKey.IS_PREVARICATION_MODE_ENABLED))

    /** The Play Store build number, or `null` if it has not been resolved. */
    val playStoreBuildNumber: Int?
        get() = Persistent.int(scopedKey(MetadataServiceStorageKey.PLAY_STORE_BUILD_NUMBER))

    /** The Play Store share link, or `null` if it has not been resolved. */
    val playStoreShareLink: String?
        get() = Persistent.string(scopedKey(MetadataServiceStorageKey.PLAY_STORE_SHARE_LINK))

    /** The hosted redirection key, or `null` if it has not been resolved. */
    val redirectionKey: String?
        get() = Persistent.string(scopedKey(MetadataServiceStorageKey.REDIRECTION_KEY))

    /**
     * A Boolean value that indicates whether the app should force an
     * update, or `null` if it has not been resolved.
     */
    val shouldForceUpdate: Boolean?
        get() = Persistent.booleanOrNull(scopedKey(MetadataServiceStorageKey.SHOULD_FORCE_UPDATE))

    /**
     * A Boolean value that indicates whether the Android app should
     * force an update, or `null` if it has not been resolved.
     */
    val shouldForceUpdateAndroid: Boolean?
        get() = Persistent.booleanOrNull(scopedKey(MetadataServiceStorageKey.SHOULD_FORCE_UPDATE_ANDROID))

    /**
     * The base URL for browsing remote storage, or `null` if it has
     * not been resolved.
     */
    val storageReferenceURL: String?
        get() = Persistent.string(scopedKey(MetadataServiceStorageKey.STORAGE_REFERENCE_URL))

    private val canRevalidate: Boolean
        get() =
            appShareLink == null ||
                appStoreBuildNumber == null ||
                geminiAPIKey == null ||
                isPrevaricationModeEnabled == null ||
                playStoreBuildNumber == null ||
                playStoreShareLink == null ||
                redirectionKey == null ||
                shouldForceUpdate == null ||
                shouldForceUpdateAndroid == null ||
                storageReferenceURL == null

    // MARK: - Resolve All Values

    /**
     * Revalidates the hosted values against the network, overwriting
     * the persisted snapshot.
     *
     * The persisted values are served immediately on launch.
     * Concurrent calls coalesce onto a single in-flight refresh, and
     * subsequent calls within the session return immediately.
     *
     * @throws Exception If fetching fails, or if a hosted value is
     *   missing or of an unexpected type.
     */
    suspend fun resolveValues() {
        if (!canRevalidate) return
        coalescer {
            if (!canRevalidate) return@coalescer
            assignValues(
                Networking.config.databaseDelegate.getValues<Map<String, Any>>(
                    NetworkPath.shared.rawValue,
                    prependingEnvironment = false,
                    cacheStrategy = CacheStrategy.RETURN_CACHE_ON_FAILURE,
                ),
            )
        }
    }

    // MARK: - Auxiliary

    @Suppress("CyclomaticComplexMethod")
    private fun assignValues(dictionary: Map<String, Any>) {
        val appShareLink =
            (dictionary[MetadataServiceKey.APP_SHARE_LINK.rawValue] as? String)?.takeIf { it.isValidURL() }
                ?: throw Exception.Networking.typecastFailed("URL", metadata = ExceptionMetadata(this))

        val storageReferenceURL =
            (dictionary[MetadataServiceKey.STORAGE_REFERENCE_URL.rawValue] as? String)?.takeIf { it.isValidURL() }
                ?: throw Exception.Networking.typecastFailed("URL", metadata = ExceptionMetadata(this))

        val playStoreShareLink =
            (dictionary[MetadataServiceKey.PLAY_STORE_SHARE_LINK.rawValue] as? String)?.takeIf { it.isValidURL() }
                ?: throw Exception.Networking.typecastFailed("URL", metadata = ExceptionMetadata(this))

        val appStoreBuildNumber =
            dictionary[MetadataServiceKey.APP_STORE_BUILD_NUMBER.rawValue].intValue
                ?: throw Exception.Networking.typecastFailed("integer", metadata = ExceptionMetadata(this))

        val playStoreBuildNumber =
            dictionary[MetadataServiceKey.PLAY_STORE_BUILD_NUMBER.rawValue].intValue
                ?: throw Exception.Networking.typecastFailed("integer", metadata = ExceptionMetadata(this))

        val geminiAPIKey =
            dictionary[MetadataServiceKey.GEMINI_API_KEY.rawValue] as? String
                ?: throw Exception.Networking.typecastFailed("string", metadata = ExceptionMetadata(this))

        val redirectionKey =
            dictionary[MetadataServiceKey.REDIRECTION_KEY.rawValue] as? String
                ?: throw Exception.Networking.typecastFailed("string", metadata = ExceptionMetadata(this))

        val isPrevaricationModeEnabled =
            dictionary[MetadataServiceKey.IS_PREVARICATION_MODE_ENABLED.rawValue] as? Boolean
                ?: throw Exception.Networking.typecastFailed("Bool", metadata = ExceptionMetadata(this))

        val shouldForceUpdate =
            dictionary[MetadataServiceKey.SHOULD_FORCE_UPDATE.rawValue] as? Boolean
                ?: throw Exception.Networking.typecastFailed("Bool", metadata = ExceptionMetadata(this))

        val shouldForceUpdateAndroid =
            dictionary[MetadataServiceKey.SHOULD_FORCE_UPDATE_ANDROID.rawValue] as? Boolean
                ?: throw Exception.Networking.typecastFailed("Bool", metadata = ExceptionMetadata(this))

        Persistent.setString(scopedKey(MetadataServiceStorageKey.APP_SHARE_LINK), appShareLink)
        Persistent.setInt(scopedKey(MetadataServiceStorageKey.APP_STORE_BUILD_NUMBER), appStoreBuildNumber)
        Persistent.setString(scopedKey(MetadataServiceStorageKey.GEMINI_API_KEY), geminiAPIKey)
        Persistent.setBoolean(scopedKey(MetadataServiceStorageKey.IS_PREVARICATION_MODE_ENABLED), isPrevaricationModeEnabled)
        Persistent.setInt(scopedKey(MetadataServiceStorageKey.PLAY_STORE_BUILD_NUMBER), playStoreBuildNumber)
        Persistent.setString(scopedKey(MetadataServiceStorageKey.PLAY_STORE_SHARE_LINK), playStoreShareLink)
        Persistent.setString(scopedKey(MetadataServiceStorageKey.REDIRECTION_KEY), redirectionKey)
        Persistent.setBoolean(scopedKey(MetadataServiceStorageKey.SHOULD_FORCE_UPDATE), shouldForceUpdate)
        Persistent.setBoolean(scopedKey(MetadataServiceStorageKey.SHOULD_FORCE_UPDATE_ANDROID), shouldForceUpdateAndroid)
        Persistent.setString(scopedKey(MetadataServiceStorageKey.STORAGE_REFERENCE_URL), storageReferenceURL)
    }

    // An integral hosted number; fractional values do not convert.
    private val Any?.intValue: Int?
        get() =
            when (this) {
                is Int -> this
                is Long -> toInt()
                else -> null
            }

    private fun String.isValidURL(): Boolean = runCatching { URI(this).scheme }.getOrNull() != null

    private fun scopedKey(key: MetadataServiceStorageKey): PersistentStorageKey = PersistentStorageKey.metadataService(key)
}
