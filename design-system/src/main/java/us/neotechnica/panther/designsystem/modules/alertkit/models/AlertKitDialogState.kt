//
//  AlertKitDialogState.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import androidx.compose.animation.core.MutableTransitionState

// The visibility of an AlertKit dialog, which animates in when first
// shown and animates out before reporting the user's choice, or before
// leaving the screen when withdrawn.
internal class AlertKitDialogState {
    // MARK: - Properties

    val visibleState = MutableTransitionState(false).apply { targetState = true }

    private var dismissalAction: (() -> Unit)? = null
    private var isDismissing = false
    private var withdrawalAction: (() -> Unit)? = null

    // MARK: - Methods

    // Animates the dialog out, then performs the given action. Requests
    // after the first are ignored.
    fun dismiss(then: () -> Unit) {
        if (isDismissing) return
        isDismissing = true
        dismissalAction = then
        visibleState.targetState = false
    }

    // Performs the pending actions once the dialog has finished
    // animating out: first the dismissal action, then the withdrawal
    // action.
    fun performPendingActions() {
        if (!visibleState.isIdle || visibleState.currentState) return

        val dismissalAction = dismissalAction
        val withdrawalAction = withdrawalAction
        this.dismissalAction = null
        this.withdrawalAction = null

        dismissalAction?.invoke()
        withdrawalAction?.invoke()
    }

    // Animates the dialog out, if it has not already, then performs the
    // given action. Use this method when the dialog's alert is
    // dismissed other than through one of its actions.
    fun withdraw(then: () -> Unit) {
        withdrawalAction = then
        if (!isDismissing) {
            isDismissing = true
            visibleState.targetState = false
        }

        performPendingActions()
    }
}
