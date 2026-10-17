//
//  UserInteractionBlocker.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.foundation.extensions.dismissSheets
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUDPresenter
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import kotlin.time.Duration.Companion.milliseconds

// Blocks user interaction on behalf of the modal heads-up display
// and the modal overlay. While either blocks, interactive content –
// the toast, the presented alert, the sheets, and the keyboard – is
// dismissed every 100 milliseconds; interaction returns only once
// neither blocks any longer.
internal object UserInteractionBlocker {
    // MARK: - Properties

    private val blockingUserInteraction = LockIsolated(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Computed Properties

    val isBlockingUserInteraction: Boolean
        get() = blockingUserInteraction.wrappedValue

    // MARK: - Methods

    fun blockUserInteraction(dismissSheets: Boolean = true) {
        blockingUserInteraction.wrappedValue = true
        dismissInteractiveContent(dismissSheets)
    }

    fun unblockUserInteraction() {
        if (HUDPresenter.isBlockingUserInteraction ||
            Overlay.isBlockingUserInteraction
        ) {
            return
        }

        blockingUserInteraction.wrappedValue = false
    }

    // MARK: - Auxiliary

    private fun dismissInteractiveContent(dismissSheets: Boolean) {
        if (!blockingUserInteraction.wrappedValue) return

        Toast.hide()
        AlertPresenter.dismiss()
        if (dismissSheets) {
            RootSheets.dismiss()
            DependencyValues
                .current
                .sharedEvents
                .dismissSheets
                .send(Unit)
        }

        KeyboardService.resignFirstResponders()

        scope.launch {
            delay(DISMISSAL_INTERVAL)
            dismissInteractiveContent(dismissSheets)
        }
    }

    // MARK: - Companion

    private val DISMISSAL_INTERVAL = 100.milliseconds
}
