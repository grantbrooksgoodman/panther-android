//
//  StorageDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.interfaces

/**
 * An interface for reading and writing binary files in remote
 * storage.
 *
 * **Note:** This Phase 2 port provides the download and upload
 * primitives only. Media-specific features – transcoding,
 * progress reporting, and directory listing – are added in later
 * phases.
 *
 * A default implementation backed by Firebase Storage is provided
 * automatically.
 */
interface StorageDelegate {
    // MARK: - Methods

    /**
     * Deletes the file at the specified storage path.
     *
     * @param path The storage path to delete.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if the deletion fails.
     */
    suspend fun delete(path: String)

    /**
     * Downloads the bytes of the file at the specified storage
     * path.
     *
     * @param path The storage path to download from.
     * @param maxBytes The maximum number of bytes to download.
     *
     * @return The downloaded bytes.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if the download fails.
     */
    suspend fun downloadBytes(
        path: String,
        maxBytes: Long,
    ): ByteArray

    /**
     * Uploads the given bytes to the specified storage path.
     *
     * @param bytes The bytes to upload.
     * @param path The storage path to upload to.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if the upload fails.
     */
    suspend fun uploadBytes(
        bytes: ByteArray,
        path: String,
    )
}
