//
//  StorageDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.interfaces

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.storage.models.DirectoryListing
import us.neotechnica.panther.networking.modules.storage.models.HostedItemMetadata
import us.neotechnica.panther.networking.modules.storage.models.HostedItemType
import us.neotechnica.panther.networking.modules.storage.models.StorageTransferProgress
import java.io.File
import kotlin.time.Duration

/**
 * An interface for managing files and directories in hosted
 * storage.
 *
 * Use [StorageDelegate] to upload, download, delete, and
 * inspect files in the backend storage system. Operations
 * support caching, environment-scoped paths, and configurable
 * timeouts:
 *
 * ```kotlin
 * // Upload data.
 * storage.upload(
 *     imageData,
 *     metadata = HostedItemMetadata("images/photo.png"),
 * )
 *
 * // Download a file.
 * storage.downloadItem(
 *     "images/photo.png",
 *     toLocalPath = localFile,
 * )
 * ```
 *
 * By default, paths are prefixed with the active
 * [NetworkEnvironment][us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment]
 * to isolate data across environments. Pass `false` for
 * `prependingEnvironment` to use a raw path.
 *
 * A default implementation backed by Firebase Cloud Storage
 * is provided automatically. The default implementation
 * coalesces identical concurrent operations – when multiple
 * callers perform the same operation at the same time, only
 * one network request is made and all callers receive the
 * same result.
 */
interface StorageDelegate {
    // MARK: - Methods

    /** Removes all locally cached storage data. */
    fun clearStore()

    /**
     * Deletes all items at the specified path.
     *
     * @param path The storage path from which to delete.
     * @param includeItemsInSubdirectories Whether items in
     *   nested subdirectories are also deleted.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the path.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @throws Exception if the deletion fails.
     */
    suspend fun deleteAllItems(
        path: String,
        includeItemsInSubdirectories: Boolean,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
    )

    /**
     * Deletes the item at the specified path.
     *
     * @param path The storage path of the item to delete.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the path.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @throws Exception if the deletion fails.
     */
    suspend fun deleteItem(
        path: String,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
    )

    /**
     * Downloads all items at the specified path to a local
     * directory.
     *
     * @param path The storage path from which to download.
     * @param toDirectory The local directory to write files
     *   to.
     * @param includeItemsInSubdirectories Whether items in
     *   nested subdirectories are also downloaded.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the path.
     * @param cacheStrategy The caching behavior for this
     *   operation.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @throws Exception if the download fails.
     */
    @Suppress("LongParameterList")
    suspend fun downloadAllItems(
        path: String,
        toDirectory: File,
        includeItemsInSubdirectories: Boolean,
        prependingEnvironment: Boolean = true,
        cacheStrategy: CacheStrategy = CacheStrategy.RETURN_CACHE_FIRST,
        timeout: Duration = Networking.defaultOperationTimeout,
    )

    /**
     * Downloads the item at the specified path to a local
     * file.
     *
     * @param path The storage path of the item to download.
     * @param toLocalPath The local file to write to.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the path.
     * @param cacheStrategy The caching behavior for this
     *   operation.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @throws Exception if the download fails.
     */
    suspend fun downloadItem(
        path: String,
        toLocalPath: File,
        prependingEnvironment: Boolean = true,
        cacheStrategy: CacheStrategy = CacheStrategy.RETURN_CACHE_FIRST,
        timeout: Duration = Networking.defaultOperationTimeout,
    )

    /**
     * Downloads the item at the specified path to a local
     * file, reporting transfer progress.
     *
     * The returned flow emits a [StorageTransferProgress]
     * snapshot each time the transfer advances, completes
     * when the download completes, and throws an `Exception`
     * if the download fails. Cancelling the collecting
     * coroutine cancels the transfer. A download satisfied by
     * cache completes immediately with no progress events.
     *
     * Progress-reporting operations are never coalesced with
     * identical concurrent operations. All other behavior –
     * timeouts, environment prepending, and caching – is
     * identical to [downloadItem].
     *
     * @param path The storage path of the item to download.
     * @param toLocalPath The local file to write to.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the path.
     * @param cacheStrategy The caching behavior for this
     *   operation.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @return A flow of transfer progress snapshots.
     */
    fun downloadItemWithProgress(
        path: String,
        toLocalPath: File,
        prependingEnvironment: Boolean = true,
        cacheStrategy: CacheStrategy = CacheStrategy.RETURN_CACHE_FIRST,
        timeout: Duration = Networking.defaultOperationTimeout,
    ): Flow<StorageTransferProgress> =
        channelFlow {
            downloadItem(
                path,
                toLocalPath = toLocalPath,
                prependingEnvironment = prependingEnvironment,
                cacheStrategy = cacheStrategy,
                timeout = timeout,
            )
        }

