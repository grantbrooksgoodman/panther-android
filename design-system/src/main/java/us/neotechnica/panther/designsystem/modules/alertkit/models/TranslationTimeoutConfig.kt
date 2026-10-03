//
//  TranslationTimeoutConfig.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 02/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlin.time.Duration

/**
 * A configuration that controls how AlertKit handles translation
 * timeouts.
 *
 * Use [TranslationTimeoutConfig] to specify how long to wait for
 * translations and what to do when they time out:
 *
 * ```kotlin
 * AlertKitConfig.overrideTranslationTimeoutConfig(
 *     TranslationTimeoutConfig(5.seconds, returnsInputsOnFailure = true),
 * )
 * ```
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
