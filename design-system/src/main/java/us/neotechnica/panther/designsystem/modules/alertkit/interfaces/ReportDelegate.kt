//
//  ReportDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.interfaces

import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ErrorReportDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

/**
 * An interface for filing error reports from AlertKit.
 *
 * Conform to this interface and register it with
 * [AlertKitConfig.registerReportDelegate][us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig.registerReportDelegate]
 * to handle the "Send Error Report" action of an
 * [ErrorAlert][us.neotechnica.panther.designsystem.modules.alertkit.models.ErrorAlert].
 * Registration also installs the delegate as the subsystem's
 * [ErrorReportDelegate], so automatic reports, toast taps, error
 * alerts, and the failure page all file through one object.
 */
interface ReportDelegate : ErrorReportDelegate {
    // MARK: - Methods

    /**
     * Files a report for the given exception.
     *
     * @param exception The exception to report.
     */
    override fun fileReport(exception: Exception)
}
