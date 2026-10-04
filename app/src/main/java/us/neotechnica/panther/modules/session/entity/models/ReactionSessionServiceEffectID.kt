//
//  ReactionSessionServiceEffectID.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.models

/**
 * A unique identifier for an effect registered with
 * [ReactionSessionService][us.neotechnica.panther.modules.session.entity.services.ReactionSessionService].
 *
 * @property rawValue The string that identifies the effect.
 */
data class ReactionSessionServiceEffectID(
    val rawValue: String,
) {
    companion object {
        val reloadCollectionView = ReactionSessionServiceEffectID("reloadCollectionView")
        val scrollToLastItem = ReactionSessionServiceEffectID("scrollToLastItem")
    }
}
