//
//  ArchiveStrategy.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.models

/**
 * When a completed translation is written to the hosted archive.
 */
enum class ArchiveStrategy {
    /** Archive later, out of the request's critical path. */
    DEFERRED,

    /** Archive as part of the translation request. */
    IMMEDIATE,
}
