//
//  StoredItemKeys.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

// MARK: - StoredItemKey

/** Whether the temporary database caches have been populated. */
val StoredItemKey.Companion.populatedTemporaryCaches: StoredItemKey
    get() = StoredItemKey("populatedTemporaryCaches")

/**
 * Whether the user should be notified when a conversation becomes
 * unavailable.
 */
val StoredItemKey.Companion.shouldNotifyOfConversationAvailability: StoredItemKey
    get() = StoredItemKey("shouldNotifyOfConversationAvailability")

// MARK: - RuntimeStorage

/** Whether the temporary database caches have been populated. */
val RuntimeStorage.populatedTemporaryCaches: Boolean
    get() = retrieve(StoredItemKey.populatedTemporaryCaches) as? Boolean ?: false

/**
 * Whether the user should be notified when a conversation becomes
 * unavailable.
 */
val RuntimeStorage.shouldNotifyOfConversationAvailability: Boolean
    get() = retrieve(StoredItemKey.shouldNotifyOfConversationAvailability) as? Boolean ?: true
