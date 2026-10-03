//
//  RootSheets.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheet
import us.neotechnica.panther.modules.content.shared.components.invitelanguagepickerview.InviteLanguagePickerView

/**
 * The named sheets presented from the app's root view.
 *
 * The root sheet presents content above all other views in the
 * hierarchy, regardless of navigation depth. Present a named sheet with
 * [RootSheets.present][us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets.present].
 */
val RootSheet.Companion.inviteLanguagePicker: RootSheet
    get() = RootSheet { InviteLanguagePickerView() }
