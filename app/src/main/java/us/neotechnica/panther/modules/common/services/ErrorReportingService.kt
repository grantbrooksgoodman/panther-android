//
//  ErrorReportingService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.interfaces.ReportDelegate
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.storage.models.StorageMetadata
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
 * with custom storage metadata describing the error, build, device,
 * language, and current user.
 */
object ErrorReportingService : ReportDelegate {
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

    // MARK: - ReportDelegate Conformance

    /**
     * Files a report for the given exception, showing a success toast.
     *
     * @param exception The exception to report.
     */
    override fun fileReport(exception: Exception) {
        fileReport(exception, showsToastOnSuccess = true)
    }

    // MARK: - File Report

    /**
     * Files a report for the given exception.
     *
     * The report uploads asynchronously; this method returns
     * immediately. Exceptions whose codes have already been reported
     * during the current app session are skipped. When the upload
     * succeeds and `showsToastOnSuccess` is `true`, a success toast is
     * shown.
     *
     * @param exception The exception to report.
     * @param showsToastOnSuccess Whether a success toast is shown when
     *   the upload succeeds.
     */
    fun fileReport(
        exception: Exception,
        showsToastOnSuccess: Boolean,
    ) {
        scope.launch {
            val errorCode = exception.code
            if (errorCode in _reportedErrorCodes.wrappedValue) return@launch

            val recordFile = Logger.sessionRecordFilePath ?: return@launch
            val recordBytes = runCatching { recordFile.readBytes() }.getOrNull() ?: return@launch

            val filePath =
                listOf(
                    "reports",
                    Build.bundleVersion,
                    errorCode,
                    "${SimpleDateFormat(FILE_DATE_FORMAT, Locale.US).format(Date())}_${fileNameSuffix()}.txt",
                ).joinToString("/")

            try {
                Networking.config.storageDelegate.uploadBytes(
                    recordBytes,
                    filePath,
                    StorageMetadata(
                        filePath = filePath,
                        contentType = "text/plain",
                        customValues = customValues(exception),
                    ),
                )
            } catch (uploadException: Exception) {
                Logger.log(uploadException, with = AlertType.toast)
                return@launch
            }

            _reportedErrorCodes.wrappedValue = _reportedErrorCodes.wrappedValue + errorCode
            if (!showsToastOnSuccess) return@launch
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

    private fun customValues(exception: Exception): Map<String, String> =
        buildMap {
            put("Error Description", exception.userFacingDescriptor)
            put("Error Code", exception.code)
            put("Build SKU", Build.buildSKU)
            put("Bundle Version", "${Build.bundleVersion} (${Build.buildNumber}${Build.milestone.shortString})")
            put("Bundle Revision", "${Build.bundleRevision} (${Build.revisionBuildNumber})")
            put("Connection Status", if (ConnectionStatusService.isOnline) "online" else "offline")
            put("Device Model", "${AndroidBuild.MODEL} (${AndroidBuild.DEVICE.lowercase()})")
            put("Language Code", RuntimeStorage.languageCode)
            put("OS Version", AndroidBuild.VERSION.RELEASE.lowercase())
            put("Timestamp", SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date()))
        }

    private fun fileNameSuffix(): String = "${Build.milestone.shortString}${Build.buildNumber}${Build.bundleRevision}"
}

private const val FILE_DATE_FORMAT = "yyMMdd"
private const val SUCCESS_TOAST_SECONDS = 3L
private const val TIMESTAMP_FORMAT = "H:mm:ss.SSSS"
