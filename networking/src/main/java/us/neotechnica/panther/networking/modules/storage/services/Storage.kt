//
//  Storage.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.services

import kotlinx.coroutines.flow.Flow
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate
import us.neotechnica.panther.networking.modules.storage.models.DirectoryListing
import us.neotechnica.panther.networking.modules.storage.models.HostedItemMetadata
import us.neotechnica.panther.networking.modules.storage.models.HostedItemType
import us.neotechnica.panther.networking.modules.storage.models.StorageOperation
import us.neotechnica.panther.networking.modules.storage.models.StorageTransferProgress
import java.io.File
import kotlin.time.Duration

/**
 * The Firebase Storage implementation of [StorageDelegate].
 *
 * Operations are coalesced by content so that identical
 * concurrent operations issue a single network request, and
 * download and existence results are cached per path with a
 * short time-to-live.
 */
class Storage : StorageDelegate {
    // MARK: - Properties

    private val coreStorage = CoreStorage()

    // MARK: - Global Cache Strategy

    override fun setGlobalCacheStrategy(globalCacheStrategy: CacheStrategy?) {
        coreStorage.setGlobalCacheStrategy(globalCacheStrategy)
    }

    // MARK: - Prewarming

    override fun prewarm() {
        coreStorage.prewarm()
    }

    // MARK: - Data Upload

    override suspend fun upload(
        data: ByteArray,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        coreStorage.performOperation(
            StorageOperation.Upload(
                data,
                metadata = metadata,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )
    }

    override suspend fun upload(
        file: File,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        coreStorage.performOperation(
            StorageOperation.UploadFile(
                file,
                metadata = metadata,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )
    }

    override fun uploadWithProgress(
        data: ByteArray,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): Flow<StorageTransferProgress> =
        coreStorage.uploadWithProgress(
            data,
            metadata = metadata,
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )

    // MARK: - Deletion

    override suspend fun deleteAllItems(
        path: String,
        includeItemsInSubdirectories: Boolean,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        coreStorage.performOperation(
            StorageOperation.DeleteAllItems(
                path,
                includeItemsInSubdirectories = includeItemsInSubdirectories,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )
    }

    override suspend fun deleteItem(
        path: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        coreStorage.performOperation(
            StorageOperation.DeleteItem(path),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )
    }

    // MARK: - Download

    override suspend fun downloadAllItems(
        path: String,
        toDirectory: File,
        includeItemsInSubdirectories: Boolean,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ) {
        coreStorage.performOperation(
            StorageOperation.DownloadAllItems(
                path,
                toDirectory = toDirectory,
                includeItemsInSubdirectories = includeItemsInSubdirectories,
                cacheStrategy = cacheStrategy,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )
    }

    override suspend fun downloadItem(
        path: String,
        toLocalPath: File,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ) {
        coreStorage.performOperation(
            StorageOperation.DownloadItem(
                path,
                toLocalPath = toLocalPath,
                cacheStrategy = cacheStrategy,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )
    }

    override fun downloadItemWithProgress(
        path: String,
        toLocalPath: File,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ): Flow<StorageTransferProgress> =
        coreStorage.downloadItemWithProgress(
            path,
            toLocalPath = toLocalPath,
            prependingEnvironment = prependingEnvironment,
            cacheStrategy = cacheStrategy,
            timeout = timeout,
        )

    // MARK: - Enumeration

    override suspend fun enumerateEmptyDirectories(
        path: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): Set<String> {
        val result =
            coreStorage.performOperation(
                StorageOperation.EnumerateEmptyDirectories(path),
                prependingEnvironment = prependingEnvironment,
                timeout = timeout,
            ) as? Set<*>

        return result?.filterIsInstance<String>()?.toSet() ?: emptySet()
    }

    override suspend fun getDirectoryListing(
        path: String,
        firstResultOnly: Boolean,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): DirectoryListing =
        coreStorage.performOperation(
            StorageOperation.GetDirectoryListing(
                path,
                firstResultOnly = firstResultOnly,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        ) as DirectoryListing

    override suspend fun itemExists(
        itemType: HostedItemType,
        path: String,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ): Boolean =
        coreStorage.performOperation(
            StorageOperation.ItemExists(
                itemType = itemType,
                path = path,
                cacheStrategy = cacheStrategy,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        ) as? Boolean ?: false

    // MARK: - Clear Store

    override fun clearStore() {
        coreStorage.clearStore()
    }
}
