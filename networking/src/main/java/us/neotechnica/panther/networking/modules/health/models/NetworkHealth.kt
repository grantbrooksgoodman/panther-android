//
//  NetworkHealth.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

// MARK: - NetworkHealth

/**
 * A representation of the network's current usability.
 *
 * [NetworkHealth] communicates the quality of the device's
 * network connection as either a measured score with an
 * associated tier, or an unknown state when insufficient data is
 * available.
 *
 * The health value is derived from passive observation of the
 * network operations the framework already performs – it
 * generates no traffic of its own unless opt-in active probing is
 * enabled through
 * [NetworkHealthConfiguration.probeConfiguration].
 */
sealed class NetworkHealth {
    // MARK: - Cases

    /**
     * The network's health has been evaluated.
     *
     * @property score A continuous value in `[0.0, 1.0]` where `0`
     *   indicates a completely unusable network and `1` indicates
     *   an excellent connection.
     * @property tier A discrete classification derived from the
     *   score.
     */
    data class Measured(
        override val score: Double,
        override val tier: NetworkHealthTier,
    ) : NetworkHealth()

    /**
     * Insufficient data is available to determine network health.
     *
     * This state occurs at launch, after a network interface
     * transition (for example, switching from Wi-Fi to cellular),
     * or after a prolonged period of inactivity.
     */
    data object Unknown : NetworkHealth()

    // MARK: - Computed Properties

    /** A Boolean value indicating whether the health is [Unknown]. */
    val isUnknown: Boolean
        get() = this is Unknown

    /** The health score, or `null` when the health is [Unknown]. */
    open val score: Double?
        get() = (this as? Measured)?.score

    /** The health tier, or `null` when the health is [Unknown]. */
    open val tier: NetworkHealthTier?
        get() = (this as? Measured)?.tier
}

// MARK: - NetworkHealthTier

/**
 * A discrete classification of network quality derived from the
 * health score.
 *
 * Tier boundaries are configurable through
 * [NetworkHealthConfiguration].
 */
enum class NetworkHealthTier(
    /** The raw string identifier of the tier. */
    val rawValue: String,
) {
    /** Network quality is acceptable but not optimal. */
    FAIR("fair"),

    /** Network quality is strong. */
    GOOD("good"),

    /** Network quality is degraded. */
    POOR("poor"),
}
