//
//  HostedItemType.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.models

/**
 * A value that identifies the kind of item at a storage
 * path.
 *
 * Pass a hosted item type to
 * [StorageDelegate.itemExists][us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate]
 * to check whether a file or directory exists at the
 * specified path.
 */
enum class HostedItemType(
    internal val rawValue: String,
) {
    /** A directory that may contain files or subdirectories. */
    DIRECTORY("directory"),

    /** A single file. */
    FILE("file"),
}
