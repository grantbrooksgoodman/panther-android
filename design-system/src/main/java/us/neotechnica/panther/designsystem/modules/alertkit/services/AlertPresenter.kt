//
//  AlertPresenter.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.AttributedStringConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.ContinuationGuard
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.designsystem.modules.foundation.services.UserInteractionBlocker
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A description of the alert currently requested for
 * presentation.
 *
 * The imperative `present()` methods on the alert types set the
 * current value; the
 * [AlertHost][us.neotechnica.panther.designsystem.modules.alertkit.views.AlertHost]
 * composable renders it.
 */
sealed interface PresentedAlert {
    /**
     * A standard alert with a title, message, and a list of actions.
     *
     * @property title The alert's title, or `null` for none.
     * @property message The alert's message, or `null` for none.
     * @property actions The actions the alert offers, in display
     *   order.
     * @property messageAttributes The attributes applied to the
     *   message, or `null` for plain text.
     * @property titleAttributes The attributes applied to the title,
     *   or `null` for plain text.
     * @property onSelect The action performed with the index of the
     *   chosen action.
     */
    data class Standard(
        val title: String?,
        val message: String?,
        val actions: List<Action>,
        val messageAttributes: AttributedStringConfig? = null,
        val titleAttributes: AttributedStringConfig? = null,
        val onSelect: (Int) -> Unit,
    ) : PresentedAlert

    /**
     * A two-button confirmation alert resolving to a Boolean.
     *
     * @property title The alert's title, or `null` for none.
     * @property message The alert's message.
     * @property cancelAction The action that declines.
     * @property confirmAction The action that confirms.
     * @property messageAttributes The attributes applied to the
     *   message, or `null` for plain text.
     * @property titleAttributes The attributes applied to the title,
     *   or `null` for plain text.
     * @property onResult The action performed with `true` if the user
     *   confirmed; otherwise, `false`.
     */
    data class Confirmation(
        val title: String?,
        val message: String,
        val cancelAction: Action,
        val confirmAction: Action,
        val messageAttributes: AttributedStringConfig? = null,
        val titleAttributes: AttributedStringConfig? = null,
        val onResult: (Boolean) -> Unit,
    ) : PresentedAlert

    /**
     * An error alert with an optional send-report action.
     *
     * @property title The alert's title, or `null` for none.
     * @property message The alert's message.
     * @property dismissButtonTitle The dismiss button's title.
     * @property sendReportButtonTitle The send-report button's title,
     *   or `null` to omit the button.
     * @property onDismiss The action performed when the user
     *   dismisses the alert.
     * @property onSendReport The action performed when the user sends
     *   a report, or `null` when none is offered.
     */
    data class ErrorContent(
        val title: String?,
        val message: String,
        val dismissButtonTitle: String,
        val sendReportButtonTitle: String?,
        val onDismiss: () -> Unit,
        val onSendReport: (() -> Unit)?,
    ) : PresentedAlert

    /**
     * A single-field text-input alert resolving to the entered text
     * or `null`.
     *
     * @property title The alert's title, or `null` for none.
     * @property message The alert's message.
     * @property attributes The text field's configuration.
     * @property cancelButtonTitle The cancel button's title.
     * @property cancelButtonStyle The cancel button's style.
     * @property confirmButtonTitle The confirm button's title.
     * @property confirmButtonStyle The confirm button's style.
     * @property messageAttributes The attributes applied to the
     *   message, or `null` for plain text.
     * @property titleAttributes The attributes applied to the title,
     *   or `null` for plain text.
     * @property onTextFieldChange The action performed with the
     *   field's text as it changes, or `null` for none.
     * @property onResult The action performed with the entered text,
     *   or `null` if the user cancelled.
     */
    data class TextInput(
        val title: String?,
        val message: String,
        val attributes: TextFieldAttributes,
        val cancelButtonTitle: String,
        val cancelButtonStyle: ActionStyle,
        val confirmButtonTitle: String,
        val confirmButtonStyle: ActionStyle,
        val messageAttributes: AttributedStringConfig? = null,
        val titleAttributes: AttributedStringConfig? = null,
        val onTextFieldChange: ((String?) -> Unit)?,
        val onResult: (String?) -> Unit,
    ) : PresentedAlert

    /**
     * A bottom action sheet offering a list of actions and a cancel
     * button.
     *
     * @property title The sheet's title, or `null` for none.
     * @property message The sheet's message, or `null` for none.
     * @property actions The actions the sheet offers, in display
     *   order.
     * @property cancelButtonTitle The cancel button's title.
     * @property messageAttributes The attributes applied to the
     *   message, or `null` for plain text.
     * @property titleAttributes The attributes applied to the title,
     *   or `null` for plain text.
     * @property onSelect The action performed with the index of the
     *   chosen action.
     * @property onCancel The action performed when the user cancels.
     */
    data class ActionSheet(
        val title: String?,
        val message: String?,
        val actions: List<Action>,
        val cancelButtonTitle: String,
        val messageAttributes: AttributedStringConfig? = null,
        val titleAttributes: AttributedStringConfig? = null,
        val onSelect: (Int) -> Unit,
        val onCancel: () -> Unit,
    ) : PresentedAlert

