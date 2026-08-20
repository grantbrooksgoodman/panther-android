//
//  ErrorAlert.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.suspendCancellableCoroutine
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import kotlin.coroutines.resume

/**
 * An alert that reports an error to the user.
 *
 * The alert displays the exception's user-facing descriptor and a
 * dismiss button. When the exception is reportable, a send-report
 * button is shown as well.
 *
 * ```kotlin
 * ErrorAlert(exception).present()
 * ```
 *
 * **Note:** filing the report and translating content are deferred to
 * later phases; the send-report action currently invokes the provided
 * callback only.
 */
class ErrorAlert(
    private val exception: Exception,
    private val dismissButtonTitle: String = "Dismiss",
    private val sendReportButtonTitle: String = "Send Error Report",
    private val onSendReport: (() -> Unit)? = null,
) {
    // MARK: - Methods

    /**
     * Presents the error alert and suspends until the user dismisses it.
     */
    suspend fun present(): Unit =
        suspendCancellableCoroutine { continuation ->
            AlertPresenter.present(
                PresentedAlert.ErrorContent(
                    title = "Error",
                    message = exception.descriptor,
                    dismissButtonTitle = dismissButtonTitle,
                    sendReportButtonTitle = if (exception.isReportable) sendReportButtonTitle else null,
                    onDismiss = {
                        AlertPresenter.dismiss()
                        if (continuation.isActive) continuation.resume(Unit)
                    },
                    onSendReport =
                        if (exception.isReportable) {
                            {
                                AlertPresenter.dismiss()
                                onSendReport?.invoke()
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                        } else {
                            null
                        },
                ),
            )

            continuation.invokeOnCancellation { AlertPresenter.dismiss() }
        }
}
