//
//  ReportDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.interfaces

import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

/**
 * An interface for filing error reports from AlertKit.
 *
 * Conform to this interface and register it with
 * [AlertKitConfig.registerReportDelegate][us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig.registerReportDelegate]
 * to handle the "Send Error Report" action of an
 * [ErrorAlert][us.neotechnica.panther.designsystem.modules.alertkit.models.ErrorAlert].
 */
interface ReportDelegate {
    // MARK: - Methods

    /**
     * Files a report for the given exception.
     *
     * @param exception The exception to report.
     */
    fun fileReport(exception: Exception)

    /**
     * Composes and presents a bug report.
     *
     * The message prompts the user to describe the issue and the steps
     * to reproduce it.
     */
    fun reportBug()

    /**
     * Composes and presents a general feedback message.
     *
     * The message invites the user to share general feedback.
     */
    fun sendFeedback()
}
