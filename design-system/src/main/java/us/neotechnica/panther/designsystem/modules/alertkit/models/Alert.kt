//
//  Alert.kt
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
    private val actions: List<Action> = listOf(Action("OK", style = ActionStyle.CANCEL) {}),
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

    // MARK: - Methods

    /**
     * Presents the alert and suspends until the user selects an action,
     * running that action's effect.
     */
    suspend fun present(): Unit =
        suspendCancellableCoroutine { continuation ->
            AlertPresenter.present(
                PresentedAlert.Standard(
                    title = title,
                    message = message,
                    actions = actions,
                ) { index ->
                    AlertPresenter.dismiss()
                    actions.getOrNull(index)?.effect?.invoke()
                    if (continuation.isActive) continuation.resume(Unit)
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
        return Alert(
            title = title?.let { translations.firstOutput(it) },
            message = message?.let { translations.firstOutput(it) },
            actions = actions.applying(translations),
        )
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
                            actions.filter { action -> key.actions.any { it.title == action.title } }
                        }
                    inputs.addAll(targetActions.map { TranslationInput(it.title) })
                }

                TranslationOptionKey.Message -> message?.let { inputs.add(TranslationInput(it)) }
                TranslationOptionKey.Title -> title?.let { inputs.add(TranslationInput(it)) }
            }
        }

        return inputs.distinctBy { it.value }.filter { it.value != DEFAULT_ACTION_TITLE }
    }
}

private const val DEFAULT_ACTION_TITLE = "OK"
