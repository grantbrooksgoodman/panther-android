//
//  SystemInformation.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.services

import java.io.File
import android.os.Build as AndroidBuild

internal object SystemInformation {
    // MARK: - Properties

    val activeCPUs: Long = Runtime.getRuntime().availableProcessors().toLong()
    val deviceName: String = readFirstLine(HOSTNAME_FILE_PATH) ?: UNKNOWN
    val kernelVersion: String = readFirstLine(VERSION_FILE_PATH) ?: UNKNOWN
    val modelCode: String = AndroidBuild.DEVICE ?: UNKNOWN
    val modelName: String = AndroidBuild.MODEL ?: UNKNOWN
    val osRelease: String = System.getProperty("os.version") ?: UNKNOWN
    val osRevision: Long = AndroidBuild.VERSION.SDK_INT.toLong()
    val osType: String = System.getProperty("os.name") ?: UNKNOWN
    val osVersion: String = AndroidBuild.VERSION.RELEASE ?: UNKNOWN

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
