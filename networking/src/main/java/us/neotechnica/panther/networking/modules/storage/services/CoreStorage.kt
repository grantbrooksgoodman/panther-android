//
//  CoreStorage.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.services

import android.net.Uri
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.prependingCurrentEnvironment
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.DataSample
import us.neotechnica.panther.networking.modules.common.models.GuardedOperation
import us.neotechnica.panther.networking.modules.health.models.HealthEvidence
import us.neotechnica.panther.networking.modules.health.models.TransferProgressProbe
import us.neotechnica.panther.networking.modules.storage.extensions.fileName
import us.neotechnica.panther.networking.modules.storage.models.DirectoryListing
import us.neotechnica.panther.networking.modules.storage.models.HostedItemMetadata
import us.neotechnica.panther.networking.modules.storage.models.HostedItemType
import us.neotechnica.panther.networking.modules.storage.models.StorageOperation
import us.neotechnica.panther.networking.modules.storage.models.StorageTransferProgress
import us.neotechnica.panther.subsystem.modules.foundation.extensions.compiledException
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import com.google.firebase.storage.StorageMetadata as FirebaseStorageMetadata

// This service exceeds the file-length and type-body-length limits.

@Suppress("LargeClass", "TooManyFunctions")
internal class CoreStorage {
    // MARK: - Properties

    private val globalCacheStrategy = LockIsolated<CacheStrategy?>(null)

    private val reference by lazy { FirebaseStorage.getInstance().reference }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val storedDownloadItemResults = LockIsolated(mapOf<String, DataSample>())
    private val storedItemExistsResults = LockIsolated(mapOf<String, DataSample>())

    // MARK: - Companion

    private companion object {
        val coalescer = Coalescer<String, Result<Any?>>()
    }

    // MARK: - Global Cache Strategy

    fun setGlobalCacheStrategy(globalCacheStrategy: CacheStrategy?) {
        this.globalCacheStrategy.wrappedValue = globalCacheStrategy
    }

    // MARK: - Prewarming

    fun prewarm() {
        Logger.log(
            "Prewarming storage connection.",
            domain = LoggerDomain.Networking.storage,
        )

        scope.launch {
            Networking.config.activityIndicatorDelegate.show()
            try {
                runCatching { reference.child("prewarm").metadata.await() }
            } finally {
                Networking.config.activityIndicatorDelegate.hide()
            }
        }
    }

    // MARK: - Perform Operation

