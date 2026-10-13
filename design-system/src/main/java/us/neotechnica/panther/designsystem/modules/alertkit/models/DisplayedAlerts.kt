//
//  DisplayedAlerts.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import androidx.compose.runtime.mutableStateListOf
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert

// The alerts on screen: the current alert, preceded by any withdrawn
// alerts that are still animating out.
internal class DisplayedAlerts {
    // MARK: - Properties

    private val withdrawnAlerts = mutableStateListOf<PresentedAlert>()

    private var lastCurrentAlert: PresentedAlert? = null

    // MARK: - Methods

    // Returns the alerts to display for the given current alert. An
    // alert that stops being current stays displayed until `remove` is
    // called for it.
    fun alerts(current: PresentedAlert?): List<PresentedAlert> {
        val outgoingAlert = lastCurrentAlert?.takeIf { isOutgoing(it, current) }
        return withdrawnAlerts + listOfNotNull(outgoingAlert, current)
    }

    // Stops displaying the given withdrawn alert.
    fun remove(alert: PresentedAlert) {
        withdrawnAlerts.removeAll { it === alert }
    }

    // Records the given alert as current, withdrawing the alert that
    // was current before it. Call this method after each composition.
    fun update(current: PresentedAlert?) {
        lastCurrentAlert?.takeIf { isOutgoing(it, current) }?.let { withdrawnAlerts.add(it) }
        lastCurrentAlert = current
    }

    // MARK: - Auxiliary

    private fun isOutgoing(
        alert: PresentedAlert,
        current: PresentedAlert?,
    ): Boolean = alert !== current && withdrawnAlerts.none { it === alert }
}
