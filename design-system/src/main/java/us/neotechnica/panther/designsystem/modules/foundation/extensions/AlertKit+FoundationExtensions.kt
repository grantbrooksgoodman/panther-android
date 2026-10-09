//
//  AlertKit+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.extensions

import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey

// MARK: - Properties

/** A cancel action using the localized cancel string. */
val Action.Companion.cancelAction: Action
    get() =
        Action(
            Localized(SubsystemStringKey.CANCEL).wrappedValue,
            style = ActionStyle.CANCEL,
        ) {}

// MARK: - Methods

/** Returns a cancel action with a custom title. */
fun Action.Companion.cancelAction(title: String): Action =
    Action(
        title,
        style = ActionStyle.CANCEL,
    ) {}