    suspend fun performOperation(
        operation: StorageOperation,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): Any? {
        val resolvedOperation = operation.resolvingAdaptiveCacheStrategy()
        val resolvedGlobalRawValue = globalCacheStrategy.wrappedValue?.resolved?.rawValue ?: ""

        return coalescer
            .submitUnlessCancelled(
                "CoreStorage.performOperation/" +
                    encodedHashOf(
                        listOf(
                            resolvedOperation.encodedHash +
                                resolvedGlobalRawValue +
                                prependingEnvironment.toString() +
                                timeout.toString(),
                        ),
                    ),
            ) {
                runCatching {
                    _performOperation(
                        resolvedOperation,
                        prependingEnvironment = prependingEnvironment,
                        timeout = timeout,
                    )
                }
            }.getOrThrow()
    }

    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    private suspend fun _performOperation(
        operation: StorageOperation,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): Any? =
        GuardedOperation.run(
            timeout = timeout,
            recordsCensoredSampleOnTimeout = false,
            showsActivityIndicator = true,
            sender = this,
        ) { _ ->
            when (operation) {
                is StorageOperation.DeleteAllItems ->
                    deleteAllItems(
                        path = operation.path.resolving(prependingEnvironment),
                        includeItemsInSubdirectories = operation.includeItemsInSubdirectories,
                    )

                is StorageOperation.DeleteItem ->
                    deleteItem(path = operation.path.resolving(prependingEnvironment))

                is StorageOperation.DownloadAllItems ->
                    downloadAllItems(
                        path = operation.path.resolving(prependingEnvironment),
                        toDirectory = operation.toDirectory,
                        includeItemsInSubdirectories = operation.includeItemsInSubdirectories,
                        cacheStrategy = (globalCacheStrategy.wrappedValue ?: operation.cacheStrategy).resolved,
                    )

                is StorageOperation.DownloadItem ->
                    downloadItem(
                        path = operation.path.resolving(prependingEnvironment),
                        toLocalPath = operation.toLocalPath,
                        cacheStrategy = (globalCacheStrategy.wrappedValue ?: operation.cacheStrategy).resolved,
                    )

                is StorageOperation.EnumerateEmptyDirectories ->
                    enumerateEmptyDirectories(
                        startingAt = operation.startingAt.resolving(prependingEnvironment),
                    )

                is StorageOperation.GetDirectoryListing ->
                    getDirectoryListing(
                        path = operation.path.resolving(prependingEnvironment),
                        firstResultOnly = operation.firstResultOnly,
                    )

                is StorageOperation.ItemExists ->
                    itemExists(
                        itemType = operation.itemType,
                        path = operation.path.resolving(prependingEnvironment),
                        cacheStrategy = (globalCacheStrategy.wrappedValue ?: operation.cacheStrategy).resolved,
                    )

                is StorageOperation.Upload ->
                    upload(
                        operation.data,
                        metadata = operation.metadata,
                        prependingEnvironment = prependingEnvironment,
                    )

                is StorageOperation.UploadFile ->
                    uploadFile(
                        operation.file,
                        metadata = operation.metadata,
                        prependingEnvironment = prependingEnvironment,
                    )
            }
        }

    // MARK: - Progress-Reporting Operations

    fun downloadItemWithProgress(
        path: String,
        toLocalPath: File,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ): Flow<StorageTransferProgress> {
        val resolvedPath = path.resolving(prependingEnvironment)
        val resolvedCacheStrategy = (globalCacheStrategy.wrappedValue ?: cacheStrategy).resolved

        return transferProgressStream(timeout) { onProgress ->
            downloadItem(
                path = resolvedPath,
                toLocalPath = toLocalPath,
                cacheStrategy = resolvedCacheStrategy,
                onProgress = onProgress,
            )
        }
    }

    fun uploadWithProgress(
        data: ByteArray,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): Flow<StorageTransferProgress> =
        transferProgressStream(timeout) { onProgress ->
            upload(
                data,
                metadata = metadata,
                prependingEnvironment = prependingEnvironment,
                onProgress = onProgress,
            )
        }

    private fun transferProgressStream(
        timeout: Duration,
        transfer: suspend (onProgress: (StorageTransferProgress) -> Unit) -> Unit,
    ): Flow<StorageTransferProgress> =
        channelFlow {
            GuardedOperation.checkPreconditions(sender = this@CoreStorage)
            Networking.config.activityIndicatorDelegate.show()

            val timeoutJob =
                launch {
                    delay(timeout)
                    close(Exception.timedOut(ExceptionMetadata(this@CoreStorage)))
                }

            try {
                transfer { trySend(it) }
                timeoutJob.cancel()
                close()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                timeoutJob.cancel()
                close(wrap(throwable))
            }
        }.buffer(Channel.UNLIMITED)
            .onCompletion { Networking.config.activityIndicatorDelegate.hide() }

    // MARK: - Data Upload

    private suspend fun upload(
        data: ByteArray,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean,
        onProgress: ((StorageTransferProgress) -> Unit)? = null,
    ): Any? {
        Logger.log(
            "Uploading data to path \"${metadata.filePath}\".",
            domain = LoggerDomain.Networking.storage,
        )

        storedDownloadItemResults.withValue { it.value = it.value - metadata.filePath }
        storedItemExistsResults.withValue { it.value = it.value - metadata.filePath }

        TransferProgressProbe.measure(
            totalBytes = { data.size },
            onProgress = onProgress,
        ) { probedOnProgress ->
            putDataObservingProgress(
                data,
                metadata = metadata,
                prependingEnvironment = prependingEnvironment,
                onProgress = probedOnProgress,
            )
        }

        return null
    }

