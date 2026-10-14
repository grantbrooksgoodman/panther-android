//
//  SettingsNavigator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.navigators

import us.neotechnica.panther.subsystem.modules.navigation.interfaces.NavigatorState
import us.neotechnica.panther.subsystem.modules.navigation.interfaces.Paths

/** The settings flow's navigation state. */
data class SettingsNavigatorState(
    override val modal: ModalPaths? = null,
    override val sheet: SheetPaths? = null,
    override val stack: List<SeguePaths> = emptyList(),
) : NavigatorState<SettingsNavigatorState.ModalPaths, SettingsNavigatorState.SeguePaths, SettingsNavigatorState.SheetPaths> {
    // MARK: - Types

    sealed interface ModalPaths : Paths

    sealed interface SeguePaths : Paths

    /** The sheet destinations presented over the settings flow. */
    sealed interface SheetPaths : Paths {
        /** The invite QR code sheet. */
        data object InviteQRCode : SheetPaths
    }
}

/** Applies [SettingsRoute]s to the settings flow's navigation state. */
object SettingsNavigator {
    fun navigate(
        to: SettingsRoute,
        on: SettingsNavigatorState,
    ): SettingsNavigatorState =
        when (to) {
            is SettingsRoute.Sheet -> on.copy(sheet = to.path)
        }
}

/** Routes handled by the settings navigator. */
sealed interface SettingsRoute {
    /** Presents the given sheet over the settings flow, or dismisses it when `null`. */
    data class Sheet(
        val path: SettingsNavigatorState.SheetPaths?,
    ) : SettingsRoute
}
