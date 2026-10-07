//
//  TextInputAlert.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.suspendCancellableCoroutine
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKit
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.firstOutput
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.nonDefaultUnique
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * An alert that prompts the user for a single line of text.
 *
 * [present] resolves to the entered text when the user confirms, or
 * `null` when they cancel:
 *
 * ```kotlin
 * val name = TextInputAlert(
 *     message = "Enter a name for this conversation.",
 *     attributes = TextFieldAttributes(placeholderText = "Name"),
 * ).present()
 * ```
 *
 * To respond to changes in the text field as the user types,
 * register a callback with [onTextFieldChange] before presenting
 * the alert:
 *
 * ```kotlin
 * alert.onTextFieldChange { text ->
 *     // Respond to changes in the text field.
 * }
 * ```
 *
 * The observer is automatically removed when the alert is
 * dismissed.
 *
 * Pass translation keys to [present] to translate the alert's content
 * into the user's language before presentation.
 */
class TextInputAlert(
    private val title: String? = null,
    private val message: String,
    private val attributes: TextFieldAttributes = TextFieldAttributes(),
    private val cancelButtonTitle: String = AlertKit.Constants.DEFAULT_CANCEL_BUTTON_TITLE,
    private val cancelButtonStyle: ActionStyle = ActionStyle.CANCEL,
    private val confirmButtonTitle: String = AlertKit.Constants.DEFAULT_CONFIRM_BUTTON_TITLE,
    private val confirmButtonStyle: ActionStyle = ActionStyle.PREFERRED,
) {
    // MARK: - Types

    /** A value that identifies a translatable part of a [TextInputAlert]. */
    sealed interface TranslationOptionKey {
        /** The cancel button's title. */
        data object CancelButtonTitle : TranslationOptionKey

        /** The confirm button's title. */
        data object ConfirmButtonTitle : TranslationOptionKey

        /** The alert's message. */
        data object Message : TranslationOptionKey

        /** The text field's placeholder. */
        data object PlaceholderText : TranslationOptionKey

        /** The text prepopulated in the field. */
        data object SampleText : TranslationOptionKey

        /** The alert's title. */
        data object Title : TranslationOptionKey
    }

    // MARK: - Properties

    private var messageAttributes: AttributedStringConfig? = null
    private var onTextFieldChangeHandler: ((String?) -> Unit)? = null
    private var titleAttributes: AttributedStringConfig? = null

    // MARK: - Methods

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
     * Registers a callback that is invoked when the text field's
     * text changes.
     *
     * Call this method before presenting the alert. The callback is
     * released when the alert is dismissed.
     *
     * @param perform The closure to call when the text changes.
     */
    fun onTextFieldChange(perform: (String?) -> Unit) {
        onTextFieldChangeHandler = perform
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
     * Presents the alert and suspends until the user confirms or
     * cancels.
     *
     * @return The entered text on confirmation, or `null` on cancel.
     */
    suspend fun present(): String? =
        suspendCancellableCoroutine { continuation ->
            val guard = ContinuationGuard<String?>(continuation, fallbackValue = null)

            AlertPresenter.present(
                PresentedAlert.TextInput(
                    title = title,
                    message = message,
                    attributes = attributes,
                    cancelButtonTitle = cancelButtonTitle,
                    cancelButtonStyle = cancelButtonStyle,
                    confirmButtonTitle = confirmButtonTitle,
                    confirmButtonStyle = confirmButtonStyle,
                    messageAttributes = messageAttributes,
                    titleAttributes = titleAttributes,
                    onTextFieldChange = onTextFieldChangeHandler,
                ) { result ->
                    guard.resume(result)
                    AlertPresenter.dismiss()
                },
                onDisplaced = { guard.fallback() },
            )

            continuation.invokeOnCancellation { AlertPresenter.dismiss() }
        }

    /**
     * Translates the alert's content according to [translating], then
     * presents it. Falls back to untranslated content if translation
     * fails.
     *
     * @param translating The parts of the alert to translate. The
     *   default includes all translatable content.
     *
     * @return The entered text on confirmation, or `null` on cancel.
     */
    suspend fun present(
        translating: List<TranslationOptionKey> =
            listOf(
                TranslationOptionKey.CancelButtonTitle,
                TranslationOptionKey.ConfirmButtonTitle,
                TranslationOptionKey.Message,
                TranslationOptionKey.PlaceholderText,
                TranslationOptionKey.SampleText,
                TranslationOptionKey.Title,
            ),
    ): String? =
        AlertKitConfig.presentWithTranslation(
            shouldTranslate = translating.isNotEmpty() && AlertKitConfig.translationDelegate != null,
            presentDirectly = { present() },
            translate = { translate(translating) },
            presentTranslated = { it.present(translating = emptyList()) },
        )

    // MARK: - Auxiliary

    private suspend fun translate(keys: List<TranslationOptionKey>): TextInputAlert {
        val uniqueKeys = keys.distinct()
        if (uniqueKeys.isEmpty()) return this

        val translations = AlertKitConfig.getTranslations(translationInputs(uniqueKeys))
        var translatedAttributes = attributes
        attributes.placeholderText?.let {
            translatedAttributes = translatedAttributes.replacingPlaceholderText(translations.firstOutput(it))
        }
        attributes.sampleText?.let {
            translatedAttributes = translatedAttributes.replacingSampleText(translations.firstOutput(it))
        }

        val alert =
            TextInputAlert(
                title = title?.let { translations.firstOutput(it) },
                message = translations.firstOutput(message),
                attributes = translatedAttributes,
                cancelButtonTitle = translations.firstOutput(cancelButtonTitle),
                cancelButtonStyle = cancelButtonStyle,
                confirmButtonTitle = translations.firstOutput(confirmButtonTitle),
                confirmButtonStyle = confirmButtonStyle,
            )

        messageAttributes?.let { alert.setMessageAttributes(it) }
        onTextFieldChangeHandler?.let { alert.onTextFieldChange(it) }
        titleAttributes?.let { alert.setTitleAttributes(it) }
        return alert
    }

    private fun translationInputs(keys: List<TranslationOptionKey>): List<TranslationInput> {
        val inputs = mutableListOf<TranslationInput>()
        for (key in keys) {
            when (key) {
                TranslationOptionKey.CancelButtonTitle -> inputs.add(TranslationInput(cancelButtonTitle))
                TranslationOptionKey.ConfirmButtonTitle -> inputs.add(TranslationInput(confirmButtonTitle))
                TranslationOptionKey.Message -> inputs.add(TranslationInput(message))
                TranslationOptionKey.PlaceholderText -> attributes.placeholderText?.let { inputs.add(TranslationInput(it)) }
                TranslationOptionKey.SampleText -> attributes.sampleText?.let { inputs.add(TranslationInput(it)) }
                TranslationOptionKey.Title -> title?.let { inputs.add(TranslationInput(it)) }
            }
        }

        return inputs.nonDefaultUnique
    }
}
