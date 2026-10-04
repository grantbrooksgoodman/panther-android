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
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.navigation.descriptor
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.storage.models.StorageMetadata
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ErrorReportDelegate
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.Translator
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
 * language, current user, and visible view.
 */
object ErrorReportingService : ReportDelegate, ErrorReportDelegate {
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

    private val bundleVersionString: String
        get() = "${if (Build.milestone == Milestone.GENERAL_RELEASE) Build.finalName else Build.codeName} (${Build.bundleVersion})"

    // MARK: - ReportDelegate Conformance

    /**
     * Files a report for the given exception, showing a success toast.
     *
     * @param exception The exception to report.
     */
    override fun fileReport(exception: Exception) {
        fileReport(exception, showsToastOnSuccess = true)
    }

    /**
     * Composes and presents a bug report.
     *
     * The message prompts the user to describe the issue and the steps
     * to reproduce it.
     */
    override fun reportBug() {
        composeMessage(
            subject = "$bundleVersionString Bug Report",
            body = "In the appropriate section, please describe the error encountered and the steps to reproduce it.",
            prompt = "Description/Steps to Reproduce",
        )
    }

    /**
     * Composes and presents a general feedback message.
     *
     * The message invites the user to share general feedback.
     */
    override fun sendFeedback() {
        composeMessage(
            subject = "$bundleVersionString Feedback Report",
            body = "Any general feedback is appreciated in the appropriate section.",
            prompt = "General Feedback",
        )
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
                    "${SimpleDateFormat(FILE_DATE_FORMAT, Locale.US).format(Date())}_${fileNameSuffix()}.txt",
                ).joinToString("/")

            if (!upload(recordBytes, filePath, exception)) return@launch

            _reportedErrorCodes.wrappedValue = _reportedErrorCodes.wrappedValue + errorCode
            if (!showsToastOnSuccess || Logger.reportsErrorsAutomatically) return@launch

            Toast.show(
                Toast(
                    Toast.Type.Capsule(ToastStyle.SUCCESS),
                    message = LocalizedStringKey.ErrorReportedSuccessfully.localized(),
                    perpetuation =
                        if (Build.isDeveloperModeEnabled) {
                            Toast.Perpetuation.Persistent
                        } else {
                            Toast.Perpetuation.Ephemeral(SUCCESS_TOAST_SECONDS.seconds)
                        },
                ),
                onTap = toastAction(parentDirectoryName),
            )
        }
    }

    // MARK: - Auxiliary

    private fun composeMessage(
        subject: String,
        body: String,
        prompt: String,
    ) {
        val activity = Translator.config.currentActivityProvider?.invoke() ?: return
        val bodyText =
            buildString {
                append(body)
                append("\n\n")
                append("$prompt:")
                append("\n\n")
                append("---\n")
                append("Device: ${AndroidBuild.MANUFACTURER} ${AndroidBuild.MODEL} (API ${AndroidBuild.VERSION.SDK_INT})")
            }

        val mailIntent =
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORT_RECIPIENT))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, bodyText)
            }

        runCatching { activity.startActivity(mailIntent) }
    }

    private suspend fun upload(
        recordBytes: ByteArray,
        filePath: String,
        exception: Exception,
    ): Boolean {
        try {
            Networking.config.storageDelegate.uploadBytes(
                recordBytes,
                filePath,
                StorageMetadata(filePath = filePath, contentType = "text/plain", customValues = customValues(exception)),
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

    private fun parentDirectoryName(
        exception: Exception,
        errorCode: String,
    ): String {
        val hostedOverrideErrorCode = exception.userInfo?.get(HOSTED_OVERRIDE_ERROR_CODE_KEY) as? String
        val staticErrorCode = exception.userInfo?.get(Exception.UserInfo.STATIC_ERROR_CODE.rawValue) as? String

        val base = hostedOverrideErrorCode ?: errorCode
        return if (staticErrorCode == null) "${base}_${exception.descriptor.shorthandErrorDescriptor()}" else base
    }

    private fun customValues(exception: Exception): Map<String, String> {
        val passthrough =
            (exception.userInfo ?: emptyMap())
                .mapNotNull { (key, value) -> (value as? String)?.let { key to it } }
                .toMap()
                .filterKeys { it !in RESERVED_USER_INFO_KEYS }

        return passthrough +
            mapOf("Error Description" to exception.descriptor) +
            standardUserInfo()
    }

    private fun standardUserInfo(): Map<String, String> =
        buildMap {
            put("Build SKU", Build.buildSKU)
            put("Bundle Revision", "${Build.bundleRevision} (${Build.revisionBuildNumber})")
            put("Bundle Version", "${Build.bundleVersion} (${Build.buildNumber}${Build.milestone.shortString})")
            put("Connection Status", if (ConnectionStatusService.isOnline) "online" else "offline")
            put("Device Model", "${AndroidBuild.MODEL} (${AndroidBuild.DEVICE.lowercase()})")
            put("Language Code", RuntimeStorage.languageCode)
            put("OS Version", AndroidBuild.VERSION.RELEASE.lowercase())
            put("Project ID", Build.projectID)
            put("Timestamp", SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date()))
            User.currentUserID?.let { put("Current User ID", it) }

            val viewID = DependencyValues.current.navigation.state.value.descriptor
            if (viewID != null) put("View ID", viewID)
        }

    private fun fileNameSuffix(): String {
        val timestamp = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date())
        val shortDateHash = encodedHashOf(listOf(timestamp)).take(SHORT_DATE_HASH_LENGTH)
        return "${Build.milestone.shortString}${Build.buildNumber}${Build.bundleRevision}_$shortDateHash"
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

    private fun String.shorthandErrorDescriptor(): String =
        uppercase()
            .split(Regex("\\s+"))
            .map { word -> word.filter { it.isLetter() } }
            .filter { it.isNotBlank() }
            .filterNot { it in SHORTHAND_EXCLUDED_WORDS }
            .take(SHORTHAND_WORD_LIMIT)
            .joinToString("_")
}

private const val ERROR_CODE_LENGTH = 4
private const val FILE_DATE_FORMAT = "yyMMdd"
private const val HOSTED_OVERRIDE_ERROR_CODE_KEY = "HostedOverrideErrorCode"
private const val REPORT_RECIPIENT = "me@grantbrooks.io"
private const val SHORT_DATE_HASH_LENGTH = 5
private const val SHORTHAND_WORD_LIMIT = 3
private const val SUCCESS_TOAST_SECONDS = 3L
private const val TIMESTAMP_FORMAT = "H:mm:ss.SSSS"
private val RESERVED_USER_INFO_KEYS = setOf("Descriptor", "ErrorCode", "HostedOverrideErrorCode", "StaticErrorCode")
private val SHORTHAND_EXCLUDED_WORDS = setOf("A", "AN", "BEEN", "HAS", "IS", "THE", "WAS")
