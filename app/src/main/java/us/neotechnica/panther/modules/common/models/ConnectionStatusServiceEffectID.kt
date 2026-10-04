//
//  ConnectionStatusServiceEffectID.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

/**
 * A unique identifier for an effect registered with
 * [ConnectionStatusService][us.neotechnica.panther.modules.common.services.ConnectionStatusService].
 */
enum class ConnectionStatusServiceEffectID(
    /** The string that identifies the effect. */
    val rawValue: String,
) {
    CHECK_FOR_UPDATES("checkForUpdates"),
    CONFIGURE_INPUT_BAR("configureInputBar"),
    RETRY_MESSAGE_OUTBOX("retryMessageOutbox"),
    SHOW_OFFLINE_MODE_TOAST("showOfflineModeToast"),
}
