//
//  FileStore.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import android.content.Context
import java.io.File

/**
 * The app's on-disk document store.
 *
 * The Android analog of the iOS `FileManager.documentsDirectoryURL`,
 * this resolves paths against the application's private files
 * directory so relative paths remain valid across launches.
 * [initialize] must be called once with the application context
 * before use.
 */
object FileStore {
    // MARK: - Properties

    @Volatile
    private var appContext: Context? = null

    // MARK: - Computed Properties

    /** The root documents directory, or `null` before initialization. */
    val documentsDirectory: File?
        get() = appContext?.filesDir

    // MARK: - Initialization

    /** Prepares the file store for use. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Accessors

    /**
     * The absolute file for [relativePath], resolved against the
     * documents directory, or `null` before initialization.
     */
    fun resolve(relativePath: String): File? = documentsDirectory?.let { File(it, relativePath) }

    /** Whether a file exists at [relativePath]. */
    fun exists(relativePath: String): Boolean = resolve(relativePath)?.exists() == true

    /**
     * Writes [bytes] to [relativePath], creating parent directories as
     * needed, and returns the written file (or `null` before
     * initialization).
     */
    fun write(
        relativePath: String,
        bytes: ByteArray,
    ): File? {
        val file = resolve(relativePath) ?: return null
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        return file
    }
}
