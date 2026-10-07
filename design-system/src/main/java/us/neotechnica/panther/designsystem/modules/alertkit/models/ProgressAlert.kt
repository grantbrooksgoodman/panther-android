//
//  ProgressAlert.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.firstOutput
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.nonDefaultUnique
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * An alert that displays the progress of a long-running operation.
 *
 * Use [ProgressAlert] to present an alert with an embedded progress
 * bar. Provide a flow of completion fractions to drive the bar
 * automatically – the alert dismisses itself when the flow finishes
 * and rethrows any error it produces:
 *
 * ```kotlin
 * val alert = ProgressAlert(
 *     title = "Uploading Photo",
 *     message = "Your photo is being uploaded.",
 *     cancelButtonTitle = "Cancel",
 * )
 *
 * alert.present(observing = uploadProgress)
 * ```
 *
 * The observed flow is collected only after the alert is presented,
 * so the observed operation does not begin while translation or
 * presentation is still in progress.
 *
 * When you provide a cancel button title, tapping the button cancels
 * the observed operation and [present] throws a
 * `CancellationException`. Omit the title to present an alert that
 * can be dismissed only programmatically.
 *
 * You can also drive the progress bar manually. Call [present] to
 * display the alert, [updateProgress] to advance the bar, and
 * [dismiss] when your work completes:
 *
 * ```kotlin
 * val alert = ProgressAlert(message = "Processing…")
 *
 * alert.present()
 * alert.updateProgress(0.5)
 * alert.dismiss()
 * ```
 *
 * By default, both presentation methods translate the alert's title,
 * message, and cancel button title into the configured target
 * language before presentation. To present without translation, pass
 * an empty list.
 */
