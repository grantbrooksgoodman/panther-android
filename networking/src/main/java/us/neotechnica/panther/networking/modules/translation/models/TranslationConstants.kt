//
//  TranslationConstants.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.models

/** Constants for the hosted translation archive wire format. */
internal object TranslationConstants {
    /** The prefix distinguishing an idempotent hosting key. */
    const val IDEMPOTENT_PREFIX = "IDEM "

    /** The duration after which the translation data snapshot expires, in milliseconds. */
    const val TRANSLATION_DATA_SAMPLE_EXPIRY_THRESHOLD_MILLIS = 300_000L

    /** The interval at which the translation data snapshot is refreshed, in milliseconds. */
    const val TRANSLATION_DATA_SAMPLE_REFRESH_INTERVAL_MILLIS = 240_000L
}
