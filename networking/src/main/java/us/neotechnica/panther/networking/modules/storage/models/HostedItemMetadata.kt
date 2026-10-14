//
//  HostedItemMetadata.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.models

import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import com.google.firebase.storage.StorageMetadata as FirebaseStorageMetadata

/**
 * Metadata that describes a file to upload to hosted
 * storage.
 *
 * Create a [HostedItemMetadata] to specify the destination
 * path and optional HTTP metadata for an upload:
 *
 * ```kotlin
 * val metadata =
 *     HostedItemMetadata(
 *         "images/photo.png",
 *         contentType = "image/png",
 *     )
 *
 * storage.upload(
 *     imageData,
 *     metadata = metadata,
 * )
 * ```
 */
data class HostedItemMetadata(
    /** The destination path for the file in hosted storage. */
    val filePath: String,
    /** The `Cache-Control` header for the file. */
    val cacheControl: String? = null,
    /** The `Content-Disposition` header for the file. */
    val contentDisposition: String? = null,
    /** The `Content-Encoding` header for the file. */
    val contentEncoding: String? = null,
    /** The `Content-Language` header for the file. */
    val contentLanguage: String? = null,
    /**
     * The `Content-Type` header for the file, such as
     * `"image/png"` or `"application/json"`.
     */
    val contentType: String? = null,
    /**
     * A map of custom metadata key-value pairs to associate
     * with the file.
     */
    val customValues: Map<String, String>? = null,
) : EncodedHashable {
    // MARK: - Computed Properties

    override val hashFactors: List<String>
        get() {
            val factors =
                mutableListOf(
                    filePath,
                    cacheControl ?: "",
                    contentDisposition ?: "",
                    contentEncoding ?: "",
                    contentLanguage ?: "",
                    contentType ?: "",
                )

            customValues?.let {
                factors.addAll(it.keys)
                factors.addAll(it.values)
            }

            return factors.sorted()
        }

    // MARK: - As StorageMetadata

    // Sets only the values that are present, since the builder sends every
    // value that is set – even an empty or null one, which hosted storage
    // rejects for some fields. The builder is not used as a receiver: its
    // getters share these properties' names and would shadow them.
    internal fun asStorageMetadata(): FirebaseStorageMetadata {
        val builder = FirebaseStorageMetadata.Builder()
        cacheControl?.let { builder.setCacheControl(it) }
        contentDisposition?.let { builder.setContentDisposition(it) }
        contentEncoding?.let { builder.setContentEncoding(it) }
        contentLanguage?.let { builder.setContentLanguage(it) }
        contentType?.let { builder.setContentType(it) }
        customValues?.forEach { (key, value) -> builder.setCustomMetadata(key, value) }
        return builder.build()
    }
}
