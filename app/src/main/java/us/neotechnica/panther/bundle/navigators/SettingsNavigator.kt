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

    /** The full-screen destinations in the settings flow; it presents none. */
    sealed interface ModalPaths : Paths

    /** The push destinations in the settings flow; it pushes none. */
    sealed interface SeguePaths : Paths

    /** The sheet destinations presented over the settings flow. */
    sealed interface SheetPaths : Paths {
        /** The invite QR code sheet. */
        data object InviteQRCode : SheetPaths
    }
}

/** Applies [SettingsRoute]s to the settings flow's navigation state. */
object SettingsNavigator {
    /**
     * Returns the settings flow's state after applying the given
     * route.
     *
     * @param to The route to apply.
     * @param on The state to apply it to.
     */
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