    private suspend fun uploadFile(
        file: File,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean,
    ): Any? {
        Logger.log(
            "Uploading file to path \"${metadata.filePath}\".",
            domain = LoggerDomain.Networking.storage,
        )

        storedDownloadItemResults.withValue { it.value = it.value - metadata.filePath }
        storedItemExistsResults.withValue { it.value = it.value - metadata.filePath }

        TransferProgressProbe.measure(
            totalBytes = { file.length().toInt() },
            onProgress = null,
        ) { probedOnProgress ->
            putFileObservingProgress(
                file,
                metadata = metadata,
                prependingEnvironment = prependingEnvironment,
                onProgress = probedOnProgress,
            )
        }

        return null
    }

    // MARK: - Deletion

    private suspend fun deleteAllItems(
        path: String,
        includeItemsInSubdirectories: Boolean,
    ): Any? {
        Logger.log(
            "Deleting all items at path \"$path\".",
            domain = LoggerDomain.Networking.storage,
        )

        _deleteAllItems(
            path,
            includeItemsInSubdirectories = includeItemsInSubdirectories,
        )

        return null
    }

    private suspend fun deleteItem(path: String): Any? {
        _itemExists(
            itemType = HostedItemType.FILE,
            path = path,
            returnCacheOnFailure = false,
        )

        Logger.log(
            "Deleting item at path \"$path\".",
            domain = LoggerDomain.Networking.storage,
        )

        storedDownloadItemResults.withValue { it.value = it.value - path }
        storedItemExistsResults.withValue { it.value = it.value - path }

        HealthEvidence.measure {
            try {
                reference.child(path).delete().await()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                throw wrap(throwable)
            }
        }

        return null
    }

    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    private suspend fun _deleteAllItems(
        path: String,
        includeItemsInSubdirectories: Boolean,
        exceptions: List<Exception> = emptyList(),
    ) {
        val compiledExceptions = exceptions.toMutableList()

        storedDownloadItemResults.withValue { it.value = it.value - path }
        storedItemExistsResults.withValue { it.value = it.value - path }

        Networking.config.activityIndicatorDelegate.show()

        try {
            val directoryListing = getDirectoryListing(path)

            coroutineScope {
                directoryListing.filePaths
                    .map { filePath ->
                        async {
                            try {
                                deleteItem(filePath)
                                null
                            } catch (exception: Exception) {
                                exception
                            }
                        }
                    }.awaitAll()
                    .filterNotNull()
                    .forEach { compiledExceptions.add(it) }
            }

            if (!includeItemsInSubdirectories) {
                compiledExceptions.compiledException?.let { throw it }
                return
            }

            for (subdirectory in directoryListing.subdirectories) {
                try {
                    _deleteAllItems(
                        subdirectory,
                        includeItemsInSubdirectories = includeItemsInSubdirectories,
                        exceptions = compiledExceptions,
                    )
                } catch (exception: Exception) {
                    compiledExceptions.add(exception)
                }
            }

            compiledExceptions.compiledException?.let { throw it }
        } catch (exception: Exception) {
            val underlyingException = compiledExceptions.compiledException ?: throw exception
            throw exception.appending(underlyingException = underlyingException)
        }
    }

    // MARK: - Download

    private suspend fun downloadAllItems(
        path: String,
        toDirectory: File,
        includeItemsInSubdirectories: Boolean,
        cacheStrategy: CacheStrategy,
    ): Any? {
        Logger.log(
            "Downloading all items at path \"$path\".",
            domain = LoggerDomain.Networking.storage,
        )

        _downloadAllItems(
            path,
            toDirectory = toDirectory,
            includeItemsInSubdirectories = includeItemsInSubdirectories,
            cacheStrategy = cacheStrategy,
        )

        return null
    }