class ProgressAlert(
    private val title: String? = null,
    private val message: String,
    private val cancelButtonTitle: String? = null,
    private val cancelButtonStyle: ActionStyle = ActionStyle.CANCEL,
) {
    // MARK: - Types

    /** A value that identifies a translatable part of a [ProgressAlert]. */
    sealed interface TranslationOptionKey {
        /** The cancel button's title. */
        data object CancelButtonTitle : TranslationOptionKey

        /** The alert's message. */
        data object Message : TranslationOptionKey

        /** The alert's title. */
        data object Title : TranslationOptionKey
    }

    private data class PresentationContent(
        val cancelButtonTitle: String?,
        val message: String,
        val title: String?,
    )

    // MARK: - Properties

    private val progress = MutableStateFlow(0.0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var dismissesWhenPresented = false
    private var hasPresented = false
    private var messageAttributes: AttributedStringConfig? = null
    private var observationTask: Job? = null
    private var onCancelHandler: (() -> Unit)? = null
    private var presentedAlert: PresentedAlert.Progress? = null
    private var titleAttributes: AttributedStringConfig? = null

    // MARK: - Computed Properties

    private val presentationContent: PresentationContent
        get() =
            PresentationContent(
                cancelButtonTitle = cancelButtonTitle,
                message = message,
                title = title,
            )

    // MARK: - Methods

    /**
     * Dismisses the alert.
     *
     * Call this method to dismiss an alert presented with [present]
     * once your work completes. Alerts presented with a flow dismiss
     * themselves automatically when the observed operation completes.
     *
     * If the alert has not yet finished presenting, it is dismissed
     * automatically as soon as presentation completes.
     */
    fun dismiss() {
        val presented = presentedAlert
        if (presented == null) {
            if (!hasPresented) dismissesWhenPresented = true
            return
        }

        presentedAlert = null
        onCancelHandler = null
        scope.launch {
            AlertPresenter.current.first { it === presented }
            AlertPresenter.dismiss()
        }
    }

    /**
     * Disables the action at the specified index in the currently
     * presented alert.
     *
     * @param index The zero-based index of the action to disable.
     */
    fun disableAction(index: Int) {
        AlertPresenter.setActionEnabled(index, false)
    }

    /**
     * Enables the action at the specified index in the currently
     * presented alert.
     *
     * @param index The zero-based index of the action to enable.
     */
    fun enableAction(index: Int) {
        AlertPresenter.setActionEnabled(index, true)
    }

    /**
     * Registers a callback that is invoked when the user taps the
     * alert's cancel button.
     *
     * Call this method before presenting the alert. The callback is
     * released when the alert is dismissed.
     *
     * @param perform The closure to call when the user cancels.
     */
    fun onCancel(perform: () -> Unit) {
        onCancelHandler = perform
    }

    /**
     * Sets the attributed string configuration for the alert's
     * message.
     *
     * Call this method before presenting the alert to customize the
     * appearance of the message text.
     *
     * @param messageAttributes The attributed string configuration
     *   to apply to the message.
     */
    fun setMessageAttributes(messageAttributes: AttributedStringConfig) {
        this.messageAttributes = messageAttributes
    }

    /**
     * Sets the attributed string configuration for the alert's
     * title.
     *
     * Call this method before presenting the alert to customize the
     * appearance of the title text.
     *
     * @param titleAttributes The attributed string configuration to
     *   apply to the title.
     */
    fun setTitleAttributes(titleAttributes: AttributedStringConfig) {
        this.titleAttributes = titleAttributes
    }

    /**
     * Presents the alert and returns once it is presented.
     *
     * Unlike other alert types, this method does not suspend until
     * the alert is dismissed. After presentation, advance the
     * progress bar with [updateProgress] and dismiss the alert with
     * [dismiss] when your work completes.
     *
     * This method translates the alert's content before presentation
     * according to the specified keys. Each key identifies a part of
     * the alert to translate. To skip translation, pass an empty
     * list.
     *
     * @param translating The parts of the alert to translate. The
     *   default includes all translatable content.
     */
    suspend fun present(
        translating: List<TranslationOptionKey> =
            listOf(
                TranslationOptionKey.CancelButtonTitle,
                TranslationOptionKey.Message,
                TranslationOptionKey.Title,
            ),
    ) {
        presentWith(resolvedContent(translating))
    }

    /**
     * Presents the alert and drives its progress bar with the values
     * of the given flow, suspending until the observed operation
     * completes.
     *
     * The flow is collected only after the alert is presented,
     * deferring the start of the underlying operation until the
     * alert is visible. Each element is a completion fraction in the
     * range `0.0` through `1.0`; values outside the range are
     * clamped. When the flow finishes, the alert dismisses itself
     * and this method returns. When the flow throws, the alert
     * dismisses itself and this method rethrows the error.
     *
     * If the alert includes a cancel button, tapping it cancels the
     * collection and this method throws a `CancellationException`.
     *
     * This method translates the alert's content before presentation
     * according to the specified keys. Each key identifies a part of
     * the alert to translate. To skip translation, pass an empty
     * list.
     *
     * @param observing A flow of completion fractions that drives
     *   the alert's progress bar.
     * @param translating The parts of the alert to translate. The
     *   default includes all translatable content.
     *
     * @throws Throwable The error produced by the flow, or
     *   `CancellationException` if the user cancels.
     */
    suspend fun present(
        observing: Flow<Double>,
        translating: List<TranslationOptionKey> =
            listOf(
                TranslationOptionKey.CancelButtonTitle,
                TranslationOptionKey.Message,
                TranslationOptionKey.Title,
            ),
    ) {
        presentWith(resolvedContent(translating))
        val presented = presentedAlert ?: return
        AlertPresenter.current.first { it === presented }

        var failure: Throwable? = null
        coroutineScope {
            val task =
                launch {
                    try {
                        observing.collect { updateProgress(it) }
                    } catch (throwable: Throwable) {
                        failure = throwable
                    }
                }

            observationTask = task
            task.join()
        }

        observationTask = null
        val collected = failure
        if (collected == null) {
            updateProgress(1.0)
            dismiss()
            return
        }

        dismiss()
        throw collected
    }

    /** Advances the progress bar to [fractionCompleted], clamped to `0.0...1.0`. */
    fun updateProgress(fractionCompleted: Double) {
        progress.value = fractionCompleted.coerceIn(0.0, 1.0)
    }

    // MARK: - Auxiliary

    private fun handleCancelActionTap() {
        observationTask?.cancel()
        onCancelHandler?.invoke()
        dismiss()
    }

    private fun presentWith(content: PresentationContent) {
        val alert =
            PresentedAlert.Progress(
                title = content.title,
                message = content.message,
                cancelButtonTitle = content.cancelButtonTitle,
                cancelButtonStyle = cancelButtonStyle,
                progress = progress.asStateFlow(),
                messageAttributes = messageAttributes,
                titleAttributes = titleAttributes,
                onCancel = if (content.cancelButtonTitle == null) null else ({ handleCancelActionTap() }),
            )

        hasPresented = true
        presentedAlert = alert
        AlertPresenter.present(alert)

        if (dismissesWhenPresented) {
            dismissesWhenPresented = false
            dismiss()
        }
    }

    private suspend fun resolvedContent(translating: List<TranslationOptionKey>): PresentationContent =
        AlertKitConfig.presentWithTranslation(
            shouldTranslate = translating.isNotEmpty() && AlertKitConfig.translationDelegate != null,
            presentDirectly = { presentationContent },
            translate = { translate(translating) },
            presentTranslated = { it },
        )

    private suspend fun translate(keys: List<TranslationOptionKey>): PresentationContent {
        val uniqueKeys = keys.distinct()
        if (uniqueKeys.isEmpty()) return presentationContent

        val translations = AlertKitConfig.getTranslations(translationInputs(uniqueKeys))
        return PresentationContent(
            cancelButtonTitle = cancelButtonTitle?.let { translations.firstOutput(it) },
            message = translations.firstOutput(message),
            title = title?.let { translations.firstOutput(it) },
        )
    }

    private fun translationInputs(keys: List<TranslationOptionKey>): List<TranslationInput> {
        val inputs = mutableListOf<TranslationInput>()
        for (key in keys) {
            when (key) {
                TranslationOptionKey.CancelButtonTitle -> cancelButtonTitle?.let { inputs.add(TranslationInput(it)) }
                TranslationOptionKey.Message -> inputs.add(TranslationInput(message))
                TranslationOptionKey.Title -> title?.let { inputs.add(TranslationInput(it)) }
            }
        }

        return inputs.nonDefaultUnique
    }
}
