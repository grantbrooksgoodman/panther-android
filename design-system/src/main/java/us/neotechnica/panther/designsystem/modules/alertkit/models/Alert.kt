//
//  Alert.kt
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
 * An alert that displays a title, message, and a set of actions.
 *
 * Create an alert with a title, an optional message, and one or more
 * actions, then call [present] to display it and suspend until the user
 * selects an action:
 *
 * ```kotlin
 * Alert(
 *     title = "Remove Item",
 *     message = "This action cannot be undone.",
 *     actions = listOf(
 *         Action("Remove", style = ActionStyle.DESTRUCTIVE) { removeItem() },
 *         Action("Cancel", style = ActionStyle.CANCEL) {},
 *     ),
 * ).present()
 * ```
 *
 * When you omit `actions`, the alert displays a single "OK" button.
 *
 * **Note:** translation of alert content is deferred to the translation
 * phase; content is presented as provided.
 */
class Alert(
    private val title: String? = null,
    private val message: String?,
    private val actions: List<Action> = listOf(Action("OK", style = ActionStyle.CANCEL) {}),
) {
    // MARK: - Methods

    /**
     * Presents the alert and suspends until the user selects an action,
     * running that action's effect.
     */
    suspend fun present(): Unit =
        suspendCancellableCoroutine { continuation ->
            AlertPresenter.present(
                PresentedAlert.Standard(
                    title = title,
                    message = message,
                    actions = actions,
                ) { index ->
                    AlertPresenter.dismiss()
                    actions.getOrNull(index)?.effect?.invoke()
                    if (continuation.isActive) continuation.resume(Unit)
                },
            )

            continuation.invokeOnCancellation { AlertPresenter.dismiss() }
        }
}