    /**
     * Recursively finds all empty directories starting at
     * the specified path.
     *
     * @param path The storage path at which to begin the
     *   enumeration.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the path.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @return A set of paths to empty directories.
     *
     * @throws Exception if the enumeration fails.
     */
    suspend fun enumerateEmptyDirectories(
        path: String,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
    ): Set<String>

    /**
     * Returns the contents of the directory at the specified
     * path.
     *
     * @param path The storage path to list.
     * @param firstResultOnly Whether only the first result is
     *   returned.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the path.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @return A [DirectoryListing] describing the directory's
     *   contents.
     *
     * @throws Exception if the listing fails.
     */
    suspend fun getDirectoryListing(
        path: String,
        firstResultOnly: Boolean = false,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
    ): DirectoryListing

    /**
     * Checks whether an item of the specified type exists at
     * the given path.
     *
     * @param itemType The kind of item to check for.
     * @param path The storage path to inspect.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the path.
     * @param cacheStrategy The caching behavior for this
     *   operation.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @return `true` if an item of the specified type exists;
     *   otherwise, `false`.
     *
     * @throws Exception if the check fails.
     */
    suspend fun itemExists(
        itemType: HostedItemType = HostedItemType.FILE,
        path: String,
        prependingEnvironment: Boolean = true,
        cacheStrategy: CacheStrategy = CacheStrategy.RETURN_CACHE_FIRST,
        timeout: Duration = Networking.defaultOperationTimeout,
    ): Boolean

    /**
     * Establishes the underlying connection to hosted storage
     * without performing a data operation.
     *
     * Call this method early in the app lifecycle to overlap
     * connection setup with other initialization work. The
     * method returns immediately; connection establishment
     * proceeds in the background.
     */
    fun prewarm()

    /**
     * Overrides the cache strategy for all storage
     * operations.
     *
     * When set, this strategy takes precedence over any
     * per-operation cache strategy. Pass `null` to revert to
     * per-operation behavior.
     *
     * @param globalCacheStrategy The cache strategy to apply
     *   globally, or `null` to clear the override.
     */
    fun setGlobalCacheStrategy(globalCacheStrategy: CacheStrategy?)

    /**
     * Uploads data to hosted storage with the specified
     * metadata.
     *
     * @param data The data to upload.
     * @param metadata The metadata describing the destination
     *   path and optional HTTP headers.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the metadata's file path.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @throws Exception if the upload fails.
     */
    suspend fun upload(
        data: ByteArray,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
    )

    /**
     * Uploads the specified local file to hosted storage with
     * the specified metadata.
     *
     * Use this method instead of the data-based overload for
     * large files. The file streams from disk rather than
     * being loaded into memory in its entirety.
     *
     * @param file The local file to upload.
     * @param metadata The metadata describing the destination
     *   path and optional HTTP headers.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the metadata's file path.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @throws Exception if the upload fails.
     */
    suspend fun upload(
        file: File,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
    )

    /**
     * Uploads data to hosted storage with the specified
     * metadata, reporting transfer progress.
     *
     * The returned flow emits a [StorageTransferProgress]
     * snapshot each time the transfer advances, completes
     * when the upload completes, and throws an `Exception` if
     * the upload fails. Cancelling the collecting coroutine
     * cancels the transfer.
     *
     * Progress-reporting operations are never coalesced with
     * identical concurrent operations. All other behavior –
     * timeouts and environment prepending – is identical to
     * [upload].
     *
     * @param data The data to upload.
     * @param metadata The metadata describing the destination
     *   path and optional HTTP headers.
     * @param prependingEnvironment Whether the active
     *   environment is prepended to the metadata's file path.
     * @param timeout The maximum time to wait before timing
     *   out.
     *
     * @return A flow of transfer progress snapshots.
     */
    fun uploadWithProgress(
        data: ByteArray,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
    ): Flow<StorageTransferProgress> =
        channelFlow {
            upload(
                data,
                metadata = metadata,
                prependingEnvironment = prependingEnvironment,
                timeout = timeout,
            )
        }
}
