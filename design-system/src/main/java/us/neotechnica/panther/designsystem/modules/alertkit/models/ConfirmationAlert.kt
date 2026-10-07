//
//  ConfirmationAlert.kt
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
 * An alert that asks the user to confirm or cancel an action.
 *
 * [present] resolves to `true` when the user taps the confirm button
 * and `false` when they cancel:
 *
 * ```kotlin
 * val confirmed = ConfirmationAlert(
 *     title = "Remove Item",
 *     message = "This action cannot be undone.",
 * ).present()
 *
 * if (confirmed) removeItem()
 * ```
 *
 * Pass translation keys to [present] to translate the alert's content
 * into the user's language before presentation.
 */
class ConfirmationAlert(
    private val title: String? = null,
    private val message: String,
    private val cancelButtonTitle: String = AlertKit.Constants.DEFAULT_CANCEL_BUTTON_TITLE,
    private val cancelButtonStyle: ActionStyle = ActionStyle.CANCEL,
    private val confirmButtonTitle: String = AlertKit.Constants.DEFAULT_CONFIRM_BUTTON_TITLE,
    private val confirmButtonStyle: ActionStyle = ActionStyle.PREFERRED,
) {
    // MARK: - Types

    /** A value that identifies a translatable part of a [ConfirmationAlert]. */
    sealed interface TranslationOptionKey {
        /** The cancel button's title. */
        data object CancelButtonTitle : TranslationOptionKey

        /** The confirm button's title. */
        data object ConfirmButtonTitle : TranslationOptionKey

        /** The alert's message. */
        data object Message : TranslationOptionKey

        /** The alert's title. */
        data object Title : TranslationOptionKey
    }

    // MARK: - Properties

    private var messageAttributes: AttributedStringConfig? = null
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
     * Presents the alert and suspends until the user makes a choice.
     *
     * @return `true` if the user confirms; otherwise, `false`.
     */
    suspend fun present(): Boolean =
        suspendCancellableCoroutine { continuation ->
            val guard = ContinuationGuard(continuation, fallbackValue = false)

            AlertPresenter.present(
                PresentedAlert.Confirmation(
                    title = title,
                    message = message,
                    cancelAction = Action(cancelButtonTitle, style = cancelButtonStyle) {},
                    confirmAction = Action(confirmButtonTitle, style = confirmButtonStyle) {},
                    messageAttributes = messageAttributes,
                    titleAttributes = titleAttributes,
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
     * @return `true` if the user confirms; otherwise, `false`.
     */
    suspend fun present(
        translating: List<TranslationOptionKey> =
            listOf(
                TranslationOptionKey.CancelButtonTitle,
                TranslationOptionKey.ConfirmButtonTitle,
                TranslationOptionKey.Message,
                TranslationOptionKey.Title,
            ),
    ): Boolean =
        AlertKitConfig.presentWithTranslation(
            shouldTranslate = translating.isNotEmpty() && AlertKitConfig.translationDelegate != null,
            presentDirectly = { present() },
            translate = { translate(translating) },
            presentTranslated = { it.present(translating = emptyList()) },
        )

    // MARK: - Auxiliary

    private suspend fun translate(keys: List<TranslationOptionKey>): ConfirmationAlert {
        val uniqueKeys = keys.distinct()
        if (uniqueKeys.isEmpty()) return this

        val translations = AlertKitConfig.getTranslations(translationInputs(uniqueKeys))
        val alert =
            ConfirmationAlert(
                title = title?.let { translations.firstOutput(it) },
                message = translations.firstOutput(message),
                cancelButtonTitle = translations.firstOutput(cancelButtonTitle),
                cancelButtonStyle = cancelButtonStyle,
                confirmButtonTitle = translations.firstOutput(confirmButtonTitle),
                confirmButtonStyle = confirmButtonStyle,
            )

        messageAttributes?.let { alert.setMessageAttributes(it) }
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
                TranslationOptionKey.Title -> title?.let { inputs.add(TranslationInput(it)) }
            }
        }

        return inputs.nonDefaultUnique
    }
}
