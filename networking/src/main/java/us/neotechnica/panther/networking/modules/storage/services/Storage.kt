//
//  Storage.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.services

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * The Firebase Storage implementation of [StorageDelegate].
 */
class Storage : StorageDelegate {
    // MARK: - Properties

    private val reference by lazy { FirebaseStorage.getInstance().reference }

    // MARK: - StorageDelegate Conformance

    override suspend fun delete(path: String) {
        runGuarded { reference.child(path).delete().await() }
    }

    override suspend fun downloadBytes(
        path: String,
        maxBytes: Long,
    ): ByteArray = runGuarded { reference.child(path).getBytes(maxBytes).await() }

    override suspend fun uploadBytes(
        bytes: ByteArray,
        path: String,
    ) {
        runGuarded { reference.child(path).putBytes(bytes).await() }
    }

    // MARK: - Auxiliary

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
}
