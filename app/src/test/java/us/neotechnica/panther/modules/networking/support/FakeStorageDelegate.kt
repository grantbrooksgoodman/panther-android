//
//  FakeStorageDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.support

import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate
import us.neotechnica.panther.networking.modules.storage.models.DirectoryListing
import us.neotechnica.panther.networking.modules.storage.models.HostedItemMetadata
import us.neotechnica.panther.networking.modules.storage.models.HostedItemType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import java.io.File
import kotlin.time.Duration

/**
 * A recording [StorageDelegate] for tests.
 *
 * Captures every upload and serves downloads from a seeded table of
 * hosted items, keyed by storage path.
 */
class FakeStorageDelegate : StorageDelegate {
    // MARK: - Recorded Operations

    /** The storage paths uploaded so far, with their content types. */
    val uploadedPaths = mutableListOf<Pair<String, String?>>()

    /** The storage paths deleted so far. */
    val deletedPaths = mutableListOf<String>()

    /** The hosted items served by [downloadItem], keyed by storage path. */
    val hostedItems = mutableMapOf<String, ByteArray>()

    // MARK: - StorageDelegate

    override fun clearStore() = Unit

    override suspend fun deleteAllItems(
        path: String,
        includeItemsInSubdirectories: Boolean,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) = Unit

    override suspend fun deleteItem(
        path: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        deletedPaths.add(path)
    }

    override suspend fun downloadAllItems(
        path: String,
        toDirectory: File,
        includeItemsInSubdirectories: Boolean,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ) = Unit

    override suspend fun downloadItem(
        path: String,
        toLocalPath: File,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ) {
        val bytes =
            hostedItems[path]
                ?: throw Exception(
                    "No item exists at the specified key path.",
                    metadata = ExceptionMetadata(this),
                )

        toLocalPath.parentFile?.mkdirs()
        toLocalPath.writeBytes(bytes)
    }

    override suspend fun enumerateEmptyDirectories(
        path: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): Set<String> = emptySet()

    override suspend fun getDirectoryListing(
        path: String,
        firstResultOnly: Boolean,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): DirectoryListing = DirectoryListing(filePaths = emptySet(), subdirectories = emptySet())

    override suspend fun itemExists(
        itemType: HostedItemType,
        path: String,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ): Boolean = hostedItems.containsKey(path)

    override fun prewarm() = Unit

    override fun setGlobalCacheStrategy(globalCacheStrategy: CacheStrategy?) = Unit

    override suspend fun upload(
        data: ByteArray,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        uploadedPaths.add(metadata.filePath to metadata.contentType)
        hostedItems[metadata.filePath] = data
    }

    override suspend fun upload(
        file: File,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        uploadedPaths.add(metadata.filePath to metadata.contentType)
        hostedItems[metadata.filePath] = file.readBytes()
    }
}