    private suspend fun downloadItem(
        path: String,
        toLocalPath: File,
        cacheStrategy: CacheStrategy,
        onProgress: ((StorageTransferProgress) -> Unit)? = null,
    ): Any? {
        if (cacheStrategy == CacheStrategy.RETURN_CACHE_FIRST &&
            storedDownloadItemResultIsValid(
                localPath = toLocalPath,
                networkPath = path,
            )
        ) {
            return null
        }

        Logger.log(
            "Downloading item at path \"$path\".",
            domain = LoggerDomain.Networking.storage,
        )

        val downloadItemStartMillis = System.currentTimeMillis()
        try {
            _downloadItem(
                path,
                toLocalPath = toLocalPath,
                onProgress = onProgress,
            )
        } catch (exception: Exception) {
            if (cacheStrategy != CacheStrategy.RETURN_CACHE_ON_FAILURE ||
                !storedDownloadItemResultIsValid(
                    localPath = toLocalPath,
                    networkPath = path,
                )
            ) {
                throw exception
            }

            Logger.log(exception, domain = LoggerDomain.Networking.storage)
            return null
        }

        storedDownloadItemResults.withValue {
            it.value = it.value +
                (path to DataSample(toLocalPath, Networking.cacheExpiryMilliseconds(downloadItemStartMillis)))
        }

        return null
    }

    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    private suspend fun _downloadAllItems(
        path: String,
        toDirectory: File,
        includeItemsInSubdirectories: Boolean,
        cacheStrategy: CacheStrategy,
        exceptions: List<Exception> = emptyList(),
    ) {
        val compiledExceptions = exceptions.toMutableList()

        Networking.config.activityIndicatorDelegate.show()

        try {
            val directoryListing = getDirectoryListing(path)

            coroutineScope {
                directoryListing.filePaths
                    .mapNotNull { filePath ->
                        val fileName =
                            filePath.fileName ?: run {
                                compiledExceptions.add(
                                    Exception(
                                        "Failed to resolve file name.",
                                        userInfo = mapOf("FilePath" to filePath),
                                        metadata = ExceptionMetadata(this@CoreStorage),
                                    ),
                                )
                                return@mapNotNull null
                            }

                        val destination = File(toDirectory, "$path/$fileName")
                        async {
                            try {
                                downloadItem(
                                    filePath,
                                    toLocalPath = destination,
                                    cacheStrategy = cacheStrategy,
                                )
                                null
                            } catch (exception: Exception) {
                                exception
                            }
                        }
                    }.awaitAll()
                    .filterNotNull()
                    .forEach { compiledExceptions.add(it) }
            }

            if (!includeItemsInSubdirectories) {
                compiledExceptions.compiledException?.let { throw it }
                return
            }

            for (subdirectory in directoryListing.subdirectories) {
                try {
                    _downloadAllItems(
                        subdirectory,
                        toDirectory = toDirectory,
                        includeItemsInSubdirectories = includeItemsInSubdirectories,
                        cacheStrategy = cacheStrategy,
                        exceptions = compiledExceptions,
                    )
                } catch (exception: Exception) {
                    compiledExceptions.add(exception)
                }
            }

            compiledExceptions.compiledException?.let { throw it }
        } catch (exception: Exception) {
            val underlyingException = compiledExceptions.compiledException ?: throw exception
            throw exception.appending(underlyingException = underlyingException)
        }
    }

    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    private suspend fun _downloadItem(
        path: String,
        toLocalPath: File,
        onProgress: ((StorageTransferProgress) -> Unit)? = null,
    ) {
        TransferProgressProbe.measure(
            totalBytes = { toLocalPath.length().toInt() },
            onProgress = onProgress,
        ) { probedOnProgress ->
            writeFileObservingProgress(
                path,
                toLocalPath = toLocalPath,
                onProgress = probedOnProgress,
            )
        }
    }

