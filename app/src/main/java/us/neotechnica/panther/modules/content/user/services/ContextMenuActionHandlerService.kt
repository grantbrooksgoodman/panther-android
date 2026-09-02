//
//  ContextMenuActionHandlerService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 01/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.modules.common.services.ErrorReportingService
import us.neotechnica.panther.modules.content.user.components.ChatMessageRowData
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.session.extensions.isFromCurrentUser
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * Builds context menu actions and handles their side effects.
 *
 * **Note:** this port currently handles only the report-mistranslation
 * action; the remaining actions (copy, view alternate) are built inline
 * by `ChatMessageCell` for now.
 *
 * The iOS original gates report-mistranslation behind retry-translation,
 * offering it only once every translation platform has been tried. This
 * port omits retry-translation, so it surfaces the action for any
 * eligible translated message and files the report directly.
 */
object ContextMenuActionHandlerService {
    // MARK: - Report Mistranslation

    /**
     * Returns the report-mistranslation action for [row], or `null` when
     * the message has no reportable translation currently on display.
     *
     * Mirrors iOS's retry-translation eligibility: the translation must be
     * non-idempotent, its output must differ from its input, either side
     * must contain letters, it must carry a hosting key, the message must
     * currently display its translation, and it must not already have been
     * reported during the current app session.
     *
     * A message displays its translation when it is a received message not
     * showing its alternate text, or an own message showing its alternate
     * text – a mistranslation is only reportable while it is visible.
     *
     * @param row The message row to build the action for.
     *
     * @return The action, or `null`.
     */
    fun reportMistranslationAction(row: ChatMessageRowData): ContextMenuAction? {
        val translation = row.translation ?: return null
        if (translation.languagePair.isIdempotent) return null

        val input = translation.input.value
        val output = translation.output
        if (output.lowercase().trim() == input.lowercase().trim()) return null
        if (input.none { it.isLetter() } && output.none { it.isLetter() }) return null

        val isDisplayingTranslation = if (row.message.isFromCurrentUser) row.showAlternate else !row.showAlternate
        if (!isDisplayingTranslation) return null

        val hostingKey = row.message.translationReferences?.firstOrNull()?.hostingKey ?: return null

        val exception = mistranslationException(hostingKey)
        if (exception.code in ErrorReportingService.reportedErrorCodes) return null

        return ContextMenuAction(
            title = LocalizedStringKey.ReportMistranslation.localized(),
            systemImageName = REPORT_ACTION_IMAGE_SYSTEM_NAME,
        ) { ErrorReportingService.fileReport(exception) }
    }

    // MARK: - Auxiliary

    private fun mistranslationException(hostingKey: String): Exception =
        Exception(
            "A mistranslation has been reported (${hostingKey.shortCode}).",
            userInfo =
                mapOf(
                    "Descriptor" to "A mistranslation has been reported.",
                    "HostedOverrideErrorCode" to "CA45",
                    "ReferenceHostingKey" to hostingKey,
                ),
            metadata = ExceptionMetadata(this),
        )

    private val String.shortCode: String
        get() = "${take(2)}${takeLast(2)}".uppercase()

    private const val REPORT_ACTION_IMAGE_SYSTEM_NAME = "flag"
}
