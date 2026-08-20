//
//  AlertPresenter.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action

/**
 * A description of the alert currently requested for presentation.
 *
 * The imperative `present()` methods on the alert types set the current
 * value; the
 * [AlertHost][us.neotechnica.panther.designsystem.modules.alertkit.views.AlertHost]
 * composable renders it.
 */
sealed interface PresentedAlert {
    /** A standard alert with a title, message, and a list of actions. */
    data class Standard(
        val title: String?,
        val message: String?,
        val actions: List<Action>,
        val onSelect: (Int) -> Unit,
    ) : PresentedAlert

    /** A two-button confirmation alert resolving to a Boolean. */
    data class Confirmation(
        val title: String?,
        val message: String,
        val cancelAction: Action,
        val confirmAction: Action,
        val onResult: (Boolean) -> Unit,
    ) : PresentedAlert

    /** An error alert with an optional send-report action. */
    data class ErrorContent(
        val title: String,
        val message: String,
        val dismissButtonTitle: String,
        val sendReportButtonTitle: String?,
        val onDismiss: () -> Unit,
        val onSendReport: (() -> Unit)?,
    ) : PresentedAlert

    /** A single-field text-input alert resolving to the entered text or `null`. */
    data class TextInput(
        val title: String?,
        val message: String,
        val placeholder: String,
        val initialText: String,
        val isSecure: Boolean,
        val cancelButtonTitle: String,
        val confirmButtonTitle: String,
        val onResult: (String?) -> Unit,
    ) : PresentedAlert

    /** A non-dismissable progress indicator with an optional cancel action. */
    data class Progress(
        val title: String?,
        val message: String,
        val cancelButtonTitle: String?,
        val onCancel: (() -> Unit)?,
    ) : PresentedAlert
}

/**
 * The single source of truth for the alert currently being presented.
 *
 * Only one alert is presented at a time; presenting a new alert
 * replaces any current one.
 */
object AlertPresenter {
    // MARK: - Properties

    private val mutableCurrent = MutableStateFlow<PresentedAlert?>(null)

    // MARK: - Computed Properties

    /** The alert currently requested for presentation, or `null`. */
    val current: StateFlow<PresentedAlert?> = mutableCurrent.asStateFlow()

    // MARK: - Methods

    /** Requests presentation of the given alert. */
    fun present(alert: PresentedAlert) {
        mutableCurrent.value = alert
    }

    /** Dismisses the current alert, if any. */
    fun dismiss() {
        mutableCurrent.value = null
    }
}
