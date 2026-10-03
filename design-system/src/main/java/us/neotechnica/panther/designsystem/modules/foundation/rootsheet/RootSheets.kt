//
//  RootSheets.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.rootsheet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Presents and dismisses a sheet from the app's root view.
 *
 * Use [RootSheets] to present a modal sheet that appears above all
 * other content, regardless of the current navigation depth. The
 * [RootSheetHost] composable renders the currently presented sheet.
 * Call [dismiss] to remove it; an optional `onDismiss` closure given
 * at presentation time runs when the sheet is dismissed.
 */
object RootSheets {
    // MARK: - Properties

    private val mutableSheet = MutableStateFlow<RootSheet?>(null)
    private var onDismiss: (() -> Unit)? = null

    // MARK: - Computed Properties

    /** The currently presented root sheet, or `null` if none. */
    val sheet: StateFlow<RootSheet?> = mutableSheet.asStateFlow()

    // MARK: - Present

    /**
     * Presents the given sheet from the root view.
     *
     * @param sheet The sheet to present.
     * @param onDismiss A closure executed when the sheet is dismissed,
     *   or `null` for none.
     */
    fun present(
        sheet: RootSheet,
        onDismiss: (() -> Unit)? = null,
    ) {
        mutableSheet.value = sheet
        this.onDismiss = onDismiss
    }

    // MARK: - Dismiss

    /**
     * Dismisses the currently presented root sheet.
     *
     * If an `onDismiss` closure was provided at presentation time, it
     * is executed and then cleared.
     */
    fun dismiss() {
        mutableSheet.value = null
        onDismiss?.invoke()
        onDismiss = null
    }
}
