//
//  AKReportDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.ErrorAlert
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.TranslationInput
import java.util.Date
import us.neotechnica.panther.designsystem.modules.alertkit.interfaces.ReportDelegate as AlertKitReportDelegate

/**
 * A service that composes and sends error reports, bug reports,
 * and user feedback via e-mail.
 *
 * [ReportDelegate] conforms to the AlertKit report delegate
 * interface and serves as the bridge between the subsystem's
 * error-handling infrastructure and the device's mail app. When
 * the user triggers a "Send Feedback" or "Report Bug" action –
 * or when an error report is filed before the app registers its
 * own report delegate – this delegate assembles an e-mail with
 * diagnostic attachments and hands it to the mail app.
 *
 * Call [registerWithDependencies] during app setup to install the
 * delegate with AlertKit. You can also access the delegate through
 * the dependency system:
 *
 * ```kotlin
 * private val reportDelegate: ReportDelegate by Dependency { it.reportDelegate }
 * ```
 *
 * Each composed message may include:
 *
 * - **Logger session record** – the full log output for the
 *   current session.
 * - **Metadata** – a JSON file containing the build SKU, bundle
 *   version, device model, OS version, connection status, and
 *   (when applicable) error details.
 *
 * **Note:** If the device has no app that can send e-mail, the
 * delegate logs the issue and presents an error alert instead of
 * composing a message.
 */
object ReportDelegate : AlertKitReportDelegate {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Computed Properties

    private val bundleVersionString: String
        get() = "${if (Build.milestone == Build.Milestone.GENERAL_RELEASE) Build.finalName else Build.codeName} (${Build.bundleVersion})"

    private val loggerSessionRecordAttachment: MailComposer.AttachmentData?
        get() {
            val loggerSessionRecordFile = Logger.sessionRecordFilePath ?: return null
            val loggerSessionRecordData = runCatching { loggerSessionRecordFile.readBytes() }.getOrNull() ?: return null
            return MailComposer.AttachmentData(
                loggerSessionRecordData,
                fileName = "logger_session_${loggerSessionRecordFile.nameWithoutExtension}.txt",
                mimeType = "text/plain",
            )
        }

    // MARK: - Register with Dependencies

    /**
     * Registers the report delegate with AlertKit's configuration.
     *
     * Call this method once during app setup so that AlertKit can
     * present error-report prompts through this delegate.
     */
    fun registerWithDependencies() {
        AlertKitConfig.registerReportDelegate(this)
    }

    // MARK: - AlertKit ReportDelegate Conformance

    /**
     * Composes and presents an error report for the given
     * exception.
     *
     * The report includes the exception's description, code, and
     * any associated user info, along with the standard diagnostic
     * attachments.
     *
     * @param exception The exception to report.
     */
    override fun fileReport(exception: Exception) {
        scope.launch {
            composeMessage(
                subject = "$bundleVersionString Error Report",
                body = null,
                prompt = null,
                exception = exception,
            )
        }
    }

    // MARK: - Report Bug

    /**
     * Composes and presents a bug report.
     *
     * The message body includes a prompt asking the user to
     * describe the issue and the steps to reproduce it.
     */
    fun reportBug() {
        scope.launch {
            composeMessage(
                subject = "$bundleVersionString Bug Report",
                body = "In the appropriate section, please describe the error encountered and the steps to reproduce it.",
                prompt = "Description/Steps to Reproduce",
                exception = null,
            )
        }
    }

    // MARK: - Send Feedback

    /**
     * Composes and presents a general feedback message.
     *
     * The message body includes a prompt inviting the user to
     * share general feedback.
     */
    fun sendFeedback() {
        scope.launch {
            composeMessage(
                subject = "$bundleVersionString Feedback Report",
                body = "Any general feedback is appreciated in the appropriate section.",
                prompt = "General Feedback",
                exception = null,
            )
        }
    }

    // MARK: - Auxiliary

    private fun compose(
        subject: String,
        body: String?,
        prompt: String?,
        exception: Exception?,
    ) {
        var bodyPair: Pair<String, Boolean>? = null
        if (body != null && prompt != null) {
            val sentences = body.split(".").filter { it.isNotEmpty() }
            var bodyHTML = "<i>${sentences[0]}.</i><p></p><b>$prompt:</b><p></p>"
            if (sentences.size > 1) {
                bodyHTML = "<i>${sentences[0]}.<p></p>${sentences[1]}.</i><p></p>"
            }

            bodyPair = bodyHTML to true
        }

        val attachments = mutableListOf<MailComposer.AttachmentData>()
        loggerSessionRecordAttachment?.let { attachments.add(it) }
        reportMetadataAttachment(exception)?.let { attachments.add(it) }

        MailComposer.compose(
            subject = subject,
            body = bodyPair,
            recipients = listOf("me@grantbrooks.io"),
            attachments = attachments,
        )
    }

