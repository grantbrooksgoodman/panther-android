//
//  ActionSheet.kt
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
 * An action sheet that displays a title, message, and a list of
 * actions.
 *
 * Use [ActionSheet] to present a set of choices related to an
 * action the user initiates. Create an action sheet with the
 * actions you want to offer, then call [present] to display it:
 *
 * ```kotlin
 * val actionSheet = ActionSheet(
 *     title = "Share Photo",
 *     actions = listOf(
 *         Action("Save to Camera Roll") { savePhoto() },
 *         Action("Copy Link") { copyLink() },
 *     ),
 * )
 *
 * actionSheet.present()
 * ```
 *
 * A cancel button is added automatically unless one of the
 * provided actions uses the [ActionStyle.CANCEL] style. You can
 * customize the cancel button's title through the
 * `cancelButtonTitle` parameter.
 *
 * By default, [present] translates all content into the
 * configured target language. Pass an empty list to skip
 * translation.
 */
class ActionSheet(
    private val title: String? = null,
    private val message: String? = null,
    private val actions: List<Action>,
    private val cancelButtonTitle: String = AlertKit.Constants.DEFAULT_CANCEL_BUTTON_TITLE,
) {
    // MARK: - Types

    /** A value that identifies a translatable part of an [ActionSheet]. */
    sealed interface TranslationOptionKey {
        /**
         * The action button titles. Pass an empty list to translate
         * all actions, or a subset to translate only those.
         */
        data class Actions(
            val actions: List<Action> = emptyList(),
        ) : TranslationOptionKey

        /** The cancel button's title. */
        data object CancelButtonTitle : TranslationOptionKey

        /** The action sheet's message. */
        data object Message : TranslationOptionKey

        /** The action sheet's title. */
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
     * Sets the attributed string configuration for the action
     * sheet's message.
     *
     * Call this method before [present] to customize the appearance
     * of the message text.
     *
     * @param messageAttributes The attributed string configuration
     *   to apply to the message.
     */
    fun setMessageAttributes(messageAttributes: AttributedStringConfig) {
        this.messageAttributes = messageAttributes
    }

    /**
     * Sets the attributed string configuration for the action
     * sheet's title.
     *
     * Call this method before [present] to customize the appearance
     * of the title text.
     *
     * @param titleAttributes The attributed string configuration to
     *   apply to the title.
     */
    fun setTitleAttributes(titleAttributes: AttributedStringConfig) {
        this.titleAttributes = titleAttributes
    }

    /**
     * Presents the action sheet and suspends until the user
     * selects an action or cancels.
     *
     * This method translates the action sheet's content before
     * presentation according to the specified keys. Each key
     * identifies a part of the action sheet to translate. To skip
     * translation, pass an empty list.
     *
     * @param translating The parts of the action sheet to
     *   translate. The default includes all translatable content.
     */
    suspend fun present(
        translating: List<TranslationOptionKey> =
            listOf(
                TranslationOptionKey.Actions(),
                TranslationOptionKey.CancelButtonTitle,
                TranslationOptionKey.Message,
                TranslationOptionKey.Title,
            ),
    ): Unit =
        AlertKitConfig.presentWithTranslation(
            shouldTranslate = translating.isNotEmpty() && AlertKitConfig.translationDelegate != null,
            presentDirectly = { presentActions() },
            translate = { translate(translating) },
            presentTranslated = { it.present(translating = emptyList()) },
        )

    // MARK: - Auxiliary

    private suspend fun presentActions(): Unit =
        suspendCancellableCoroutine { continuation ->
            val guard = ContinuationGuard(continuation, fallbackValue = Unit)

            // A cancel-styled action occupies the cancel slot; the automatic
            // cancel button is added only when no action carries that style.
            val cancelAction = actions.firstOrNull { it.style == ActionStyle.CANCEL }
            val displayedActions = actions.filter { it.style != ActionStyle.CANCEL }
            AlertPresenter.present(
                PresentedAlert.ActionSheet(
                    title = title,
                    message = message,
                    actions = displayedActions,
                    cancelButtonTitle = cancelAction?.title ?: cancelButtonTitle,
                    messageAttributes = messageAttributes,
                    titleAttributes = titleAttributes,
                    onSelect = { index ->
                        displayedActions.getOrNull(index)?.effect?.invoke()
                        guard.resume(Unit)
                        AlertPresenter.dismiss()
                    },
                    onCancel = {
                        cancelAction?.effect?.invoke()
                        guard.resume(Unit)
                        AlertPresenter.dismiss()
                    },
                ),
                onDisplaced = { guard.fallback() },
            )

            continuation.invokeOnCancellation { AlertPresenter.dismiss() }
        }

    private suspend fun translate(keys: List<TranslationOptionKey>): ActionSheet {
        val uniqueKeys = keys.distinct()
        if (uniqueKeys.isEmpty()) return this

        val translations = AlertKitConfig.getTranslations(translationInputs(uniqueKeys))
        val alert =
            ActionSheet(
                title = title?.let { translations.firstOutput(it) },
                message = message?.let { translations.firstOutput(it) },
                actions = actions.applying(translations),
                cancelButtonTitle = translations.firstOutput(cancelButtonTitle),
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

                TranslationOptionKey.CancelButtonTitle -> inputs.add(TranslationInput(cancelButtonTitle))
                TranslationOptionKey.Message -> message?.let { inputs.add(TranslationInput(it)) }
                TranslationOptionKey.Title -> title?.let { inputs.add(TranslationInput(it)) }
            }
        }

        return inputs.nonDefaultUnique
    }
}
