//
//  LoggerPresentationService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 31/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ErrorAlert
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.LoggerPresentationDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
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
 * This is the Android counterpart of the presentation performed by
 * the iOS logger through `CoreKit`: error alerts and informational
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
            ErrorAlert(exception, onSendReport = { ErrorReportingService.fileReport(exception) }).present()
        }
    }

    private fun presentNormalAlert(
        exception: Exception?,
        text: String?,
    ) {
        val message = exception?.userFacingDescriptor ?: text ?: return
        scope.launch { Alert(message = message).present() }
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
                Toast.Type.Banner(style)
            } else {
                Toast.Type.Capsule(style)
            }

        val perpetuation =
            if (alertType.isPersistent) {
                Toast.Perpetuation.Persistent
            } else {
                Toast.Perpetuation.Ephemeral(TOAST_EPHEMERAL_DURATION_SECONDS.seconds)
            }

        // Reportable exceptions invite the user to file a report by tapping.
        val reportableException = exception?.takeIf { it.isReportable }
        Toast.show(
            Toast(
                type,
                title = reportableException?.let { descriptor },
                message = if (reportableException != null) "Tap to report" else descriptor,
                perpetuation = perpetuation,
            ),
            onTap = reportableException?.let { ex -> { ErrorReportingService.fileReport(ex) } },
        )
    }
}

private const val TOAST_EPHEMERAL_DURATION_SECONDS = 10L
