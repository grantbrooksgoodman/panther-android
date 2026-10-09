//
//  SystemInformation.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import java.io.File
import android.os.Build as AndroidBuild

/**
 * A namespace for low-level hardware and kernel values.
 *
 * Use [SystemInformation] to read details about the device and
 * operating system. Each property returns a fallback value –
 * `"Unknown"` or zero – when the underlying query fails.
 */
object SystemInformation {
    // MARK: - Computed Properties

    /** The number of CPUs available on the device. */
    val activeCPUs: Long
        get() = Runtime.getRuntime().availableProcessors().toLong()

    /** The kernel's host name, or `"Unknown"` if the query fails. */
    val deviceName: String
        get() = readFirstLine(HOSTNAME_FILE_PATH) ?: UNKNOWN

    /** The kernel's version string, or `"Unknown"` if the query fails. */
    val kernelVersion: String
        get() = readFirstLine(VERSION_FILE_PATH) ?: UNKNOWN

    /**
     * The device's hardware code name, or `"Unknown"` if the query
     * fails.
     */
    val modelCode: String
        get() = AndroidBuild.DEVICE ?: UNKNOWN

    /**
     * The device's end-user-visible model name, or `"Unknown"` if
     * the query fails.
     */
    val modelName: String
        get() = AndroidBuild.MODEL ?: UNKNOWN

    /** The kernel's release string, or `"Unknown"` if the query fails. */
    val osRelease: String
        get() = System.getProperty("os.version") ?: UNKNOWN

    /** The operating system's API level. */
    val osRevision: Long
        get() = AndroidBuild.VERSION.SDK_INT.toLong()

    /** The operating system's type string, or `"Unknown"` if the query fails. */
    val osType: String
        get() = System.getProperty("os.name") ?: UNKNOWN

    /**
     * The operating system's user-visible version string, or
     * `"Unknown"` if the query fails.
     */
    val osVersion: String
        get() = AndroidBuild.VERSION.RELEASE ?: UNKNOWN

    // MARK: - Auxiliary

    private fun readFirstLine(path: String): String? =
        runCatching {
            File(path)
                .readLines()
                .firstOrNull()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
}

private const val HOSTNAME_FILE_PATH = "/proc/sys/kernel/hostname"
private const val UNKNOWN = "Unknown"
private const val VERSION_FILE_PATH = "/proc/version"
