//
//  ConfirmationAlert.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.suspendCancellableCoroutine
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import kotlin.coroutines.resume

/**
 * An alert that asks the user to confirm or cancel an action.
 *
 * [present] resolves to `true` when the user taps the confirm button
 * and `false` when they cancel:
 *
 * ```kotlin
 * val confirmed = ConfirmationAlert(
 *     title = "Remove Item",
 *     message = "This action cannot be undone.",
 * ).present()
 *
 * if (confirmed) removeItem()
 * ```
 *
 * **Note:** translation of alert content is deferred to the translation
 * phase.
 */
class ConfirmationAlert(
    private val title: String? = null,
    private val message: String,
    private val cancelButtonTitle: String = "Cancel",
    private val cancelButtonStyle: ActionStyle = ActionStyle.CANCEL,
    private val confirmButtonTitle: String = "Confirm",
    private val confirmButtonStyle: ActionStyle = ActionStyle.PREFERRED,
) {
    // MARK: - Methods

    /**
     * Presents the alert and suspends until the user makes a choice.
     *
     * @return `true` if the user confirms; otherwise, `false`.
     */
    suspend fun present(): Boolean =
        suspendCancellableCoroutine { continuation ->
            AlertPresenter.present(
                PresentedAlert.Confirmation(
                    title = title,
                    message = message,
                    cancelAction = Action(cancelButtonTitle, style = cancelButtonStyle) {},
                    confirmAction = Action(confirmButtonTitle, style = confirmButtonStyle) {},
                ) { result ->
                    AlertPresenter.dismiss()
                    if (continuation.isActive) continuation.resume(result)
                },
            )

            continuation.invokeOnCancellation { AlertPresenter.dismiss() }
        }
}
