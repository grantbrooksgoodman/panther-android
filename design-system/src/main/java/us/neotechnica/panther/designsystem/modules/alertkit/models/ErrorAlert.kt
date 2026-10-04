//
//  ErrorAlert.kt
//  Panther Android
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
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.translator.models.TranslationInput
import kotlin.coroutines.resume

/**
 * An alert that reports an error to the user.
 *
 * When the error is reportable, automatic error reporting is off, and
 * a report delegate is registered, the alert shows a preferred
 * "Send Error Report" button that files a report through the
 * [ReportDelegate][us.neotechnica.panther.designsystem.modules.alertkit.interfaces.ReportDelegate]
 * registered with `AlertKitConfig`, with the error description as the
 * message. Otherwise, it shows the error description as the title and
 * the error identifier in the message.
 *
 * ```kotlin
 * ErrorAlert(exception).present()
 * ```
 *
 * Pass translation keys to [present] to translate the alert's content
 * into the user's language before presentation.
 */
class ErrorAlert(
    private val exception: Exception,
    private val dismissButtonTitle: String = "Dismiss",
    private val sendErrorReportButtonTitle: String = "Send Error Report",
    private val errorDescription: String = exception.userFacingDescriptor,
) {
    // MARK: - Types

    /** A value that identifies a translatable part of an [ErrorAlert]. */
    sealed interface TranslationOptionKey {
        /** The dismiss button's title. */
        data object DismissButtonTitle : TranslationOptionKey

        /** The error's description text. */
        data object ErrorDescription : TranslationOptionKey

        /** The send error report button's title. */
        data object SendErrorReportButtonTitle : TranslationOptionKey
    }

    // MARK: - Methods

    /**
     * Presents the error alert and suspends until the user dismisses it.
     */
    suspend fun present(): Unit =
        suspendCancellableCoroutine { continuation ->
            val showsReportAction =
                exception.isReportable &&
                    !Logger.reportsErrorsAutomatically &&
                    AlertKitConfig.reportDelegate != null

            AlertPresenter.present(
                PresentedAlert.ErrorContent(
                    title = if (showsReportAction) null else errorDescription,
                    message = if (showsReportAction) errorDescription else "\n${exception.id}",
                    dismissButtonTitle = dismissButtonTitle,
                    sendReportButtonTitle = if (showsReportAction) sendErrorReportButtonTitle else null,
                    onDismiss = {
                        AlertPresenter.dismiss()
                        if (continuation.isActive) continuation.resume(Unit)
                    },
                    onSendReport =
                        if (showsReportAction) {
                            {
                                AlertPresenter.dismiss()
                                AlertKitConfig.reportDelegate?.fileReport(exception)
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                        } else {
                            null
                        },
                ),
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
                TranslationOptionKey.DismissButtonTitle,
                TranslationOptionKey.ErrorDescription,
                TranslationOptionKey.SendErrorReportButtonTitle,
            ),
    ): Unit =
        AlertKitConfig.presentWithTranslation(
            shouldTranslate = translating.isNotEmpty() && AlertKitConfig.translationDelegate != null,
            presentDirectly = { present() },
            translate = { translate(translating) },
            presentTranslated = { it.present(translating = emptyList()) },
        )

    // MARK: - Auxiliary

    private suspend fun translate(keys: List<TranslationOptionKey>): ErrorAlert {
        val uniqueKeys = keys.distinct()
        if (uniqueKeys.isEmpty()) return this

        val translations = AlertKitConfig.getTranslations(translationInputs(uniqueKeys))
        return ErrorAlert(
            exception = exception,
            dismissButtonTitle = translations.firstOutput(dismissButtonTitle),
            sendErrorReportButtonTitle = translations.firstOutput(sendErrorReportButtonTitle),
            errorDescription = translations.firstOutput(errorDescription),
        )
    }

    private fun translationInputs(keys: List<TranslationOptionKey>): List<TranslationInput> {
        val inputs = mutableListOf<TranslationInput>()
        for (key in keys) {
            when (key) {
                TranslationOptionKey.DismissButtonTitle -> inputs.add(TranslationInput(dismissButtonTitle))
                TranslationOptionKey.ErrorDescription -> inputs.add(TranslationInput(errorDescription))
                TranslationOptionKey.SendErrorReportButtonTitle -> inputs.add(TranslationInput(sendErrorReportButtonTitle))
            }
        }

        return inputs.distinctBy { it.value }.filter { it.value.isNotBlank() }
    }
}