    /**
     * A non-dismissable determinate progress bar with an optional
     * cancel action.
     *
     * @property title The alert's title, or `null` for none.
     * @property message The alert's message.
     * @property cancelButtonTitle The cancel button's title, or
     *   `null` to omit the button.
     * @property cancelButtonStyle The cancel button's style.
     * @property progress The progress to display, from `0` to `1`.
     * @property messageAttributes The attributes applied to the
     *   message, or `null` for plain text.
     * @property titleAttributes The attributes applied to the
     *   title, or `null` for plain text.
     * @property onCancel The action performed when the user
     *   cancels, or `null` when cancellation is not offered.
     */
    data class Progress(
        val title: String?,
        val message: String,
        val cancelButtonTitle: String?,
        val cancelButtonStyle: ActionStyle,
        val progress: StateFlow<Double>,
        val messageAttributes: AttributedStringConfig? = null,
        val titleAttributes: AttributedStringConfig? = null,
        val onCancel: (() -> Unit)?,
    ) : PresentedAlert
}

/**
 * The single source of truth for the alert currently being presented.
 *
 * Only one alert is presented at a time. Presenting another alert
 * queues it; queued alerts are presented in the order they were
 * requested, each once the current alert dismisses and no modal
 * display blocks user interaction.
 */
object AlertPresenter {
    // MARK: - Types

    private class PendingPresentation(
        val alert: PresentedAlert,
        val onDisplaced: (() -> Unit)?,
    )

    // MARK: - Properties

    private val mutableActionEnabledOverrides = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    private val mutableCurrent = MutableStateFlow<PresentedAlert?>(null)
    private val pendingPresentations = ArrayDeque<PendingPresentation>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var onDisplaced: (() -> Unit)? = null
    private var queueJob: Job? = null

    // MARK: - Computed Properties

    /** The alert currently requested for presentation, or `null`. */
    val current: StateFlow<PresentedAlert?> = mutableCurrent.asStateFlow()

    /**
     * A Boolean value that indicates whether an alert is
     * presented.
     */
    val isPresenting: Boolean
        get() = mutableCurrent.value != null

    internal val actionEnabledOverrides: StateFlow<Map<Int, Boolean>> = mutableActionEnabledOverrides.asStateFlow()

    // MARK: - Methods

    /**
     * Requests presentation of the given alert, queueing it
     * behind any alert already presented or queued.
     *
     * @param alert The alert to present.
     * @param onDisplaced A closure invoked if the alert is
     *   dismissed without one of its actions being selected.
     */
    fun present(
        alert: PresentedAlert,
        onDisplaced: (() -> Unit)? = null,
    ) {
        scope.launch {
            pendingPresentations.addLast(PendingPresentation(alert, onDisplaced))
            presentQueuedAlerts()
        }
    }

    /** Dismisses the current alert, if any. */
    fun dismiss() {
        mutableActionEnabledOverrides.value = emptyMap()
        mutableCurrent.value = null
        val displaced = onDisplaced
        onDisplaced = null
        displaced?.invoke()
    }

    /**
     * Withdraws the given alert: removes it from the queue if it has
     * not yet been presented, or dismisses it if it is presented.
     *
     * @param alert The alert to withdraw.
     */
    fun withdraw(alert: PresentedAlert) {
        scope.launch {
            if (mutableCurrent.value === alert) {
                dismiss()
            } else {
                pendingPresentations.removeAll { it.alert === alert }
            }
        }
    }

    /**
     * Presents an alert and suspends until the alert resumes the
     * continuation guard passed to [alert]: with a value from one of
     * its actions, or with [fallbackValue] if it is displaced.
     * Cancelling the coroutine withdraws the alert.
     *
     * @param fallbackValue The value returned if the alert is
     *   displaced.
     * @param alert Builds the alert to present, resuming the given
     *   guard from the alert's actions.
     *
     * @return The value the guard was resumed with.
     */
    internal suspend fun <T> presentAwaitingResult(
        fallbackValue: T,
        alert: (ContinuationGuard<T>) -> PresentedAlert,
    ): T =
        suspendCancellableCoroutine { continuation ->
            val guard = ContinuationGuard(continuation, fallbackValue = fallbackValue)
            val presentedAlert = alert(guard)
            present(presentedAlert, onDisplaced = { guard.fallback() })
            continuation.invokeOnCancellation { withdraw(presentedAlert) }
        }

    internal fun setActionEnabled(
        index: Int,
        isEnabled: Boolean,
    ) {
        mutableActionEnabledOverrides.value = mutableActionEnabledOverrides.value + (index to isEnabled)
    }

    // MARK: - Auxiliary

    // Presents queued alerts in order, one at a time, waiting while an
    // alert is presented or a modal display blocks user interaction.
    private fun presentQueuedAlerts() {
        if (queueJob?.isActive == true) return
        queueJob =
            scope.launch {
                while (pendingPresentations.isNotEmpty()) {
                    if (UserInteractionBlocker.isBlockingUserInteraction || mutableCurrent.value != null) {
                        delay(RETRY_INTERVAL_MILLISECONDS.milliseconds)
                        continue
                    }

                    val pendingPresentation = pendingPresentations.removeFirst()
                    HUD.hide(after = Duration.ZERO)
                    onDisplaced = pendingPresentation.onDisplaced
                    mutableActionEnabledOverrides.value = emptyMap()
                    mutableCurrent.value = pendingPresentation.alert
                }
            }
    }
}

private const val RETRY_INTERVAL_MILLISECONDS = 100L
