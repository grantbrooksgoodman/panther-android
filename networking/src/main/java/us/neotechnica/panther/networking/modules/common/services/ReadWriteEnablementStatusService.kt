//
//  ReadWriteEnablementStatusService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.services

import kotlinx.coroutines.flow.first
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import us.neotechnica.panther.subsystem.modules.shared.models.isForcedUpdateRequired

internal object ReadWriteEnablementStatusService {
    // MARK: - Methods

    suspend fun listenForReadWriteEnablementStatusChanges() {
        AppSubsystem.delegates.forcedUpdateModal ?: return
        SharedState { it.isForcedUpdateRequired }
            .projectedValue
            .changes
            .first { it }

        Networking.isReadWriteEnabled = false
    }
}
