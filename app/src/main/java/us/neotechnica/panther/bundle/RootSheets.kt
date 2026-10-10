//
//  RootSheets.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheet
import us.neotechnica.panther.modules.content.shared.components.invitelanguagepickerview.InviteLanguagePickerView
import us.neotechnica.panther.modules.content.user.views.reactiondetailspageview.ReactionDetailsPageView

/**
 * The named sheets presented from the app's root view.
 *
 * The root sheet presents content above all other views in the
 * hierarchy, regardless of navigation depth. Present a named sheet with
 * [RootSheets.present][us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets.present].
 */
val RootSheet.Companion.inviteLanguagePicker: RootSheet
    get() = RootSheet(interactiveDismissDisabled = true) { InviteLanguagePickerView() }

/**
 * The reaction details sheet for the message with the given
 * identifier.
 *
 * @param messageID The identifier of the message whose reactions are
 *   shown.
 *
 * @return The reaction details root sheet.
 */
fun RootSheet.Companion.reactionDetailsPageView(messageID: String): RootSheet = RootSheet { ReactionDetailsPageView(messageID) }
