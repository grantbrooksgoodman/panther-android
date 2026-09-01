//
//  ErrorReportingService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 01/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.services.ConnectionStatusService
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import android.os.Build as AndroidBuild

/**
 * Uploads error reports to remote storage.
 *
 * A report consists of the current logger session record, uploaded
 * with a header describing the error, build, device, language, and
 * current user.
 *
 * **Note:** the Android `StorageDelegate` upload does not yet carry
 * custom metadata, so the report's metadata is prepended to the
 * uploaded session record as a text header rather than attached as
 * custom storage values.
 */
object ErrorReportingService {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _reportedErrorCodes = LockIsolated(listOf<String>())

    // MARK: - Computed Properties

    /**
     * The codes of errors reported during the current app session.
     *
     * Each error code is reported at most once per session.
     */
    val reportedErrorCodes: List<String>
        get() = _reportedErrorCodes.wrappedValue

    // MARK: - File Report

    /**
     * Files a report for the given exception.
     *
     * The report uploads asynchronously; this method returns
     * immediately. Exceptions whose codes have already been reported
     * during the current app session are skipped. When the upload
     * succeeds, a success toast is shown.
     *
     * @param exception The exception to report.
     */
    fun fileReport(exception: Exception) {
        scope.launch {
            val errorCode = exception.code
            if (errorCode in _reportedErrorCodes.wrappedValue) return@launch

            val recordFile = Logger.sessionRecordFilePath ?: return@launch
            val recordBytes = runCatching { recordFile.readBytes() }.getOrNull() ?: return@launch

            val payload = (header(exception) + SESSION_RECORD_SEPARATOR + String(recordBytes)).toByteArray()
            val filePath =
                listOf(
                    "reports",
                    Build.bundleVersion,
                    errorCode,
                    "${SimpleDateFormat(FILE_DATE_FORMAT, Locale.US).format(Date())}_${fileNameSuffix()}.txt",
                ).joinToString("/")

            try {
                Networking.config.storageDelegate.uploadBytes(payload, filePath)
            } catch (uploadException: Exception) {
                Logger.log(uploadException, with = AlertType.toast)
                return@launch
            }

            _reportedErrorCodes.wrappedValue = _reportedErrorCodes.wrappedValue + errorCode
            Toast.show(
                Toast(
                    Toast.Type.Capsule(ToastStyle.SUCCESS),
                    message = "Error reported successfully.",
                    perpetuation = Toast.Perpetuation.Ephemeral(SUCCESS_TOAST_SECONDS.seconds),
                ),
            )
        }
    }

    // MARK: - Auxiliary

    private fun header(exception: Exception): String =
        buildString {
            appendLine("Error Description: ${exception.userFacingDescriptor}")
            appendLine("Error Code: ${exception.code}")
            appendLine("Build SKU: ${Build.buildSKU}")
            appendLine("Bundle Version: ${Build.bundleVersion} (${Build.buildNumber}${Build.milestone.shortString})")
            appendLine("Bundle Revision: ${Build.bundleRevision} (${Build.revisionBuildNumber})")
            appendLine("Connection Status: ${if (ConnectionStatusService.isOnline) "online" else "offline"}")
            appendLine("Device Model: ${AndroidBuild.MODEL} (${AndroidBuild.DEVICE.lowercase()})")
            appendLine("Language Code: ${RuntimeStorage.languageCode}")
            appendLine("OS Version: ${AndroidBuild.VERSION.RELEASE.lowercase()}")
            appendLine("Timestamp: ${SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date())}")
        }

    private fun fileNameSuffix(): String =
        "${Build.milestone.shortString}${Build.buildNumber}${Build.bundleRevision}"
}

private const val FILE_DATE_FORMAT = "yyMMdd"
private const val SESSION_RECORD_SEPARATOR = "\n\n---\n\n"
private const val SUCCESS_TOAST_SECONDS = 3L
private const val TIMESTAMP_FORMAT = "H:mm:ss.SSSS"
