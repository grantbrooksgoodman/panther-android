//
//  CacheDomain.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

import us.neotechnica.panther.subsystem.AppSubsystem

/**
 * A named group of cached entries that can be cleared as a unit.
 *
 * A [CacheDomain] pairs a string identifier with a closure that
 * removes every cached entry belonging to that domain. Cache
 * domains offer granular clear-caches controls and drive
 * memory-pressure cleanup.
 *
 * Two cache domains are equal when their [rawValue] strings match;
 * the [clear] closure is not compared.
 */
class CacheDomain(
    /** A string that uniquely identifies the domain. */
    val rawValue: String,
    /** A closure that removes every cached entry in this domain. */
    val clear: () -> Unit,
) {
    // MARK: - Companion

    companion object {
        /** The subsystem's own built-in cache domains. */
        val subsystemCases: List<CacheDomain> = emptyList()

        /** Every registered cache domain, combining app and subsystem domains. */
        val allCases: List<CacheDomain>
            get() =
                ((AppSubsystem.delegates.cacheDomainList?.appCacheDomains ?: emptyList()) + subsystemCases)
                    .distinctBy { it.rawValue }
    }

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean = other is CacheDomain && other.rawValue == rawValue

    override fun hashCode(): Int = rawValue.hashCode()
}
