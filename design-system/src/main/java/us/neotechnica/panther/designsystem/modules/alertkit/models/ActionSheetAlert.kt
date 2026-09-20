//
//  ActionSheetAlert.kt
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
 * A bottom action sheet, standing in for the iOS `AKActionSheet`.
 *
 * The sheet has two forms:
 *
 * - A binary confirm/cancel sheet, created with a [confirmButtonTitle].
 *   [present] resolves to `true` when the user confirms and `false` when
 *   they cancel.
 * - A multi-action sheet, created with a list of [Action]s (the iOS
 *   `AKActionSheet(title:actions:cancelButtonTitle:)`). [present] runs
 *   the selected action's effect and resolves to `true`; cancelling
 *   resolves to `false`.
 *
 * **Note:** translation of sheet content is deferred to the reducers
 * that present it, which resolve their strings beforehand.
 */
class ActionSheetAlert private constructor(
    private val title: String?,
    private val message: String?,
    private val confirmButtonTitle: String?,
    private val cancelButtonTitle: String,
    private val isDestructive: Boolean,
    private val actions: List<Action>?,
) {
    // MARK: - Init

    /** Creates a binary confirm/cancel action sheet. */
    constructor(
        title: String? = null,
        message: String? = null,
        confirmButtonTitle: String,
        cancelButtonTitle: String = "Cancel",
        isDestructive: Boolean = false,
    ) : this(title, message, confirmButtonTitle, cancelButtonTitle, isDestructive, null)

    /** Creates a multi-action action sheet offering [actions] and a cancel button. */
    constructor(
        title: String? = null,
        message: String? = null,
        actions: List<Action>,
        cancelButtonTitle: String = "Cancel",
    ) : this(title, message, null, cancelButtonTitle, false, actions)

    // MARK: - Methods

    /**
     * Presents the sheet and suspends until the user makes a choice.
     *
     * @return `true` if the user confirms or selects an action;
     *   otherwise, `false`.
     */
    suspend fun present(): Boolean = presentActions(actions ?: listOf(confirmAction()))

    // MARK: - Auxiliary

    /** The single action a binary confirm/cancel sheet presents. */
    private fun confirmAction(): Action =
        Action(
            title = confirmButtonTitle.orEmpty(),
            style = if (isDestructive) ActionStyle.DESTRUCTIVE else ActionStyle.DEFAULT,
        ) {}

    private suspend fun presentActions(actions: List<Action>): Boolean =
        suspendCancellableCoroutine { continuation ->
            AlertPresenter.present(
                PresentedAlert.ActionSheet(
                    title = title,
                    message = message,
                    actions = actions,
                    cancelButtonTitle = cancelButtonTitle,
                    onSelect = { index ->
                        AlertPresenter.dismiss()
                        actions.getOrNull(index)?.effect?.invoke()
                        if (continuation.isActive) continuation.resume(true)
                    },
                    onCancel = {
                        AlertPresenter.dismiss()
                        if (continuation.isActive) continuation.resume(false)
                    },
                ),
            )

            continuation.invokeOnCancellation { AlertPresenter.dismiss() }
        }
}
