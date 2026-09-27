//
//  StorageMetadata.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.models

/**
 * Metadata attached to a stored file.
 *
 * Use [StorageMetadata] to record a file's content type and arbitrary
 * custom key-value pairs alongside its bytes, so consumers can
 * classify and describe the file without downloading it.
 */
data class StorageMetadata(
    /** The storage path the metadata describes, if known. */
    val filePath: String? = null,
    /** The MIME content type of the file, or `null` if unspecified. */
    val contentType: String? = null,
    /** Arbitrary custom key-value pairs to attach to the file. */
    val customValues: Map<String, String> = emptyMap(),
)
