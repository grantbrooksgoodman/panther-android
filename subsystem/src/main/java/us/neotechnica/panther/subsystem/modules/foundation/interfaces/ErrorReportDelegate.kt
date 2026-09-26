//
//  ErrorReportDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

/**
 * A delegate that files error reports on the logger's behalf.
 *
 * The subsystem cannot reach the reporting layer directly, so
 * [Logger] forwards automatic error reports through this delegate
 * when [Logger.reportsErrorsAutomatically] is enabled. Register an
 * implementation once at launch with
 * [AppSubsystem.Delegates.registerErrorReportDelegate].
 *
 * **Important:** [fileReport] may be invoked from any thread.
 */
interface ErrorReportDelegate {
    /**
     * Files a report for the given exception.
     *
     * @param exception The exception to report.
     */
    fun fileReport(exception: Exception)
}
