//
//  RootSheetHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.rootsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Renders the currently presented [RootSheets] sheet as a modal bottom
 * sheet above all other content.
 *
 * Mount this once at the root of the view hierarchy. Dismissing the
 * sheet, whether by swipe or scrim tap, routes through
 * [RootSheets.dismiss] so any `onDismiss` closure runs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootSheetHost() {
    val sheet by RootSheets.sheet.collectAsState()
    val currentSheet = sheet ?: return

    ModalBottomSheet(
        onDismissRequest = { RootSheets.dismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        currentSheet.content()
    }
}
