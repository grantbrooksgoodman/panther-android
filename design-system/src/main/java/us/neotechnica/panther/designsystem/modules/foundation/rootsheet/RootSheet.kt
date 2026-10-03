//
//  RootSheet.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.rootsheet

import androidx.compose.runtime.Composable

/**
 * A composable intended for presentation as a sheet from the app's
 * root view.
 *
 * Wrap any composable in a [RootSheet] and pass it to
 * [RootSheets.present] to display it as a modal sheet at the root
 * level of the view hierarchy. Presenting from the root ensures the
 * sheet appears above all other content, regardless of the current
 * navigation depth.
 */
class RootSheet(
    /** The composable content to present as a sheet. */
    val content: @Composable () -> Unit,
) {
    // MARK: - Companion

    companion object
}
