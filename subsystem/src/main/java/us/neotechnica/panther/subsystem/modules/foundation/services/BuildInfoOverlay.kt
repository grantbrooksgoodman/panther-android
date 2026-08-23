//
//  BuildInfoOverlay.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey

/**
 * Controls the visibility of the build-info overlay.
 *
 * Mirrors the iOS `BuildInfoOverlay`. The hidden state is published as
 * a [StateFlow] so the overlay view reacts to changes, and it is
 * persisted across launches.
 */
object BuildInfoOverlay {
    // MARK: - Properties

    private val internalIsHidden =
        MutableStateFlow(Persistent.boolean(PersistentStorageKey.hidesBuildInfoOverlay, default = false))

    /** Whether the overlay is currently hidden. */
    val isHidden: StateFlow<Boolean> = internalIsHidden.asStateFlow()

    // MARK: - Methods

    /** Hides the overlay, persisting the setting by default. */
    fun hide(persistSetting: Boolean = true) {
        internalIsHidden.value = true
        if (persistSetting) Persistent.setBoolean(PersistentStorageKey.hidesBuildInfoOverlay, true)
    }

    /** Shows the overlay, persisting the setting by default. */
    fun show(persistSetting: Boolean = true) {
        internalIsHidden.value = false
        if (persistSetting) Persistent.setBoolean(PersistentStorageKey.hidesBuildInfoOverlay, false)
    }
}
