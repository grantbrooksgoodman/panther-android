//
//  LoggerPresentationService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ErrorAlert
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.LoggerPresentationDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import kotlin.time.Duration.Companion.seconds

/**
 * Presents the logger's user-visible alerts through the design
 * system.
 *
 * The subsystem's [Logger] cannot reach the design system's alert
 * and toast components directly, so it forwards presentation
 * requests to this delegate. Register the service once at launch
 * with `Logger.setPresentationDelegate(LoggerPresentationService)`.
 *
 * Presents logger output: error alerts and informational
 * alerts route through AlertKit, while lightweight feedback routes
 * through a [Toast].
 */
object LoggerPresentationService : LoggerPresentationDelegate {
    // MARK: - Properties

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // MARK: - LoggerPresentationDelegate Conformance

    override fun present(
        alertType: AlertType,
        exception: Exception?,
        text: String?,
    ) {
        when (alertType) {
            AlertType.ErrorAlert -> presentErrorAlert(exception, text)
            AlertType.NormalAlert -> presentNormalAlert(exception, text)
            is AlertType.Toast -> presentToast(alertType, exception, text)
        }
    }

    // MARK: - Auxiliary

    private fun presentErrorAlert(
        exception: Exception?,
        text: String?,
    ) {
        val exception = exception ?: return presentNormalAlert(null, text)
        scope.launch {
            val mockGenericException = Exception(metadata = ExceptionMetadata(this@LoggerPresentationService))
            val mockTimedOutException = Exception("The operation timed out.", metadata = ExceptionMetadata(this@LoggerPresentationService))

            val hasUserFacingDescriptor = exception.descriptor != exception.userFacingDescriptor
            val notGenericDescriptor = exception.userFacingDescriptor != mockGenericException.userFacingDescriptor
            val notTimedOutDescriptor = exception.userFacingDescriptor != mockTimedOutException.userFacingDescriptor
            val shouldTranslate = hasUserFacingDescriptor && notGenericDescriptor && notTimedOutDescriptor

            val translationOptionKeys = mutableListOf<ErrorAlert.TranslationOptionKey>()
            if (shouldTranslate) translationOptionKeys.add(ErrorAlert.TranslationOptionKey.ErrorDescription)
            if (exception.isReportable && !Logger.reportsErrorsAutomatically) {
                translationOptionKeys.add(ErrorAlert.TranslationOptionKey.SendErrorReportButtonTitle)
            }

            ErrorAlert(
                exception = exception,
                dismissButtonTitle = LocalizedStringKey.Dismiss.localized(),
            ).present(translating = translationOptionKeys)
        }
    }

    private fun presentNormalAlert(
        exception: Exception?,
        text: String?,
    ) {
        val message = exception?.userFacingDescriptor ?: text ?: return
        scope.launch {
            Alert(message = message).present(
                translating = listOf(Alert.TranslationOptionKey.Actions(), Alert.TranslationOptionKey.Message),
            )
        }
    }

    private fun presentToast(
        alertType: AlertType.Toast,
        exception: Exception?,
        text: String?,
    ) {
        val descriptor = exception?.userFacingDescriptor ?: text ?: return
        val style = alertType.style ?: if (exception == null) ToastStyle.INFO else ToastStyle.ERROR

        val type =
            if (alertType.isPersistent) {
                Toast.ToastType.Banner(style)
            } else {
                Toast.ToastType.Capsule(style)
            }

        val perpetuation =
            if (alertType.isPersistent) {
                Toast.PerpetuationStrategy.Persistent
            } else {
                Toast.PerpetuationStrategy.Ephemeral(TOAST_EPHEMERAL_DURATION_SECONDS.seconds)
            }

        // Reportable exceptions invite the user to file a report by tapping.
        val reportableException = exception?.takeIf { it.isReportable }
        scope.launch {
            Toast.show(
                Toast(
                    type,
                    title = reportableException?.let { descriptor },
                    message = if (reportableException != null) "Tap to report" else descriptor,
                    perpetuation = perpetuation,
                ),
                translating = listOf(Toast.TranslationOptionKey.Message, Toast.TranslationOptionKey.Title),
                onTap = reportableException?.let { ex -> { ErrorReportingService.fileReport(ex) } },
            )
        }
    }
}

private const val TOAST_EPHEMERAL_DURATION_SECONDS = 10L
