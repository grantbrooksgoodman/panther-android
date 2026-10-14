//
//  ChatNavigator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.navigators

import us.neotechnica.panther.subsystem.modules.navigation.interfaces.NavigatorState
import us.neotechnica.panther.subsystem.modules.navigation.interfaces.Paths

/** The chat flow's navigation state. */
data class ChatNavigatorState(
    override val modal: ModalPaths? = null,
    override val sheet: SheetPaths? = null,
    override val stack: List<SeguePaths> = emptyList(),
) : NavigatorState<ChatNavigatorState.ModalPaths, ChatNavigatorState.SeguePaths, ChatNavigatorState.SheetPaths> {
    // MARK: - Types

    sealed interface ModalPaths : Paths

    sealed interface SeguePaths : Paths

    /** The sheet destinations presented over the chat flow. */
    sealed interface SheetPaths : Paths {
        data object CameraPicker : SheetPaths

        data object ContactSelector : SheetPaths

        data object PhotoPicker : SheetPaths
    }
}

/** Applies [ChatRoute]s to the chat flow's navigation state. */
object ChatNavigator {
    fun navigate(
        to: ChatRoute,
        on: ChatNavigatorState,
    ): ChatNavigatorState =
        when (to) {
            is ChatRoute.Sheet -> on.copy(sheet = to.path)
        }
}

/** Routes handled by the chat navigator. */
sealed interface ChatRoute {
    /** Presents the given sheet over the chat flow, or dismisses it when `null`. */
    data class Sheet(
        val path: ChatNavigatorState.SheetPaths?,
    ) : ChatRoute
}