    // MARK: - Enumeration

    private suspend fun enumerateEmptyDirectories(startingAt: String): Any? {
        Logger.log(
            "Enumerating empty directories, starting at \"$startingAt\".",
            domain = LoggerDomain.Networking.storage,
        )

        return _enumerateEmptyDirectories(startingAt)
    }

    private suspend fun getDirectoryListing(
        path: String,
        firstResultOnly: Boolean = false,
    ): DirectoryListing {
        val listResult =
            HealthEvidence.measure {
                try {
                    if (firstResultOnly) {
                        reference.child(path).list(1).await()
                    } else {
                        reference.child(path).listAll().await()
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    throw wrap(throwable)
                }
            }

        val directoryListing = DirectoryListing(listResult)

        if (directoryListing.filePaths.isEmpty() &&
            directoryListing.subdirectories.isEmpty()
        ) {
            throw Exception.Networking.hostedItemTypeMismatch(
                path = path,
                type = null,
                metadata = ExceptionMetadata(this),
            )
        }

        return directoryListing
    }

    private suspend fun itemExists(
        itemType: HostedItemType,
        path: String,
        cacheStrategy: CacheStrategy,
    ): Any? {
        if (cacheStrategy == CacheStrategy.RETURN_CACHE_FIRST &&
            storedItemExistsResultIsValid(
                itemType = itemType,
                path = path,
            )
        ) {
            return true
        }

        Logger.log(
            "Checking item exists at path \"$path\".",
            domain = LoggerDomain.Networking.storage,
        )

        return try {
            _itemExists(
                itemType = itemType,
                path = path,
                returnCacheOnFailure = cacheStrategy == CacheStrategy.RETURN_CACHE_ON_FAILURE,
            )

            true
        } catch (exception: Exception) {
            Logger.log(
                exception,
                domain = LoggerDomain.Networking.storage,
            )

            false
        }
    }

    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    private suspend fun _enumerateEmptyDirectories(
        startingAt: String,
        with: Set<String> = emptySet(),
        exceptions: List<Exception> = emptyList(),
    ): Set<String> {
        val emptyDirectories = with.toMutableSet()
        val compiledExceptions = exceptions.toMutableList()

        Networking.config.activityIndicatorDelegate.show()

        try {
            val directoryListing = getDirectoryListing(startingAt)

            for (subdirectory in directoryListing.subdirectories) {
                try {
                    emptyDirectories.addAll(
                        _enumerateEmptyDirectories(
                            subdirectory,
                            with = emptyDirectories,
                            exceptions = compiledExceptions,
                        ),
                    )
                } catch (exception: Exception) {
                    compiledExceptions.add(exception)
                }
            }
        } catch (exception: Exception) {
            if (exception.isEqual(to = AppException.Networking.Storage.storageItemDoesNotExist)) {
                emptyDirectories.add(startingAt)
            } else {
                val underlyingException = compiledExceptions.compiledException ?: throw exception
                throw exception.appending(underlyingException = underlyingException)
            }
        }

        compiledExceptions.compiledException?.let { throw it }
        return emptyDirectories
    }

    // The existence check mirrors the storage surface's branch
    // structure.
    @Suppress("CyclomaticComplexMethod", "FunctionNaming", "ktlint:standard:function-naming")
    private suspend fun _itemExists(
        itemType: HostedItemType,
        path: String,
        returnCacheOnFailure: Boolean,
    ) {
        suspend fun itemExists(itemType: HostedItemType): Boolean {
            val startMillis = System.currentTimeMillis()
            var exception: Exception? = null

            if (itemType == HostedItemType.DIRECTORY) {
                try {
                    getDirectoryListing(
                        path,
                        firstResultOnly = true,
                    )
                } catch (listingException: Exception) {
                    exception = listingException
                }
            } else {
                try {
                    getFileMetadata(path)
                } catch (metadataException: Exception) {
                    exception = metadataException
                }
            }

            val cacheExpiryMilliseconds = Networking.cacheExpiryMilliseconds(startMillis)
            val unwrappedException =
                exception ?: run {
                    storedItemExistsResults.withValue {
                        it.value = it.value + (path to DataSample(itemType, cacheExpiryMilliseconds))
                    }

                    return true
                }

            if (!unwrappedException.isEqual(
                    toAny =
                        listOf(
                            AppException.Networking.Storage.genericStorageError,
                            AppException.Networking.Storage.storageItemDoesNotExist,
                        ),
                )
            ) {
                Logger.log(
                    unwrappedException,
                    domain = LoggerDomain.Networking.storage,
                )
            }

            if (returnCacheOnFailure &&
                storedItemExistsResultIsValid(
                    itemType = itemType,
                    path = path,
                )
            ) {
                return true
            }

            return false
        }

        val existsAsFile = itemExists(HostedItemType.FILE)

        if (existsAsFile && itemType == HostedItemType.FILE) return

        val existsAsDirectory = itemExists(HostedItemType.DIRECTORY)

        if (existsAsDirectory && itemType == HostedItemType.DIRECTORY) return

        when {
            existsAsDirectory && itemType == HostedItemType.FILE ->
                throw Exception.Networking.hostedItemTypeMismatch(
                    path = path,
                    type = HostedItemType.FILE,
                    metadata = ExceptionMetadata(this),
                )

            existsAsFile && itemType == HostedItemType.DIRECTORY ->
                throw Exception.Networking.hostedItemTypeMismatch(
                    path = path,
                    type = HostedItemType.DIRECTORY,
                    metadata = ExceptionMetadata(this),
                )

            !existsAsDirectory && !existsAsFile ->
                throw Exception.Networking.hostedItemTypeMismatch(
                    path = path,
                    type = null,
                    metadata = ExceptionMetadata(this),
                )
        }
    }

    // MARK: - Clear Store

    fun clearStore() {
        storedDownloadItemResults.withValue { it.value = mapOf() }
        storedItemExistsResults.withValue { it.value = mapOf() }
    }

    // MARK: - Auxiliary

    private suspend fun getFileMetadata(path: String): FirebaseStorageMetadata =
        HealthEvidence.measure {
            try {
                reference.child(path).metadata.await()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                throw wrap(throwable)
            }
        }

    private suspend fun putDataObservingProgress(
        data: ByteArray,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean,
        onProgress: (StorageTransferProgress) -> Unit,
    ) {
        val filePath = metadata.filePath.resolving(prependingEnvironment)
        awaitTransferCompletion(
            reference.child(filePath).putBytes(data, metadata.asStorageMetadata()),
            progressOf = { snapshot ->
                StorageTransferProgress(
                    completedBytes = snapshot.bytesTransferred,
                    totalBytes = snapshot.totalByteCount,
                )
            },
            onProgress = onProgress,
        )
    }

    private suspend fun putFileObservingProgress(
        file: File,
        metadata: HostedItemMetadata,
        prependingEnvironment: Boolean,
        onProgress: (StorageTransferProgress) -> Unit,
    ) {
        val filePath = metadata.filePath.resolving(prependingEnvironment)
        awaitTransferCompletion(
            reference.child(filePath).putFile(Uri.fromFile(file), metadata.asStorageMetadata()),
            progressOf = { snapshot ->
                StorageTransferProgress(
                    completedBytes = snapshot.bytesTransferred,
                    totalBytes = snapshot.totalByteCount,
                )
            },
            onProgress = onProgress,
        )
    }

    private suspend fun writeFileObservingProgress(
        path: String,
        toLocalPath: File,
        onProgress: (StorageTransferProgress) -> Unit,
    ) {
        toLocalPath.parentFile?.mkdirs()
        awaitTransferCompletion(
            reference.child(path).getFile(toLocalPath),
            progressOf = { snapshot ->
                StorageTransferProgress(
                    completedBytes = snapshot.bytesTransferred,
                    totalBytes = snapshot.totalByteCount,
                )
            },
            onProgress = onProgress,
        )
    }

    private suspend fun awaitTransferCompletion(
        task: UploadTask,
        progressOf: (UploadTask.TaskSnapshot) -> StorageTransferProgress,
        onProgress: (StorageTransferProgress) -> Unit,
    ) {
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                task.addOnProgressListener { snapshot -> onProgress(progressOf(snapshot)) }
                task.addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
                task.addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }

                continuation.invokeOnCancellation { task.cancel() }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            throw wrap(throwable)
        }
    }

    private suspend fun awaitTransferCompletion(
        task: FileDownloadTask,
        progressOf: (FileDownloadTask.TaskSnapshot) -> StorageTransferProgress,
        onProgress: (StorageTransferProgress) -> Unit,
    ) {
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                task.addOnProgressListener { snapshot -> onProgress(progressOf(snapshot)) }
                task.addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
                task.addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }

                continuation.invokeOnCancellation { task.cancel() }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            throw wrap(throwable)
        }
    }

    private fun storedDownloadItemResultIsValid(
        localPath: File,
        networkPath: String,
    ): Boolean {
        val isValid =
            storedDownloadItemResults.withValue {
                val storedDataSample = it.value[networkPath] ?: return@withValue false
                val storedLocalPath = storedDataSample.data as? File

                if (storedDataSample.isExpired ||
                    storedLocalPath == null ||
                    (storedLocalPath.absolutePath != localPath.absolutePath) ||
                    !localPath.exists()
                ) {
                    it.value = it.value - networkPath
                    false
                } else {
                    true
                }
            }

        if (!isValid) return false

        Logger.log(
            "Returning stored download item result for network path \"$networkPath\".",
            domain = LoggerDomain.caches,
        )

        return true
    }

    private fun storedItemExistsResultIsValid(
        itemType: HostedItemType,
        path: String,
    ): Boolean {
        val isValid =
            storedItemExistsResults.withValue {
                val storedDataSample = it.value[path] ?: return@withValue false
                val storedItemExistsResult = storedDataSample.data as? HostedItemType

                if (storedDataSample.isExpired || storedItemExistsResult != itemType) {
                    it.value = it.value - path
                    false
                } else {
                    true
                }
            }

        if (!isValid) return false

        Logger.log(
            "Returning stored item exists result for network path \"$path\".",
            domain = LoggerDomain.caches,
        )

        return true
    }

    // The storage SDK reports failures as coded exceptions; the
    // object-not-found and unknown codes are mapped to their
    // catalogued static error codes so classification and
    // existence checks can match on them.
    private fun wrap(throwable: Throwable): Exception {
        (throwable as? Exception)?.let { return it }

        val staticErrorCode =
            when ((throwable as? StorageException)?.errorCode) {
                StorageException.ERROR_OBJECT_NOT_FOUND -> STORAGE_ITEM_DOES_NOT_EXIST_ERROR_CODE
                StorageException.ERROR_UNKNOWN -> GENERIC_STORAGE_ERROR_CODE
                else -> return Exception.from(throwable, ExceptionMetadata(this))
            }

        return Exception(
            throwable.message ?: throwable.javaClass.simpleName,
            userInfo = mapOf(Exception.UserInfo.STATIC_ERROR_CODE.rawValue to staticErrorCode),
            metadata = ExceptionMetadata(this),
        )
    }

    private fun String.resolving(prependingEnvironment: Boolean): String = if (prependingEnvironment) prependingCurrentEnvironment else this
}

private const val GENERIC_STORAGE_ERROR_CODE = "C81B"
private const val STORAGE_ITEM_DOES_NOT_EXIST_ERROR_CODE = "9207"
