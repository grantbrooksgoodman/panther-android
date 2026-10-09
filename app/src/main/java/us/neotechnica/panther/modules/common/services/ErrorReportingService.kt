//
//  ErrorReportingService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.interfaces.ReportDelegate
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.common.models.SystemInformation
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.navigation.descriptor
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.storage.models.HostedItemMetadata
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ErrorReportDelegate
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.foundation.services.TimestampDateFormatter
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.translator.Translator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

/**
 * Uploads error reports to remote storage.
 *
 * A report consists of the current logger session record, uploaded
 * with custom storage metadata describing the error, build, device,
 * language, current user, and visible view.
 */
object ErrorReportingService : ReportDelegate, ErrorReportDelegate {
    // MARK: - Dependencies

    private val timestampDateFormatter: TimestampDateFormatter by Dependency { it.timestampDateFormatter }

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
     * succeeds, `showsToastOnSuccess` is `true`, and automatic error
     * reporting is disabled, a success toast is shown; in developer
     * mode, tapping the toast opens the report's storage location and
     * copies its URL to the clipboard.
     *
     * @param exception The exception to report.
     * @param showsToastOnSuccess Whether a success toast may be shown
     *   when the upload succeeds.
     */
    fun fileReport(
        exception: Exception,
        showsToastOnSuccess: Boolean,
    ) {
        scope.launch {
            val recordFile = Logger.sessionRecordFilePath ?: return@launch
            val recordBytes = runCatching { recordFile.readBytes() }.getOrNull() ?: return@launch

            val errorCode = exception.id.take(ERROR_CODE_LENGTH).uppercase()
            if (errorCode in _reportedErrorCodes.wrappedValue) return@launch

            val parentDirectoryName = parentDirectoryName(exception, errorCode)
            val filePath =
                listOf(
                    "reports",
                    Build.bundleVersion,
                    parentDirectoryName,
                    "${SimpleDateFormat(FILE_DATE_FORMAT, Locale.US).format(Date())}_${fileNameSuffix(Date())}.txt",
                ).joinToString("/")

            if (!upload(recordBytes, filePath, exception)) return@launch

            _reportedErrorCodes.wrappedValue = _reportedErrorCodes.wrappedValue + errorCode
            if (!showsToastOnSuccess || Logger.reportsErrorsAutomatically) return@launch

            Toast.show(
                Toast(
                    Toast.ToastType.Capsule(ToastStyle.SUCCESS),
                    message = LocalizedStringKey.ErrorReportedSuccessfully.localized(),
                    perpetuation =
                        if (Build.isDeveloperModeEnabled) {
                            Toast.PerpetuationStrategy.Persistent
                        } else {
                            Toast.PerpetuationStrategy.Ephemeral(SUCCESS_TOAST_SECONDS.seconds)
                        },
                ),
                onTap = toastAction(parentDirectoryName),
            )
        }
    }

    // MARK: - Auxiliary

    private suspend fun upload(
        recordBytes: ByteArray,
        filePath: String,
        exception: Exception,
    ): Boolean {
        try {
            Networking.config.storageDelegate.upload(
                recordBytes,
                metadata =
                    HostedItemMetadata(
                        filePath,
                        contentType = "text/plain",
                        customValues = customValues(exception),
                    ),
            )
        } catch (uploadException: Exception) {
            // Reporting the upload failure must not recurse into another automatic report.
            if (!Logger.reportsErrorsAutomatically) {
                Logger.log(uploadException, with = AlertType.toast)
                return false
            }
            Logger.setReportsErrorsAutomatically(false)
            Logger.log(uploadException, with = AlertType.toast)
            Logger.setReportsErrorsAutomatically(true)
            return false
        }
        return true
    }

    internal fun parentDirectoryName(
        exception: Exception,
        errorCode: String,
    ): String {
        val exceptionDescriptor = exception.userInfo?.get(Exception.UserInfo.DESCRIPTOR.rawValue) as? String
        var parentDirectoryName = exception.userInfo?.get(HOSTED_OVERRIDE_ERROR_CODE_KEY) as? String ?: errorCode
        if (exceptionDescriptor != null &&
            exception.userInfo?.get(Exception.UserInfo.STATIC_ERROR_CODE.rawValue) == null
        ) {
            parentDirectoryName = "${parentDirectoryName}_${exceptionDescriptor.shorthandErrorDescriptor}"
        }

        return parentDirectoryName
    }

