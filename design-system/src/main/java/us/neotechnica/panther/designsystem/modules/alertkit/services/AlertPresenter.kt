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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.AttributedStringConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
        val messageAttributes: AttributedStringConfig? = null,
        val titleAttributes: AttributedStringConfig? = null,
        val onSelect: (Int) -> Unit,
    ) : PresentedAlert

    /** A two-button confirmation alert resolving to a Boolean. */
    data class Confirmation(
        val title: String?,
        val message: String,
        val cancelAction: Action,
        val confirmAction: Action,
        val messageAttributes: AttributedStringConfig? = null,
        val titleAttributes: AttributedStringConfig? = null,
        val onResult: (Boolean) -> Unit,
    ) : PresentedAlert

    /** An error alert with an optional send-report action. */
    data class ErrorContent(
        val title: String?,
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

    /** A non-dismissable determinate progress bar with an optional cancel action. */
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
 * Only one alert is presented at a time; presenting another alert
 * queues it until the current one dismisses and no modal display
 * blocks user interaction.
 */
object AlertPresenter {
    // MARK: - Properties

    private val mutableActionEnabledOverrides = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    private val mutableCurrent = MutableStateFlow<PresentedAlert?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var onDisplaced: (() -> Unit)? = null

    // MARK: - Computed Properties

    /** The alert currently requested for presentation, or `null`. */
    val current: StateFlow<PresentedAlert?> = mutableCurrent.asStateFlow()

    internal val actionEnabledOverrides: StateFlow<Map<Int, Boolean>> = mutableActionEnabledOverrides.asStateFlow()

    // MARK: - Methods

    /**
     * Requests presentation of the given alert, queueing it while
     * another alert is presented or a modal display blocks user
     * interaction.
     *
     * @param alert The alert to present.
     * @param onDisplaced A closure invoked if the alert is dismissed
     *   without one of its actions being selected.
     */
    fun present(
        alert: PresentedAlert,
        onDisplaced: (() -> Unit)? = null,
    ) {
        scope.launch {
            while (HUD.isBlockingUserInteraction || mutableCurrent.value != null) {
                delay(RETRY_INTERVAL_MILLISECONDS.milliseconds)
            }

            HUD.hide(after = Duration.ZERO)
            this@AlertPresenter.onDisplaced = onDisplaced
            mutableActionEnabledOverrides.value = emptyMap()
            mutableCurrent.value = alert
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

    internal fun setActionEnabled(
        index: Int,
        isEnabled: Boolean,
    ) {
        mutableActionEnabledOverrides.value = mutableActionEnabledOverrides.value + (index to isEnabled)
    }
}

private const val RETRY_INTERVAL_MILLISECONDS = 100L
