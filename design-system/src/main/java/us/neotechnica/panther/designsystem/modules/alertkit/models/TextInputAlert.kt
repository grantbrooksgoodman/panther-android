//
//  TextInputAlert.kt
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
 * An alert that prompts the user for a single line of text.
 *
 * [present] resolves to the entered text when the user confirms, or
 * `null` when they cancel:
 *
 * ```kotlin
 * val name = TextInputAlert(
 *     message = "Enter a name for this conversation.",
 *     placeholder = "Name",
 * ).present()
 * ```
 *
 * **Note:** translation of alert content is deferred to the translation
 * phase.
 */
class TextInputAlert(
    private val title: String? = null,
    private val message: String,
    private val placeholder: String = "",
    private val initialText: String = "",
    private val isSecure: Boolean = false,
    private val cancelButtonTitle: String = "Cancel",
    private val confirmButtonTitle: String = "Confirm",
) {
    // MARK: - Methods

    /**
     * Presents the alert and suspends until the user confirms or
     * cancels.
     *
     * @return The entered text on confirmation, or `null` on cancel.
     */
    suspend fun present(): String? =
        suspendCancellableCoroutine { continuation ->
            AlertPresenter.present(
                PresentedAlert.TextInput(
                    title = title,
                    message = message,
                    placeholder = placeholder,
                    initialText = initialText,
                    isSecure = isSecure,
                    cancelButtonTitle = cancelButtonTitle,
                    confirmButtonTitle = confirmButtonTitle,
                ) { result ->
                    AlertPresenter.dismiss()
                    if (continuation.isActive) continuation.resume(result)
                },
            )

            continuation.invokeOnCancellation { AlertPresenter.dismiss() }
        }
}
