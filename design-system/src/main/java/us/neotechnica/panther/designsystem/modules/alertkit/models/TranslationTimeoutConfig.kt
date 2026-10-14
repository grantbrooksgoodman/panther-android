//
//  TranslationTimeoutConfig.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlin.time.Duration

/**
 * A configuration that controls how AlertKit handles translation
 * timeouts.
 *
 * A [TranslationTimeoutConfig] specifies how long to wait for
 * translations and what to do when they time out. AlertKit passes
 * [AlertKitConfig.translationTimeoutConfig][us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig.translationTimeoutConfig]
 * to its translation delegate with each request.
 *
 * @property duration The maximum duration to wait for a translation
 *   to complete.
 * @property returnsInputsOnFailure Whether a timed-out translation
 *   falls back to the original untranslated strings rather than
 *   reporting an error.
 */
data class TranslationTimeoutConfig(
    val duration: Duration,
    val returnsInputsOnFailure: Boolean,
)
