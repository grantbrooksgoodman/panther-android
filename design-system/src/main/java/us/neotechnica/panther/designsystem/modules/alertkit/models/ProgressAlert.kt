//
//  ProgressAlert.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert

/**
 * A non-dismissable alert that displays an activity indicator while
 * work is in progress.
 *
 * Present the alert, perform your work, then dismiss it:
 *
 * ```kotlin
 * val progress = ProgressAlert(message = "Loading…")
 * progress.present()
 * try {
 *     doWork()
 * } finally {
 *     progress.dismiss()
 * }
 * ```
 *
 * Provide a `cancelButtonTitle` to let the user abort the operation.
 */
class ProgressAlert(
    private val title: String? = null,
    private val message: String,
    private val cancelButtonTitle: String? = null,
    private val onCancel: (() -> Unit)? = null,
) {
    // MARK: - Methods

    /** Dismisses the alert. */
    fun dismiss() {
        AlertPresenter.dismiss()
    }

    /** Presents the alert. Call [dismiss] once your work completes. */
    fun present() {
        AlertPresenter.present(
            PresentedAlert.Progress(
                title = title,
                message = message,
                cancelButtonTitle = cancelButtonTitle,
                onCancel =
                    onCancel?.let { cancel ->
                        {
                            AlertPresenter.dismiss()
                            cancel()
                        }
                    },
            ),
        )
    }
}
