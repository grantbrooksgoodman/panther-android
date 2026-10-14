//
//  RootSheetHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.rootsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import us.neotechnica.panther.designsystem.modules.foundation.components.PageSheetView

/**
 * Renders the currently presented [RootSheets] sheet as a page sheet
 * above all other content.
 *
 * Mount this once at the root of the view hierarchy. Dismissing the
 * sheet, whether by swipe or scrim tap, routes through
 * [RootSheets.dismiss] so any `onDismiss` closure runs. A sheet with
 * `interactiveDismissDisabled` ignores swipes and scrim taps and can
 * only be dismissed programmatically.
 */
@Composable
fun RootSheetHost() {
    val sheet by RootSheets.sheet.collectAsState()

    PageSheetView(
        item = sheet,
        onDismissRequest = { RootSheets.dismiss() },
        isInteractiveDismissDisabled = sheet?.interactiveDismissDisabled == true,
        ignoresBottomSafeArea = sheet?.ignoresBottomSafeArea == true,
    ) { presentedSheet ->
        presentedSheet.content()
    }
}
