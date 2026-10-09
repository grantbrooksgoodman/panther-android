//
//  ConnectionAlert.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.models

import android.content.Intent
import android.provider.Settings
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.foundation.extensions.cancelAction
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey
import us.neotechnica.panther.translator.Translator

internal object ConnectionAlert {
    // MARK: - Present

    suspend fun present() {
        val actions =
            listOf(
                Action.cancelAction(title = "OK"),
                Action(Localized(SubsystemStringKey.SETTINGS).wrappedValue) {
                    Translator
                        .config
                        .currentActivityProvider
                        ?.invoke()
                        ?.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                },
            )

        Alert(
            message = Localized(SubsystemStringKey.NO_INTERNET_MESSAGE).wrappedValue,
            actions = actions,
        ).present(translating = emptyList())
    }
}
