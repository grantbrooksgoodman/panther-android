//
//  StorageOperation.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.models

import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import java.io.File
import java.security.MessageDigest

internal sealed interface StorageOperation : EncodedHashable {
    // MARK: - Cases

    data class DeleteAllItems(
        val path: String,
        val includeItemsInSubdirectories: Boolean,
    ) : StorageOperation {
        override val hashFactors: List<String>
            get() = listOf(path, includeItemsInSubdirectories.toString())
    }

    data class DeleteItem(
        val path: String,
    ) : StorageOperation {
        override val hashFactors: List<String>
            get() = listOf(path)
    }

    data class DownloadAllItems(
        val path: String,
        val toDirectory: File,
        val includeItemsInSubdirectories: Boolean,
        val cacheStrategy: CacheStrategy,
    ) : StorageOperation {
        override val hashFactors: List<String>
            get() =
                listOf(
                    path,
                    toDirectory.absolutePath,
                    includeItemsInSubdirectories.toString(),
                    cacheStrategy.rawValue,
                )
    }

    data class DownloadItem(
        val path: String,
        val toLocalPath: File,
        val cacheStrategy: CacheStrategy,
    ) : StorageOperation {
        override val hashFactors: List<String>
            get() = listOf(path, toLocalPath.absolutePath, cacheStrategy.rawValue)
    }

    data class EnumerateEmptyDirectories(
        val startingAt: String,
    ) : StorageOperation {
        override val hashFactors: List<String>
            get() = listOf(startingAt)
    }

    data class GetDirectoryListing(
        val path: String,
        val firstResultOnly: Boolean,
    ) : StorageOperation {
        override val hashFactors: List<String>
            get() = listOf(path, firstResultOnly.toString())
    }

    data class ItemExists(
        val itemType: HostedItemType,
        val path: String,
        val cacheStrategy: CacheStrategy,
    ) : StorageOperation {
        override val hashFactors: List<String>
            get() = listOf(itemType.rawValue, path, cacheStrategy.rawValue)
    }

    data class Upload(
        val data: ByteArray,
        val metadata: HostedItemMetadata,
    ) : StorageOperation {
        override val hashFactors: List<String>
            get() = listOf(data.encodedHash, metadata.encodedHash)

        override fun equals(other: Any?): Boolean =
            other is Upload &&
                data.contentEquals(other.data) &&
                metadata == other.metadata

        override fun hashCode(): Int = 31 * data.contentHashCode() + metadata.hashCode()
    }

    data class UploadFile(
        val file: File,
        val metadata: HostedItemMetadata,
    ) : StorageOperation {
        override val hashFactors: List<String>
            get() = listOf(file.absolutePath, metadata.encodedHash)
    }

    // MARK: - Methods

    /**
     * Returns a copy with any [CacheStrategy.ADAPTIVE] cache
     * strategy resolved to a concrete value.
     */
    fun resolvingAdaptiveCacheStrategy(): StorageOperation =
        when (this) {
            is DownloadAllItems ->
                if (cacheStrategy == CacheStrategy.ADAPTIVE) copy(cacheStrategy = cacheStrategy.resolved) else this

            is DownloadItem ->
                if (cacheStrategy == CacheStrategy.ADAPTIVE) copy(cacheStrategy = cacheStrategy.resolved) else this

            is ItemExists ->
                if (cacheStrategy == CacheStrategy.ADAPTIVE) copy(cacheStrategy = cacheStrategy.resolved) else this

            is DeleteAllItems, is DeleteItem, is EnumerateEmptyDirectories, is GetDirectoryListing,
            is Upload, is UploadFile,
            -> this
        }
}

private val ByteArray.encodedHash: String
    get() =
        MessageDigest
            .getInstance("SHA-256")
            .digest(this)
            .joinToString("") { "%02x".format(it) }
