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
}
