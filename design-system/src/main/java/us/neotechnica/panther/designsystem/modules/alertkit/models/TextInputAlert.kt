//
//  TextInputAlert.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
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
 *     placeholder = "Name",
 * ).present()
 * ```
 *
 * Pass translation keys to [present] to translate the alert's content
 * into the user's language before presentation.
 */
class TextInputAlert(
    private val title: String? = null,
    private val message: String,
    private val placeholder: String = "",
    private val initialText: String = "",
    private val isSecure: Boolean = false,
    private val cancelButtonTitle: String = "Cancel",
    private val confirmButtonTitle: String = "Confirm",
    private val isConfirmEnabled: ((String) -> Boolean)? = null,
) {
    // MARK: - Types

    /** A value that identifies a translatable part of a [TextInputAlert]. */
    sealed interface TranslationOptionKey {
        /** The alert's message. */
        data object Message : TranslationOptionKey

        /** The text field's placeholder. */
        data object Placeholder : TranslationOptionKey

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
                    placeholder = placeholder,
                    initialText = initialText,
                    isSecure = isSecure,
                    cancelButtonTitle = cancelButtonTitle,
                    confirmButtonTitle = confirmButtonTitle,
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
     * fails. The confirm and cancel button titles are expected to be
     * pre-localized by the caller, mirroring iOS.
     *
     * @param translating The parts of the alert to translate. The
     *   default includes all translatable content.
     *
     * @return The entered text on confirmation, or `null` on cancel.
     */
    suspend fun present(
        translating: List<TranslationOptionKey> =
            listOf(
                TranslationOptionKey.Message,
                TranslationOptionKey.Placeholder,
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
        return TextInputAlert(
            title = title?.let { translations.firstOutput(it) },
            message = translations.firstOutput(message),
            placeholder = translations.firstOutput(placeholder),
            initialText = initialText,
            isSecure = isSecure,
            cancelButtonTitle = cancelButtonTitle,
            confirmButtonTitle = confirmButtonTitle,
            isConfirmEnabled = isConfirmEnabled,
        )
    }

    private fun translationInputs(keys: List<TranslationOptionKey>): List<TranslationInput> {
        val inputs = mutableListOf<TranslationInput>()
        for (key in keys) {
            when (key) {
                TranslationOptionKey.Message -> inputs.add(TranslationInput(message))
                TranslationOptionKey.Placeholder -> if (placeholder.isNotEmpty()) inputs.add(TranslationInput(placeholder))
                TranslationOptionKey.Title -> title?.let { inputs.add(TranslationInput(it)) }
            }
        }

        return inputs.distinctBy { it.value }
    }
}
