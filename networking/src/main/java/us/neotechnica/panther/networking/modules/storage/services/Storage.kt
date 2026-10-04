//
//  Storage.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.services

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata as FirebaseStorageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.health.models.TransferProgressProbe
import us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate
import us.neotechnica.panther.networking.modules.storage.models.StorageMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.io.File

/**
 * The Firebase Storage implementation of [StorageDelegate].
 */
class Storage : StorageDelegate {
    // MARK: - Properties

    private val reference by lazy { FirebaseStorage.getInstance().reference }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // MARK: - StorageDelegate Conformance

    override suspend fun delete(path: String) {
        runGuarded { reference.child(environmentPath(path)).delete().await() }
    }

    override suspend fun downloadBytes(
        path: String,
        maxBytes: Long,
    ): ByteArray {
        val start = System.currentTimeMillis()
        val bytes = runGuarded { reference.child(environmentPath(path)).getBytes(maxBytes).await() }
        recordThroughput(bytes.size, start)
        return bytes
    }

    override suspend fun download(
        path: String,
        toFile: File,
    ) {
        val probe = TransferProgressProbe()
        try {
            runGuarded {
                toFile.parentFile?.mkdirs()
                reference
                    .child(environmentPath(path))
                    .getFile(toFile)
                    .addOnProgressListener { probe.handleProgress(it.bytesTransferred) }
                    .await()
            }
        } catch (exception: Exception) {
            probe.invalidate()
            throw exception
        }
        probe.finish(toFile.length().toInt())
    }

    override suspend fun uploadBytes(
        bytes: ByteArray,
        path: String,
        metadata: StorageMetadata?,
    ) {
        val probe = TransferProgressProbe()
        try {
            runGuarded {
                val ref = reference.child(environmentPath(path))
                val firebaseMetadata = metadata?.let(::firebaseMetadata)
                val task = if (firebaseMetadata != null) ref.putBytes(bytes, firebaseMetadata) else ref.putBytes(bytes)
                task.addOnProgressListener { probe.handleProgress(it.bytesTransferred) }.await()
            }
        } catch (exception: Exception) {
            probe.invalidate()
            throw exception
        }
        probe.finish(bytes.size)
    }

    override suspend fun upload(
        file: File,
        path: String,
        metadata: StorageMetadata?,
    ) {
        val probe = TransferProgressProbe()
        try {
            runGuarded {
                val ref = reference.child(environmentPath(path))
                val uri = Uri.fromFile(file)
                val firebaseMetadata = metadata?.let(::firebaseMetadata)
                val task = if (firebaseMetadata != null) ref.putFile(uri, firebaseMetadata) else ref.putFile(uri)
                task.addOnProgressListener { probe.handleProgress(it.bytesTransferred) }.await()
            }
        } catch (exception: Exception) {
            probe.invalidate()
            throw exception
        }
        probe.finish(file.length().toInt())
    }

    override suspend fun itemExists(path: String): Boolean =
        runCatching {
            reference.child(environmentPath(path)).metadata.await()
            true
        }.getOrDefault(false)

    override fun prewarm() {
        Logger.log(
            "Prewarming storage connection.",
            domain = LoggerDomain.Networking.storage,
        )

        scope.launch {
            Networking.config.activityIndicatorDelegate.show()
            try {
                runCatching { reference.child(environmentPath("prewarm")).metadata.await() }
            } finally {
                Networking.config.activityIndicatorDelegate.hide()
            }
        }
    }

    // MARK: - Auxiliary

    /**
     * Prepends the active environment's short string to [path] so
     * storage is isolated per environment (for example,
     * `"media/x.jpg"` → `"dev/media/x.jpg"`).
     */
    private fun environmentPath(path: String): String = "${Networking.config.environment.shortString}/${path.trim('/')}"

    private fun firebaseMetadata(metadata: StorageMetadata): FirebaseStorageMetadata =
        FirebaseStorageMetadata
            .Builder()
            .apply {
                metadata.contentType?.let { setContentType(it) }
                metadata.customValues.forEach { (key, value) -> setCustomMetadata(key, value) }
            }.build()

    private fun recordThroughput(
        byteCount: Int,
        startMillis: Long,
    ) {
        val seconds = (System.currentTimeMillis() - startMillis) / MILLIS_PER_SECOND
        Networking.health.recordThroughputSample(byteCount, seconds)
    }

    private suspend fun <T> runGuarded(operation: suspend () -> T): T {
        if (!Networking.isReadWriteEnabled) {
            throw Exception(
                "Read/write access is currently disabled.",
                metadata = ExceptionMetadata(this),
            )
        }

        Networking.config.activityIndicatorDelegate.show()
        return try {
            operation()
        } catch (throwable: Throwable) {
            throw (throwable as? Exception) ?: Exception.from(throwable, ExceptionMetadata(this))
        } finally {
            Networking.config.activityIndicatorDelegate.hide()
        }
    }

    // MARK: - Companion

    private companion object {
        private const val MILLIS_PER_SECOND = 1000.0
    }
}
