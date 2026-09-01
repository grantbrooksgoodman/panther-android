//
//  LoggerPresentationDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

/**
 * A delegate that presents user-visible alerts on the logger's
 * behalf.
 *
 * The subsystem cannot reach the design system's alert and toast
 * components directly, so [Logger] forwards presentation requests
 * through this delegate. Register an implementation once at launch
 * with [Logger.setPresentationDelegate]; the app's implementation
 * renders the request through the appropriate alert or toast.
 *
 * **Important:** [present] may be invoked from any thread. Marshal
 * to the main thread before touching UI.
 */
interface LoggerPresentationDelegate {
    /**
     * Presents an alert of the given type for the given exception
     * or text.
     *
     * @param alertType The kind of alert to present.
     * @param exception The exception the alert describes, or
     *   `null` when presenting free text.
     * @param text The message to present when no exception is
     *   available, or `null` when an exception is provided.
     */
    fun present(
        alertType: AlertType,
        exception: Exception?,
        text: String?,
    )
}
