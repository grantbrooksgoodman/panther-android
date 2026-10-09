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
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ErrorAlert
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.LoggerPresentationDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey
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
 * Error alerts and informational alerts route through AlertKit,
 * while lightweight feedback routes through a [Toast]. Content is
 * translated only when the exception carries a user-facing
 * descriptor other than the generic and timed-out descriptors.
 * Reportable exceptions invite the user to file a report – unless
 * the logger reports errors automatically, in which case the
 * toast states that the error has been reported.
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
        scope.launch { showAlert(alertType, exception, text) }
    }

    // MARK: - Auxiliary

    private suspend fun showAlert(
        type: AlertType,
        exception: Exception? = null,
        text: String? = null,
    ) {
        val userFacingDescriptor = exception?.userFacingDescriptor ?: text ?: return
        HUD.hide()

        val mockGenericException = Exception(metadata = ExceptionMetadata(this))
        val mockTimedOutException = Exception.timedOut(ExceptionMetadata(this))
        val notGenericDescriptor = userFacingDescriptor != mockGenericException.userFacingDescriptor
        val notTimedOutDescriptor = userFacingDescriptor != mockTimedOutException.userFacingDescriptor
        val hasUserFacingDescriptor = exception?.descriptor != exception?.userFacingDescriptor

        val shouldTranslate = hasUserFacingDescriptor && notGenericDescriptor && notTimedOutDescriptor

        when (type) {
            AlertType.ErrorAlert -> {
                if (exception == null) return showAlert(AlertType.NormalAlert, text = text)
                presentErrorAlert(exception, shouldTranslate = shouldTranslate)
            }

            AlertType.NormalAlert -> {
                val alert = Alert(message = userFacingDescriptor)
                if (shouldTranslate) return alert.present(translating = listOf(Alert.TranslationOptionKey.Message))
                alert.present(translating = emptyList())
            }

            is AlertType.Toast ->
                Toast.show(
                    toast(
                        type,
                        exception = exception,
                        userFacingDescriptor = userFacingDescriptor,
                    ),
                    translating =
                        if (shouldTranslate) {
                            listOf(
                                Toast.TranslationOptionKey.Message,
                                Toast.TranslationOptionKey.Title,
                            )
                        } else {
                            emptyList()
                        },
                    onTap = reportAction(exception),
                )
        }
    }

    private suspend fun presentErrorAlert(
        exception: Exception,
        shouldTranslate: Boolean,
    ) {
        val errorAlert =
            ErrorAlert(
                exception.hydrated,
                dismissButtonTitle = Localized(SubsystemStringKey.DISMISS).wrappedValue,
            )

        val translationOptionKeys = mutableListOf<ErrorAlert.TranslationOptionKey>()
        if (shouldTranslate) translationOptionKeys.add(ErrorAlert.TranslationOptionKey.ErrorDescription)
        if (exception.isReportable && !Logger.reportsErrorsAutomatically) {
            translationOptionKeys.add(ErrorAlert.TranslationOptionKey.SendErrorReportButtonTitle)
        }

        errorAlert.present(translating = translationOptionKeys)
    }

    internal fun toast(
        type: AlertType.Toast,
        exception: Exception?,
        userFacingDescriptor: String,
    ): Toast {
        val style = type.style ?: if (exception == null) ToastStyle.INFO else ToastStyle.ERROR

        var title: String? = null
        var message: String? = null

        if (exception != null && exception.isReportable) {
            title = userFacingDescriptor
            message =
                if (Logger.reportsErrorsAutomatically) {
                    Localized(SubsystemStringKey.ERROR_REPORTED).wrappedValue
                } else {
                    Localized(SubsystemStringKey.TAP_TO_REPORT).wrappedValue
                }
        }

        return Toast(
            if (type.isPersistent) Toast.ToastType.Banner(style) else Toast.ToastType.Capsule(style),
            title = title,
            message = message ?: userFacingDescriptor,
            perpetuation =
                if (type.isPersistent) {
                    Toast.PerpetuationStrategy.Persistent
                } else {
                    Toast.PerpetuationStrategy.Ephemeral(TOAST_EPHEMERAL_DURATION_SECONDS.seconds)
                },
        )
    }

    internal fun reportAction(exception: Exception?): (() -> Unit)? {
        if (exception == null ||
            !exception.isReportable ||
            Logger.reportsErrorsAutomatically
        ) {
            return null
        }

        return { AlertKitConfig.reportDelegate?.fileReport(exception.hydrated) }
    }
}

private const val TOAST_EPHEMERAL_DURATION_SECONDS = 10L
