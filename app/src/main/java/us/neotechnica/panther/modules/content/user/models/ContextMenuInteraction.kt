//
//  ContextMenuInteraction.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A namespace controlling whether message context menu
 * interactions may begin.
 */
object ContextMenuInteraction {
    // MARK: - Properties

    private var internalCanBegin by mutableStateOf(true)

    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether context menu
     * interactions may begin.
     */
    val canBegin: Boolean
        get() = internalCanBegin

    // MARK: - Methods

    /**
     * Sets whether context menu interactions may begin.
     *
     * Enabling interactions arms the long press that lifts a
     * message bubble; disabling them ignores the long press on
     * visible cells.
     *
     * @param canBegin A Boolean value that indicates whether
     *   interactions may begin.
     */
    fun setCanBegin(canBegin: Boolean) {
        internalCanBegin = canBegin
    }
}
