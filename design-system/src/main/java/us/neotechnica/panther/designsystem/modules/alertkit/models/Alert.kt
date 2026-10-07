//
//  Alert.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.suspendCancellableCoroutine
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKit
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.applying
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.firstOutput
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.nonDefaultUnique
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * An alert that displays a title, message, and a set of actions.
 *
 * Create an alert with a title, an optional message, and one or more
 * actions, then call [present] to display it and suspend until the user
 * selects an action:
 *
 * ```kotlin
 * Alert(
 *     title = "Remove Item",
 *     message = "This action cannot be undone.",
 *     actions = listOf(
 *         Action("Remove", style = ActionStyle.DESTRUCTIVE) { removeItem() },
 *         Action("Cancel", style = ActionStyle.CANCEL) {},
 *     ),
 * ).present()
 * ```
 *
 * When you omit `actions`, the alert displays a single "OK" button. Pass
 * translation keys to [present] to translate the alert's content into
 * the user's language before presentation.
 */
class Alert(
    private val title: String? = null,
    private val message: String?,
    private val actions: List<Action> = listOf(Action(AlertKit.Constants.DEFAULT_ACTION_TITLE, style = ActionStyle.CANCEL) {}),
) {
    // MARK: - Types

    /** A value that identifies a translatable part of an [Alert]. */
    sealed interface TranslationOptionKey {
        /**
         * The action button titles. Pass an empty list to translate
         * all actions, or a subset to translate only those.
         */
        data class Actions(
            val actions: List<Action> = emptyList(),
        ) : TranslationOptionKey

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
     * Presents the alert and suspends until the user selects an action,
     * running that action's effect.
     */
    suspend fun present(): Unit =
        suspendCancellableCoroutine { continuation ->
            val guard = ContinuationGuard(continuation, fallbackValue = Unit)

            AlertPresenter.present(
                PresentedAlert.Standard(
                    title = title,
                    message = message,
                    actions = actions,
                    messageAttributes = messageAttributes,
                    titleAttributes = titleAttributes,
                ) { index ->
                    actions.getOrNull(index)?.effect?.invoke()
                    guard.resume(Unit)
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
     */
    suspend fun present(
        translating: List<TranslationOptionKey> =
            listOf(
                TranslationOptionKey.Actions(),
                TranslationOptionKey.Message,
                TranslationOptionKey.Title,
            ),
    ): Unit =
        AlertKitConfig.presentWithTranslation(
            shouldTranslate = translating.isNotEmpty() && AlertKitConfig.translationDelegate != null,
            presentDirectly = { present() },
            translate = { translate(translating) },
            presentTranslated = { it.present(translating = emptyList()) },
        )

    // MARK: - Auxiliary

    private suspend fun translate(keys: List<TranslationOptionKey>): Alert {
        val uniqueKeys = keys.distinct()
        if (uniqueKeys.isEmpty()) return this

        val translations = AlertKitConfig.getTranslations(translationInputs(uniqueKeys))
        val alert =
            Alert(
                title = title?.let { translations.firstOutput(it) },
                message = message?.let { translations.firstOutput(it) },
                actions = actions.applying(translations),
            )

        messageAttributes?.let { alert.setMessageAttributes(it) }
        titleAttributes?.let { alert.setTitleAttributes(it) }
        return alert
    }

    private fun translationInputs(keys: List<TranslationOptionKey>): List<TranslationInput> {
        val inputs = mutableListOf<TranslationInput>()
        for (key in keys) {
            when (key) {
                is TranslationOptionKey.Actions -> {
                    val targetActions =
                        if (key.actions.isEmpty()) {
                            actions
                        } else {
                            actions.filter { action -> key.actions.contains(action) }
                        }
                    inputs.addAll(targetActions.map { TranslationInput(it.title) })
                }

                TranslationOptionKey.Message -> message?.let { inputs.add(TranslationInput(it)) }
                TranslationOptionKey.Title -> title?.let { inputs.add(TranslationInput(it)) }
            }
        }

        return inputs.nonDefaultUnique
    }
}
