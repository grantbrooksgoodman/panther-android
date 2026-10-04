//
//  HUDConfig.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlin.time.Duration

/**
 * A configuration that controls the appearance of a
 * heads-up display shown during translation.
 *
 * When a translation takes longer than expected, AlertKit
 * displays a HUD to indicate that work is in progress. Use
 * [HUDConfig] to control when the HUD appears and whether
 * it blocks interaction:
 *
 * ```kotlin
 * AlertKitConfig.overrideTranslationHUDConfig(
 *     HUDConfig(appearsAfter = 1.seconds, isModal = true),
 * )
 * ```
 *
 * @property appearsAfter The duration to wait before
 *   displaying the HUD.
 * @property isModal Whether the HUD prevents interaction
 *   with the underlying content.
 */
data class HUDConfig(
    val appearsAfter: Duration,
    val isModal: Boolean,
)
