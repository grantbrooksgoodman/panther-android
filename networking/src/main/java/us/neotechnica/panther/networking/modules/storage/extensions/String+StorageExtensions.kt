//
//  String+StorageExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage.extensions

internal val String.fileName: String?
    get() {
        val fileNamePlusExtension = split("/").lastOrNull() ?: return null
        val fileExtension = fileNamePlusExtension.split(".").lastOrNull()
        if (fileExtension.isNullOrBlank()) return null
        return fileNamePlusExtension
    }