    internal fun fileNameSuffix(date: Date): String {
        val shortDateHash =
            encodedHashOf(listOf(timestampDateFormatter.format(date)))
                .take(SHORT_DATE_HASH_LENGTH)

        return listOf(
            Build.milestone.shortString,
            Build.buildNumber.toString(),
            Build.bundleRevision,
            "_$shortDateHash",
        ).joinToString("")
    }

    internal fun standardUserInfo(date: Date): Map<String, String> =
        buildMap {
            put("Build SKU", Build.buildSKU)
            put("Bundle Revision", "${Build.bundleRevision} (${Build.revisionBuildNumber})")
            put("Bundle Version", "${Build.bundleVersion} (${Build.buildNumber}${Build.milestone.shortString})")
            put("Connection Status", if (Build.isOnline) "online" else "offline")
            put("Device Model", "${SystemInformation.modelName} (${SystemInformation.modelCode.lowercase()})")
            put("Language Code", RuntimeStorage.languageCode)
            put("OS Version", SystemInformation.osVersion.lowercase())
            put("Project ID", Build.projectID)
            put("Timestamp", timestampDateFormatter.format(date))
            User.currentUserID?.let { put("Current User ID", it) }

            val viewID = DependencyValues.current.navigation.state.value.descriptor
            if (viewID != null) put("View ID", viewID)
        }

    private fun customValues(exception: Exception): Map<String, String> {
        val passthrough =
            (exception.userInfo ?: emptyMap())
                .mapNotNull { (key, value) -> (value as? String)?.let { key to it } }
                .toMap()
                .filterKeys { it !in RESERVED_USER_INFO_KEYS }

        val exceptionDescriptor = exception.userInfo?.get(Exception.UserInfo.DESCRIPTOR.rawValue) as? String
        return passthrough +
            mapOf("Error Description" to (exceptionDescriptor ?: exception.descriptor)) +
            standardUserInfo(Date())
    }

    private fun toastAction(parentDirectoryName: String): (() -> Unit)? {
        if (!Build.isDeveloperModeEnabled) return null
        val urlStringPrefix = MetadataService.storageReferenceURL ?: return null

        val urlString =
            urlStringPrefix +
                listOf(Networking.config.environment.shortString, "reports", Build.bundleVersion, parentDirectoryName)
                    .joinToString("~2F") { Uri.encode(it) }

        return {
            val activity = Translator.config.currentActivityProvider?.invoke()
            if (activity != null) {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlString)))
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Report URL", urlString))
            }
        }
    }

    private val String.shorthandErrorDescriptor: String
        get() =
            trim { it.isPunctuation }
                .uppercase()
                .split(" ")
                .map { word -> word.filter { it.isLetter() }.trim() }
                .filter { it.isNotBlank() }
                .filterNot { it in SHORTHAND_EXCLUDED_WORDS }
                .take(SHORTHAND_WORD_LIMIT)
                .joinToString("_")

    private val Char.isPunctuation: Boolean
        get() =
            when (Character.getType(this).toByte()) {
                Character.CONNECTOR_PUNCTUATION,
                Character.DASH_PUNCTUATION,
                Character.END_PUNCTUATION,
                Character.FINAL_QUOTE_PUNCTUATION,
                Character.INITIAL_QUOTE_PUNCTUATION,
                Character.OTHER_PUNCTUATION,
                Character.START_PUNCTUATION,
                -> true

                else -> false
            }
}

private const val ERROR_CODE_LENGTH = 4
private const val FILE_DATE_FORMAT = "yyMMdd"
private const val HOSTED_OVERRIDE_ERROR_CODE_KEY = "HostedOverrideErrorCode"
private const val SHORT_DATE_HASH_LENGTH = 5
private const val SHORTHAND_WORD_LIMIT = 3
private const val SUCCESS_TOAST_SECONDS = 3L
private val RESERVED_USER_INFO_KEYS = setOf("Descriptor", "ErrorCode", "HostedOverrideErrorCode", "StaticErrorCode")
private val SHORTHAND_EXCLUDED_WORDS = setOf("A", "AN", "BEEN", "HAS", "IS", "THE", "WAS")
