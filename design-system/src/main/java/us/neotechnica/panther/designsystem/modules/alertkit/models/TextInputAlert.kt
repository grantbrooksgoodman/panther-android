//
//  TextInputAlert.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 02/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.suspendCancellableCoroutine
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.firstOutput
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.translator.models.TranslationInput
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
 *     attributes = TextFieldAttributes(placeholderText = "Name"),
 * ).present()
 * ```
 *
 * Pass translation keys to [present] to translate the alert's content
 * into the user's language before presentation.
 */
class TextInputAlert(
    private val title: String? = null,
    private val message: String,
    private val attributes: TextFieldAttributes = TextFieldAttributes(),
    private val cancelButtonTitle: String = DEFAULT_CANCEL_BUTTON_TITLE,
    private val cancelButtonStyle: ActionStyle = ActionStyle.CANCEL,
    private val confirmButtonTitle: String = DEFAULT_CONFIRM_BUTTON_TITLE,
    private val confirmButtonStyle: ActionStyle = ActionStyle.PREFERRED,
    private val isConfirmEnabled: ((String) -> Boolean)? = null,
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
        data object Placeholder : TranslationOptionKey

        /** The text prepopulated in the field. */
        data object SampleText : TranslationOptionKey

        /** The alert's title. */
        data object Title : TranslationOptionKey
    }

    // MARK: - Methods

    /**
     * Presents the alert and suspends until the user confirms or
     * cancels.
     *
     * When [isConfirmEnabled] is provided, the confirm button is
     * enabled live as the field's text changes, only while the closure
     * returns `true`.
     *
     * @return The entered text on confirmation, or `null` on cancel.
     */
    suspend fun present(): String? =
        suspendCancellableCoroutine { continuation ->
            AlertPresenter.present(
                PresentedAlert.TextInput(
                    title = title,
                    message = message,
                    attributes = attributes,
                    cancelButtonTitle = cancelButtonTitle,
                    cancelButtonStyle = cancelButtonStyle,
                    confirmButtonTitle = confirmButtonTitle,
                    confirmButtonStyle = confirmButtonStyle,
                    isConfirmEnabled = isConfirmEnabled,
                ) { result ->
                    AlertPresenter.dismiss()
                    if (continuation.isActive) continuation.resume(result)
                },
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
                TranslationOptionKey.Placeholder,
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

        return TextInputAlert(
            title = title?.let { translations.firstOutput(it) },
            message = translations.firstOutput(message),
            attributes = translatedAttributes,
            cancelButtonTitle = translations.firstOutput(cancelButtonTitle),
            cancelButtonStyle = cancelButtonStyle,
            confirmButtonTitle = translations.firstOutput(confirmButtonTitle),
            confirmButtonStyle = confirmButtonStyle,
            isConfirmEnabled = isConfirmEnabled,
        )
    }

    private fun translationInputs(keys: List<TranslationOptionKey>): List<TranslationInput> {
        val inputs = mutableListOf<TranslationInput>()
        for (key in keys) {
            when (key) {
                TranslationOptionKey.CancelButtonTitle -> inputs.add(TranslationInput(cancelButtonTitle))
                TranslationOptionKey.ConfirmButtonTitle -> inputs.add(TranslationInput(confirmButtonTitle))
                TranslationOptionKey.Message -> inputs.add(TranslationInput(message))
                TranslationOptionKey.Placeholder -> attributes.placeholderText?.let { inputs.add(TranslationInput(it)) }
                TranslationOptionKey.SampleText -> attributes.sampleText?.let { inputs.add(TranslationInput(it)) }
                TranslationOptionKey.Title -> title?.let { inputs.add(TranslationInput(it)) }
            }
        }

        // nonDefaultUnique: translate each unique string once, leaving the
        // default button titles to be localized from the string catalog.
        return inputs
            .distinctBy { it.value }
            .filterNot { it.value == DEFAULT_CANCEL_BUTTON_TITLE || it.value == DEFAULT_CONFIRM_BUTTON_TITLE }
    }

    // MARK: - Companion

    private companion object {
        const val DEFAULT_CANCEL_BUTTON_TITLE = "Cancel"
        const val DEFAULT_CONFIRM_BUTTON_TITLE = "Confirm"
    }
}
