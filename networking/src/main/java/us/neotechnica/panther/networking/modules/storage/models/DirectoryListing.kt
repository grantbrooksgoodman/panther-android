//
//  DirectoryListing.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.models

import com.google.firebase.storage.ListResult

/**
 * A snapshot of the files and subdirectories at a given
 * path in hosted storage.
 *
 * You receive a [DirectoryListing] from
 * [StorageDelegate.getDirectoryListing][us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate.getDirectoryListing].
 * Inspect its properties to discover what exists at a
 * storage path:
 *
 * ```kotlin
 * val directoryListing =
 *     storage.getDirectoryListing(
 *         "images",
 *     )
 *
 * print(directoryListing.filePaths)
 * print(directoryListing.subdirectories)
 * ```
 */
data class DirectoryListing(
    /** The paths of all files in the directory. */
    val filePaths: Set<String>,
    /** The paths of all subdirectories in the directory. */
    val subdirectories: Set<String>,
) {
    // MARK: - Init

    internal constructor(listResult: ListResult) : this(
        filePaths = listResult.items.map { it.path.trimStart('/') }.toSet(),
        subdirectories = listResult.prefixes.map { it.path.trimStart('/') }.toSet(),
    )
}