    private suspend fun composeMessage(
        subject: String,
        body: String?,
        prompt: String?,
        exception: Exception?,
    ) {
        if (!MailComposer.canSendMail) {
            val noEmailException =
                Exception(
                    "Device is not configured for e-mail.",
                    isReportable = false,
                    userInfo =
                        mapOf(
                            Exception.UserInfo.USER_FACING_DESCRIPTOR.rawValue to
                                Localized(SubsystemStringKey.NO_EMAIL).wrappedValue,
                        ),
                    metadata = ExceptionMetadata(this),
                )

            Logger.log(noEmailException)
            ErrorAlert(
                noEmailException,
                dismissButtonTitle = Localized(SubsystemStringKey.DISMISS).wrappedValue,
            ).present(translating = emptyList())
            return
        }

        if (body == null || prompt == null) return compose(subject, null, null, exception)
        val translationDelegate = AlertKitConfig.translationDelegate ?: return

        try {
            val translations =
                translationDelegate.getTranslations(
                    inputs =
                        listOf(
                            TranslationInput(body),
                            TranslationInput(prompt),
                        ),
                    languagePair =
                        LanguagePair(
                            from = "en",
                            to = RuntimeStorage.languageCode,
                        ),
                    hudConfig = AlertKitConfig.translationHUDConfig,
                    timeoutConfig = AlertKitConfig.translationTimeoutConfig,
                )

            compose(
                subject = subject,
                body = translations.firstOrNull { it.input.value == body }?.output ?: body,
                prompt = translations.firstOrNull { it.input.value == prompt }?.output ?: prompt,
                exception = exception,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Logger.log(Exception.from(throwable, ExceptionMetadata(this)))
            compose(subject, body, prompt, exception)
        }
    }

    private fun reportMetadataAttachment(exception: Exception? = null): MailComposer.AttachmentData? {
        val sections =
            mutableMapOf(
                "build_sku" to Build.buildSKU,
                "bundle_revision" to "${Build.bundleRevision} (${Build.revisionBuildNumber})",
                "bundle_version" to "${Build.bundleVersion} (${Build.buildNumber}${Build.milestone.shortString})",
                "connection_status" to if (Build.isOnline) "online" else "offline",
                "device_model" to "${SystemInformation.modelName} (${SystemInformation.modelCode.lowercase()})",
                "language_code" to RuntimeStorage.languageCode,
                "os_version" to SystemInformation.osVersion.lowercase(),
                "project_id" to Build.projectID,
                "timestamp" to DependencyValues.current.timestampDateFormatter.format(Date()),
            )

        if (exception != null) {
            val descriptor = exception.userInfo?.get(Exception.UserInfo.DESCRIPTOR.rawValue) as? String
            val code = exception.userInfo?.get(Exception.UserInfo.ERROR_CODE.rawValue) as? String

            sections["error_description"] =
                if (descriptor != null && code != null) "$descriptor (${code.uppercase()})" else exception.descriptor
            sections["error_id"] = exception.id

            val additionalParameters =
                exception.userInfo
                    ?.filterKeys { it != Exception.UserInfo.DESCRIPTOR.rawValue && it != Exception.UserInfo.ERROR_CODE.rawValue }
                    .orEmpty()

            if (additionalParameters.isNotEmpty()) {
                sections["error_parameters"] =
                    additionalParameters
                        .mapKeys { (key, _) -> key.replaceFirstChar { it.uppercase() } }
                        .entries
                        .joinToString(prefix = "[", postfix = "]") { (key, value) -> "'$key': '$value'" }
            }
        }

        return runCatching {
            MailComposer.AttachmentData(
                JSONObject(sections.toMap()).toString().toByteArray(Charsets.UTF_8),
                fileName = "metadata.log",
                mimeType = "application/json",
            )
        }.onFailure {
            Logger.log(Exception.from(it, ExceptionMetadata(this)))
        }.getOrNull()
    }
}
