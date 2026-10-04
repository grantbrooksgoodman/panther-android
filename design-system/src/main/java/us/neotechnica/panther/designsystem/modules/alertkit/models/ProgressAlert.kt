//
//  ProgressAlert.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.firstOutput
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * A non-dismissable alert that displays a determinate progress bar
 * while work is in progress.
 *
 * Present the alert, advance the bar with [updateProgress], then
 * dismiss it:
 *
 * ```kotlin
 * val alert = ProgressAlert(message = "Processing…")
 * alert.present()
 * alert.updateProgress(0.5)
 * alert.dismiss()
 * ```
 *
 * Alternatively, pass a flow of completion fractions to drive the bar
 * automatically; the alert dismisses itself when the flow completes:
 *
 * ```kotlin
 * ProgressAlert(message = "Uploading…").present(observing = uploadProgress)
 * ```
 *
 * Provide a `cancelButtonTitle` to let the user abort the operation.
 */
class ProgressAlert(
    private val title: String? = null,
    private val message: String,
    private val cancelButtonTitle: String? = null,
    private val cancelButtonStyle: ActionStyle = ActionStyle.CANCEL,
    private val onCancel: (() -> Unit)? = null,
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

    // MARK: - Properties

    private val progress = MutableStateFlow(0.0)

    private var isPresented = false
    private var dismissesWhenPresented = false

    // MARK: - Methods

    /** Dismisses the alert, deferring until it has finished presenting. */
    fun dismiss() {
        if (!isPresented) {
            dismissesWhenPresented = true
            return
        }
        isPresented = false
        AlertPresenter.dismiss()
    }

    /** Presents the alert. Call [dismiss] once your work completes. */
    fun present() {
        presentResolved(title, message, cancelButtonTitle)
    }

    /**
     * Translates the alert's content according to [translating], then
     * presents it. Falls back to untranslated content if translation
     * fails.
     *
     * @param translating The parts of the alert to translate.
     */
    suspend fun present(
        translating: List<TranslationOptionKey> =
            listOf(
                TranslationOptionKey.CancelButtonTitle,
                TranslationOptionKey.Message,
                TranslationOptionKey.Title,
            ),
    ) {
        val uniqueKeys = translating.distinct()
        if (uniqueKeys.isEmpty() || AlertKitConfig.translationDelegate == null) return present()

        val translations = runCatching { AlertKitConfig.getTranslations(translationInputs(uniqueKeys)) }.getOrNull()
        if (translations == null) return present()

        presentResolved(
            title?.let { translations.firstOutput(it) },
            translations.firstOutput(message),
            cancelButtonTitle?.let { translations.firstOutput(it) },
        )
    }

    /**
     * Presents the alert, drives the progress bar from [observing], and
     * dismisses itself when the flow completes.
     *
     * @param observing A flow of completion fractions in `0.0...1.0`.
     * @param translating The parts of the alert to translate.
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
        present(translating)
        try {
            observing.collect { updateProgress(it) }
        } finally {
            dismiss()
        }
    }

    /** Advances the progress bar to [fractionCompleted], clamped to `0.0...1.0`. */
    fun updateProgress(fractionCompleted: Double) {
        progress.value = fractionCompleted.coerceIn(0.0, 1.0)
    }

    // MARK: - Auxiliary

    private fun presentResolved(
        title: String?,
        message: String,
        cancelButtonTitle: String?,
    ) {
        isPresented = true
        AlertPresenter.present(
            PresentedAlert.Progress(
                title = title,
                message = message,
                cancelButtonTitle = cancelButtonTitle,
                cancelButtonStyle = cancelButtonStyle,
                progress = progress.asStateFlow(),
                onCancel =
                    onCancel?.let { cancel ->
                        {
                            dismiss()
                            cancel()
                        }
                    },
            ),
        )

        if (dismissesWhenPresented) {
            dismissesWhenPresented = false
            dismiss()
        }
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

        return inputs.distinctBy { it.value }
    }
}
