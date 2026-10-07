//
//  CacheDomain.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashStore
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver

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
        /** The cache domain for identity-hash lookups. */
        val encodedHash = CacheDomain("encodedHash") { EncodedHashStore.clearStore() }

        /** The cache domain for localized string lookups. */
        val localization = CacheDomain("localization") { LocalizedStringResolver.clearCache() }

        /** The subsystem's own built-in cache domains. */
        val subsystemCases: List<CacheDomain> =
            listOf(
                encodedHash,
                localization,
            )

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
