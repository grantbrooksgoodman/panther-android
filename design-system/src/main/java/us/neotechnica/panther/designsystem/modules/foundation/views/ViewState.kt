//
//  ViewState.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views

import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

/**
 * The current state of a [StatefulView].
 */
sealed interface ViewState {
    /** An error occurred; the associated [Exception] is displayed. */
    data class Error(
        val exception: Exception,
    ) : ViewState

    /** Content has loaded and is ready for display. */
    data object Loaded : ViewState

    /** Content is loading; a progress indicator is shown. */
    data object Loading : ViewState
}
