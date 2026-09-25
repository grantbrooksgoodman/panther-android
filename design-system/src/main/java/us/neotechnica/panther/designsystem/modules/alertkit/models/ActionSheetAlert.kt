//
//  ActionSheetAlert.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.suspendCancellableCoroutine
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.applying
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.firstOutput
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.translator.models.TranslationInput
import kotlin.coroutines.resume

/**
 * A bottom action sheet, standing in for the iOS `AKActionSheet`.
 *
 * The sheet has two forms:
 *
 * - A binary confirm/cancel sheet, created with a [confirmButtonTitle].
 *   [present] resolves to `true` when the user confirms and `false` when
 *   they cancel.
 * - A multi-action sheet, created with a list of [Action]s. [present]
 *   runs the selected action's effect and resolves to `true`;
 *   cancelling resolves to `false`.
 *
 * Pass translation keys to [present] to translate the sheet's content
 * into the user's language before presentation.
 */
class ActionSheetAlert private constructor(
    private val title: String?,
    private val message: String?,
    private val confirmButtonTitle: String?,
    private val cancelButtonTitle: String,
    private val isDestructive: Boolean,
    private val actions: List<Action>?,
) {
    // MARK: - Types

    /** A value that identifies a translatable part of an [ActionSheetAlert]. */
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

    // MARK: - Init

    /** Creates a binary confirm/cancel action sheet. */
    constructor(
        title: String? = null,
        message: String? = null,
        confirmButtonTitle: String,
        cancelButtonTitle: String = "Cancel",
        isDestructive: Boolean = false,
    ) : this(title, message, confirmButtonTitle, cancelButtonTitle, isDestructive, null)

    /** Creates a multi-action action sheet offering [actions] and a cancel button. */
    constructor(
        title: String? = null,
        message: String? = null,
        actions: List<Action>,
        cancelButtonTitle: String = "Cancel",
    ) : this(title, message, null, cancelButtonTitle, false, actions)

    // MARK: - Methods

    /**
     * Presents the sheet and suspends until the user makes a choice.
     *
     * @return `true` if the user confirms or selects an action;
     *   otherwise, `false`.
     */
    suspend fun present(): Boolean = presentActions(actions ?: listOf(confirmAction()))

    /**
     * Translates the sheet's content according to [translating], then
     * presents it. Falls back to untranslated content if translation
     * fails.
     *
     * @param translating The parts of the sheet to translate. The
     *   default includes all translatable content.
     *
     * @return `true` if the user confirms or selects an action;
     *   otherwise, `false`.
     */
    suspend fun present(
        translating: List<TranslationOptionKey> =
            listOf(
                TranslationOptionKey.Actions(),
                TranslationOptionKey.CancelButtonTitle,
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

    /** The single action a binary confirm/cancel sheet presents. */
    private fun confirmAction(): Action =
        Action(
            title = confirmButtonTitle.orEmpty(),
            style = if (isDestructive) ActionStyle.DESTRUCTIVE else ActionStyle.DEFAULT,
        ) {}

    private suspend fun presentActions(actions: List<Action>): Boolean =
        suspendCancellableCoroutine { continuation ->
            AlertPresenter.present(
                PresentedAlert.ActionSheet(
                    title = title,
                    message = message,
                    actions = actions,
                    cancelButtonTitle = cancelButtonTitle,
                    onSelect = { index ->
                        AlertPresenter.dismiss()
                        actions.getOrNull(index)?.effect?.invoke()
                        if (continuation.isActive) continuation.resume(true)
                    },
                    onCancel = {
                        AlertPresenter.dismiss()
                        if (continuation.isActive) continuation.resume(false)
                    },
                ),
            )

            continuation.invokeOnCancellation { AlertPresenter.dismiss() }
        }

    private suspend fun translate(keys: List<TranslationOptionKey>): ActionSheetAlert {
        val uniqueKeys = keys.distinct()
        if (uniqueKeys.isEmpty()) return this

        val translations = AlertKitConfig.getTranslations(translationInputs(uniqueKeys))
        return ActionSheetAlert(
            title = title?.let { translations.firstOutput(it) },
            message = message?.let { translations.firstOutput(it) },
            confirmButtonTitle = confirmButtonTitle?.let { translations.firstOutput(it) },
            cancelButtonTitle = translations.firstOutput(cancelButtonTitle),
            isDestructive = isDestructive,
            actions = actions?.applying(translations),
        )
    }

    private fun translationInputs(keys: List<TranslationOptionKey>): List<TranslationInput> {
        val effectiveActions = actions ?: listOf(confirmAction())
        val inputs = mutableListOf<TranslationInput>()
        for (key in keys) {
            when (key) {
                is TranslationOptionKey.Actions -> {
                    val targetActions =
                        if (key.actions.isEmpty()) {
                            effectiveActions
                        } else {
                            effectiveActions.filter { action -> key.actions.any { it.title == action.title } }
                        }
                    inputs.addAll(targetActions.map { TranslationInput(it.title) })
                }

                TranslationOptionKey.CancelButtonTitle -> inputs.add(TranslationInput(cancelButtonTitle))
                TranslationOptionKey.Message -> message?.let { inputs.add(TranslationInput(it)) }
                TranslationOptionKey.Title -> title?.let { inputs.add(TranslationInput(it)) }
            }
        }

        return inputs.distinctBy { it.value }.filter { it.value != DEFAULT_ACTION_TITLE }
    }
}

private const val DEFAULT_ACTION_TITLE = "OK"
